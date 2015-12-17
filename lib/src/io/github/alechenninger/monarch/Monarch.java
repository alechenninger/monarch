package io.github.alechenninger.monarch;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Monarch {
  /**
   * Generates new data for all known sources in the hierarchy based on the hierarchy, the data
   * changes you want applied, the existing state of the data, and a "pivot" source which you want
   * to change alongside all of its children.
   *
   * @param hierarchy A tree-structure describing which sources inherit from which parent sources.
   * @param changes The "end-state" changes to be applied assuming you could update the entire tree.
   *                Even if you are not pivoting on the root of the tree (which would generate an
   *                entirely new tree), the {@code pivotSource} and its children will be updated
   *                such that each source's inherited values are what you want your end state to be,
   *                as described by the changes.
   * @param pivotSource A source in the hierarchy that represents a level that we can start to
   *                    change values. Sources above this source will be untouched. This source and
   *                    any below it will be updated to achieve the desired {@code changes}.
   * @param data A map of sources to key:value pairs representing the existing state of the data.
   * @param mergeKeys A list of keys whose flattened value is a <em>merge</em> of all of the values
   *                  in the hierarchy for that key, provided the values are all either
   *                  {@link Collection collections} or {@link Map maps}. Keys not in the list
   *                  use values from only the nearest in a sources ancestry.
   * @return A map of sources to key:value pairs representing the new state of the data with changes
   *         applied to the given {@code pivotSource} and its children.
   */
  public Map<String, Map<String, Object>> generateSources(Hierarchy hierarchy,
      Iterable<Change> changes, String pivotSource, Map<String, Map<String, Object>> data,
      List<String> mergeKeys) {
    List<String> descendants = hierarchy.descendantsOf(pivotSource)
        .orElseThrow(() -> new IllegalArgumentException("Could not find pivot source in " +
            "hierarchy. Pivot source: " + pivotSource + ". Hierarchy: \n" + hierarchy));
    Map<String, Map<String, Object>> result = copyMapAndValues(data);

    // From top-most to inner-most, generate results, taking into account the results from ancestors
    // as we go along.
    for (String descendant : descendants) {
      result.put(descendant, generateSingleSource(hierarchy, changes, descendant, result,
          mergeKeys));
    }

    return result;
  }

  public Optional<Change> findChangeForSource(String source, Iterable<Change> changes) {
    return StreamSupport.stream(changes.spliterator(), false)
        .filter(c -> Objects.equals(c.source(), source))
        .collect(Collect.maxOneResultOrThrow(() -> new IllegalArgumentException(
            "Expected only one change with matching source in list of changes, but got: " +
                changes)));
  }

  /**
   * Flattens a hierarchy of key:values into a single map. It's a "view" of the data from a child's
   * point of view: all keys in the hierarchy, with the values from the nearest ancestor (or self).
   *
   * @param ancestry Ordered list of sources, child first to furthest ancestor last.
   * @param sourceToData Map of sources to their current key:values.
   */
  public Map<String, Object> flattenSource(List<String> ancestry,
      Map<String, Map<String, Object>> sourceToData, List<String> mergeKeys) {
    if (!sourceToData.keySet().containsAll(ancestry)) {
      List<String> missing = ancestry.stream()
          .filter(s -> !sourceToData.containsKey(s))
          .collect(Collectors.toList());

      // TODO: Is this exceptional or just treat as empty data?
      throw new IllegalArgumentException("Not all sources in ancestry found in source data.\n" +
          "  Missing sources: " + missing + "\n" +
          "  Sources in ancestry: " + ancestry + "\n" +
          "  Sources in source data: " + sourceToData.keySet());
    }

    Map<String, Object> flattened = new HashMap<>();

    for (String source : new ListReversed<>(ancestry)) {
      Map<String, Object> sourceData = sourceToData.get(source);
      if (sourceData != null) {
        for (Map.Entry<String, Object> sourceEntry : sourceData.entrySet()) {
          String sourceKey = sourceEntry.getKey();
          Object sourceValue = sourceEntry.getValue();

          Object newValue = mergeKeys.contains(sourceKey)
              ? getMergedValue(flattened, sourceKey, sourceValue)
              : sourceValue;

          flattened.put(sourceKey, newValue);
        }
      }
    }

    return flattened;
  }

  /**
   * Generates new data for the given source only, taking into account the desired changes, the
   * existing hierarchy, and the existing data in the hierarchy.
   */
  private Map<String, Object> generateSingleSource(Hierarchy hierarchy, Iterable<Change> changes,
      String pivotSource, Map<String, Map<String, Object>> data, List<String> mergeKeys) {
    List<String> ancestors = hierarchy.ancestorsOf(pivotSource)
        .orElseThrow(() -> new IllegalArgumentException(
            "Could not find pivot source in hierarchy. Pivot source: " + pivotSource + ". " +
            "Hierarchy: \n" + hierarchy));

    Map<String, Object> flattenedSourceData = flattenSource(ancestors, data, mergeKeys);

    Map<String, Object> original = data.get(pivotSource);
    Map<String, Object> result = original == null ? new HashMap<>() : new HashMap<>(original);

    for (String ancestor : new ListReversed<>(ancestors)) {
      Optional<Change> maybeChange = findChangeForSource(ancestor, changes);

      if (!maybeChange.isPresent()) continue;

      Change change = maybeChange.get();

      for (Map.Entry<String, Object> setEntry : change.set().entrySet()) {
        String setKey = setEntry.getKey();
        Object setValue = setEntry.getValue();

        if (!Objects.equals(change.source(), pivotSource)) {
          if (mergeKeys.contains(setKey)) {
            if (isValueInherited(flattenedSourceData, setKey, setValue, mergeKeys)) {
              Object unmerged = getUnmergedValue(result.get(setKey), setValue);

              if (mergeableIsNotEmpty(unmerged)) {
                result.put(setKey, unmerged);
              }

              continue;
            }
          } else {
            if (Objects.equals(flattenedSourceData.get(setKey), setValue)) {
              result.remove(setKey);
              continue;
            }
          }
        }

        Object newValue = mergeKeys.contains(setKey)
            ? getMergedValue(result, setKey, setValue)
            : setValue;

        result.put(setKey, newValue);
      }

      // TODO: Support removing nested keys (keys in a hash)
      for (String key : change.remove()) {
        result.remove(key);
      }
    }

    return result;
  }

  private static boolean mergeableIsNotEmpty(Object collectionOrMapOrNull) {
    if (collectionOrMapOrNull == null) {
      return false;
    }

    if (collectionOrMapOrNull instanceof Collection) {
      return !((Collection) collectionOrMapOrNull).isEmpty();
    }

    if (collectionOrMapOrNull instanceof Map) {
      return !((Map) collectionOrMapOrNull).isEmpty();
    }

    throw new IllegalArgumentException("Value not a mergeable type: " + collectionOrMapOrNull);
  }

  private static Object getUnmergedValue(Object mergedValue, Object setValue) {
    if (mergedValue == null) {
      return null;
    }

    if (mergedValue instanceof Collection) {
      if (!(setValue instanceof Collection)) {
        throw new IllegalArgumentException("Incompatible values for merge key: " + mergedValue +
            " and " + setValue);
      }

      Set<Object> unmergedValue = new HashSet<>();
      unmergedValue.addAll((Collection) mergedValue);
      unmergedValue.removeAll((Collection) setValue);

      return unmergedValue;
    }

    if (mergedValue instanceof Map) {
      if (!(setValue instanceof Map)) {
        throw new IllegalArgumentException("Incompatible values for merge key: " + mergedValue +
            " and " + setValue);
      }

      Map<String, Object> mergedValueAsMap = (Map<String, Object>) mergedValue;
      Map<String, Object> setValueAsMap = (Map<String, Object>) setValue;
      Map<String, Object> unmergedValue = new HashMap<>(mergedValueAsMap);

      for (String setValueKey : setValueAsMap.keySet()) {
        unmergedValue.remove(setValueKey);
      }

      return unmergedValue;
    }

    throw new IllegalArgumentException("Current value not a mergeable type: " + mergedValue);
  }

  private static boolean isValueInherited(Map<String, Object> data, String key, Object value,
      List<String> mergeKeys) {
    if (!data.containsKey(key)) {
      return false;
    }

    Object dataValue = data.get(key);

    if (Objects.equals(dataValue, value)) {
      return true;
    }

    if (mergeKeys.contains(key)) {
      if (dataValue instanceof Collection) {
        if (!(value instanceof Collection)) {
          throw new IllegalArgumentException("Incompatible values for merge key: " + dataValue +
              " and " + value);
        }

        return ((Collection) dataValue).containsAll((Collection) value);
      }

      if (dataValue instanceof Map) {
        if (!(value instanceof Map)) {
          throw new IllegalArgumentException("Incompatible values for merge key: " + dataValue +
              " and " + value);
        }

        Map<String, Object> dataValueAsMap = (Map<String, Object>) dataValue;
        Map<String, Object> valueAsMap = (Map<String, Object>) value;

        for (Map.Entry<String, Object> entryInValue : valueAsMap.entrySet()) {
          String keyInValue = entryInValue.getKey();
          Object valueForKey = entryInValue.getValue();

          if (!dataValueAsMap.containsKey(keyInValue)) {
            return false;
          }

          if (!Objects.equals(dataValueAsMap.get(keyInValue), valueForKey)) {
            return false;
          }
        }

        return true;
      }
    }

    return false;
  }

  private static Object getMergedValue(Map<String, Object> map, String key, Object valueToMerge) {
    if (!(valueToMerge instanceof Map || valueToMerge instanceof Collection)) {
      throw new IllegalArgumentException("Asked to merge " + key + " but value is not a "
          + "collection or map.");
    }

    Object currentValue = map.get(key);

    if (currentValue == null) {
      return valueToMerge;
    }

    if (currentValue instanceof Collection) {
      if (!(valueToMerge instanceof Collection)) {
        throw new IllegalArgumentException("Asked to merge " + key + ", but the current value "
            + "is a collection while the new value is not a collection. Both values must be "
            + "similar types to be able to merge. Old value is: " + currentValue + ". New value is "
            + valueToMerge);
      }

      Set<Object> mergedValue = new HashSet<>();
      mergedValue.addAll((Collection) currentValue);
      mergedValue.addAll((Collection) valueToMerge);

      return mergedValue;
    }

    if (currentValue instanceof Map) {
      if (!(valueToMerge instanceof Map)) {
        throw new IllegalArgumentException("Asked to merge " + key + ", but the old value "
            + "is a map while the new value is not a map. Both values must be "
            + "similar types to be able to merge. Old value is: " + currentValue + ". New value is "
            + valueToMerge);
      }

      // TODO: Recurse? What if map of maps?
      Map<String, Object> mergedValue = new HashMap<>();
      mergedValue.putAll((Map) currentValue);
      mergedValue.putAll((Map) valueToMerge);

      return mergedValue;
    }

    throw new IllegalArgumentException("Current value not a merge-able type: " + currentValue);
  }

  private static Map<String, Map<String, Object>> copyMapAndValues(
      Map<String, Map<String, Object>> data) {
    Map<String, Map<String, Object>> copy = new HashMap<>();

    for (Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
      String key = entry.getKey();
      Map value = entry.getValue();
      copy.put(key, value == null ? new HashMap<>() : new HashMap<>(value));
    }

    return copy;
  }
}

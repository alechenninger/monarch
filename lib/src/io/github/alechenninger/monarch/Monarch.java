package io.github.alechenninger.monarch;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
      Set<String> mergeKeys) {
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
   * Generates new data for the given source only, taking into account the desired changes, the
   * existing hierarchy, and the existing data in the hierarchy.
   */
  private Map<String, Object> generateSingleSource(Hierarchy hierarchy, Iterable<Change> changes,
      String pivotSource, Map<String, Map<String, Object>> data, Set<String> mergeKeys) {
    List<String> ancestors = hierarchy.ancestorsOf(pivotSource)
        .orElseThrow(() -> new IllegalArgumentException(
            "Could not find pivot source in hierarchy. Pivot source: " + pivotSource + ". " +
                "Hierarchy: \n" + hierarchy));

    DataLookup sourceLookup = new DataLookupFromMap(data, pivotSource, hierarchy, mergeKeys);

    Map<String, Object> sourceData = data.get(pivotSource);
    Map<String, Object> resultSourceData = sourceData == null
        ? new HashMap<>()
        : new HashMap<>(sourceData);

    for (String ancestor : new ListReversed<>(ancestors)) {
      Optional<Change> maybeChange = findChangeForSource(ancestor, changes);

      if (!maybeChange.isPresent()) {
        continue;
      }

      Change change = maybeChange.get();

      for (Map.Entry<String, Object> setEntry : change.set().entrySet()) {
        String setKey = setEntry.getKey();
        Object setValue = setEntry.getValue();

        if (!Objects.equals(change.source(), pivotSource)) {
          if (sourceLookup.isValueInherited(setKey, setValue)) {
            if (resultSourceData.containsKey(setKey)) {
              if (mergeKeys.contains(setKey)) {
                Merger merger = Merger.startingWith(resultSourceData.get(setKey));
                merger.unmerge(setValue);
                resultSourceData.put(setKey, merger.getMerged());
              } else {
                resultSourceData.remove(setKey);
              }
            }
            continue;
          }
        }

        Object newValue;
        Object currentValue = resultSourceData.get(setKey);

        if (mergeKeys.contains(setKey) && currentValue != null) {
          Merger merger = Merger.startingWith(currentValue);
          merger.merge(setValue);
          newValue = merger.getMerged();
        } else {
          newValue = setValue;
        }

        resultSourceData.put(setKey, newValue);
      }

      // TODO: Support removing nested keys (keys in a hash)
      for (String key : change.remove()) {
        resultSourceData.remove(key);
      }
    }

    return resultSourceData;
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

package io.github.alechenninger.monarch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
   * @return A map of sources to key:value pairs representing the new state of the data with changes
   *         applied to the given {@code pivotSource} and its children.
   */
  public Map<String, Map<String, Object>> generateSources(Hierarchy hierarchy,
      Iterable<Change> changes, String pivotSource, Map<String, Map<String, Object>> data) {
    List<String> descendants = hierarchy.descendantsOf(pivotSource)
        .orElseThrow(() -> new IllegalArgumentException("Could not find pivot source in " +
            "hierarchy. Pivot source: " + pivotSource + ". Hierarchy: \n" + hierarchy));
    Map<String, Map<String, Object>> result = copyMapAndValues(data);

    // From top-most to inner-most, generate results, taking into account the results from ancestors
    // as we go along.
    for (String descendant : descendants) {
      result.put(descendant, generateSingleSource(hierarchy, changes, descendant, result));
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
      Map<String, Map<String, Object>> sourceToData) {
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

    for (String source : ancestry) {
      Map<String, Object> data = sourceToData.get(source);
      if (data != null) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
          flattened.put(entry.getKey(), entry.getValue());
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
      String pivotSource, Map<String, Map<String, Object>> data) {
    List<String> ancestors = hierarchy.ancestorsOf(pivotSource)
        .orElseThrow(() -> new IllegalArgumentException(
            "Could not find pivot source in hierarchy. Pivot source: " + pivotSource + ". " +
            "Hierarchy: \n" + hierarchy));

    Map<String, Object> flattenedSourceData = flattenSource(ancestors, data);

    Map<String, Object> original = data.get(pivotSource);
    Map<String, Object> result = original == null ? new HashMap<>() : new HashMap<>(original);

    for (String source : new ListReversed<>(ancestors)) {
      Optional<Change> maybeChange = findChangeForSource(source, changes);

      if (!maybeChange.isPresent()) continue;

      Change change = maybeChange.get();

      for (Map.Entry<String, Object> entry : change.set().entrySet()) {
        // Is this change not for the pivot source, and have we already inherited this value?
        if (!Objects.equals(change.source(), pivotSource) &&
            flattenedSourceData.containsKey(entry.getKey()) &&
            Objects.equals(flattenedSourceData.get(entry.getKey()), entry.getValue())) {
          // Ensure not present in result source since it would be redundant and not explicitly set
          result.remove(entry.getKey());
          continue;
        }

        result.put(entry.getKey(), entry.getValue());
      }

      // TODO: Support removing nested keys (keys in a hash)
      for (String key : change.remove()) {
        result.remove(key);
      }
    }

    return result;
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

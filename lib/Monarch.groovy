class Monarch {
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
  Map<String, Map> generateSources(Hierarchy hierarchy, Iterable<Change> changes,
      String pivotSource, Map<String, Map> data) {
    def descendants = hierarchy.descendantsOf(pivotSource)
        .orElseThrow({new IllegalArgumentException("Could not find pivot source in hierarchy.\n" +
            "  Pivot source: ${pivotSource}\n" +
            "  Hierarchy: ${hierarchy}")})
    def result = deepCopy(data)

    // From top-most to inner-most, generate results, taking into account the results from ancestors
    // as we go along.
    for (def descendant in descendants) {
      result[descendant] = generateSingleSource(hierarchy, changes, descendant, result)
    }

    return result
  }

  Optional<Change> findChangeForSource(String source, Iterable<Change> changes) {
    def found = changes.findAll {it.source == source}
    if (found.empty) {
      return Optional.empty()
    }
    if (found.size() > 1) {
      throw new IllegalArgumentException(
          "Expected only one change with matching source in list of changes, but got: ${changes}")
    }
    return Optional.of(found.first())
  }

  /**
   * Flattens a hierarchy of key:values into a single map. It's a "view" of the data from a child's
   * point of view: all keys in the hierarchy, with the values from the nearest ancestor (or self).
   *
   * @param ancestry Ordered list of sources, child first to furthest ancestor last.
   * @param sourceToData Map of sources to their current key:values.
   */
  Map flattenSource(List<String> ancestry, Map<String, Map> sourceToData) {
    if (!sourceToData.keySet().containsAll(ancestry)) {
      def missing = ancestry - ancestry.intersect(sourceToData.keySet())
      throw new IllegalArgumentException("Not all sources in ancestry found in source data.\n" +
          "  Missing sources: ${missing}\n" +
          "  Sources in ancestry: ${ancestry}\n" +
          "  Sources in source data: ${sourceToData.keySet()}");
    }

    Map flattened = [:]

    for (def source in ancestry) {
      def data = sourceToData[source]
      if (data != null) {
        for (def entry in sourceToData[source]) {
          flattened[entry.key] = entry.value
        }
      }
    }

    return flattened;
  }

  /**
   * Generates new data for the given source only, taking into account the desired changes, the
   * existing hierarchy, and the existing data in the hierarchy.
   */
  private Map<String, ?> generateSingleSource(Hierarchy hierarchy, Iterable<Change> changes,
      String pivotSource, Map<String, Map> data) {
    def ancestors = hierarchy.ancestorsOf(pivotSource)
        .orElseThrow({new IllegalArgumentException("Could not find pivot source in hierarchy.\n" +
        "  Pivot source: ${pivotSource}\n" +
        "  Hierarchy: ${hierarchy}")})

    def flattenedSourceData = flattenSource(ancestors, data)

    def original = data[pivotSource]
    def result = original == null ? new HashMap<>() : new HashMap<>(original)

    for (source in ancestors.reverse()) {
      def maybeChange = findChangeForSource(source, changes);

      if (!maybeChange.present) continue

      def change = maybeChange.get()

      for (entry in change.set) {
        // Have we already inherited this value (or have it set ourselves)?
        if (flattenedSourceData.containsKey(entry.key) &&
            flattenedSourceData[entry.key] == entry.value) {
          // Is the change not for the source we're updating?
          if (source != pivotSource) {
            // Ensure not present in result source since it would be redundant
            result.remove(entry.key)
          }
          continue
        }

        result[entry.key] = entry.value
      }

      // TODO: Support removing nested keys (keys in a hash)
      for (key in change.remove) {
        result.remove(key)
      }
    }

    return result
  }

  private static Map deepCopy(Map<String, Map> data) {
    def copy = [:]
    data.each { k, v ->
      copy[k] = v == null ? new HashMap<>() : new HashMap<>(v)
    }
    return copy
  }
}

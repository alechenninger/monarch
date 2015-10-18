class Monarch {
  Map generate(Map hierarchy, Iterable<Change> changes, String sourceToChange,
               Map<String, Map> data) {
    def maybeDescendants = getDescendants(sourceToChange, hierarchy)
    def result = data

    // From top-most to inner-most, generate results, taking into account the results from ancestors
    // so far.
    for (def descendant in maybeDescendants.get()) {
      result = generateIgnoringDescendants(hierarchy, changes, descendant, result)
    }

    return result
  }

  Optional<Change> getChangeForSource(String source, Iterable<Change> changes) {
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

  List<String> getAncestors(String source, Map<String, ?> hierarchy, String parent = null) {
    for (entry in hierarchy) {
      def key = entry.key
      def value = entry.value

      if (key == source) {
        return parent == null ? [source] : [source, parent]
      }

      if (value == source) {
        return parent == null ? [source, key] : [source, key, parent]
      }

      if (value instanceof List && value.contains(source)) {
        return parent == null ? [source, key] : [source, key, parent]
      }

      if (value instanceof Map) {
        def a = getAncestors(source, value, key)
        return parent == null ? a : a.with { add(parent); return it }
      }
    }

    // TODO: Should this error instead? Means there is no hierarchy
    return [];
  }

  /**
   * Gets all descendants of a source in the given hierarchy, flattened, where the closest
   * descendants are first. If descendants are the same distance from the source then there is no
   * significance in their ordering; the only guarantee is that further descendants will be later in
   * the list. See {@link #getAllDescendants(java.lang.Object)} for a deeper explanation of the
   * algorithm.
   */
  Optional<List<String>> getDescendants(String source, Map<String, ?> hierarchy) {
    def parents = hierarchy.entrySet().findAll { it.key == source }

    if (parents.size() > 1) {
      throw new IllegalArgumentException()
    }

    if (parents.empty) {
      for (def node in hierarchy.values()) {
        if (node == source) {
          // Source is a leaf
          return Optional.of([source])
        }

        if (node instanceof List && node.contains(source)) {
          // Source is a leaf
          return Optional.of([source])
        }

        if (node instanceof Map) {
          def maybeDescendants = getDescendants(source, node)
          if (maybeDescendants.present) {
            return maybeDescendants
          }
        }
      }

      return Optional.empty()
    }

    if (parents.size() == 1) {
      def parent = parents.first()
      return Optional.of([parent.key].with {
        addAll(getAllDescendants(parent.value))
        return it
      })
    }

    return Optional.empty()
  }

  /**
   * This is kinda gnarly. The goal is to take a nested tree structure of any combination of maps,
   * lists, and strings, and return a list of <em>only</em> strings, in order of <em>nearest to
   * furthest</em>. The leaf nodes will be after the "branch" nodes, and later the deeper in the
   * tree they are.
   *
   * Maps and lists and strings combine to form a tree structure with named string nodes (whether
   * branches or leaves). See the following diagram:
   *
   * <pre><code>
   *            foo (foo is a map because it contains only branches)
   *           /   \
   *         bar   baz (bar is list because it contains a leaf)
   *                   (baz is a map because it contains only branches)
   *        /  \     \
   *       1    2     3  (3 is a list because it contains a leaf. leaves cant be expressed in maps)
   *                 /  \
   *               fizz buzz (buzz is a map with one key, "buzz" and value, "blue")
   *                       \
   *                      blue
   * </code></pre>
   *
   * The order we want from a result should reflect how nested (or not) each node is. The order of
   * this tree is foo, bar, baz, 1, 2, 3, fizz, buzz, blue. Foo is at the top of the tree so it is
   * first. Blue is at the bottom so it is last. Let's walk through how we calculate this.
   *
   * <p>First off, the result needs to be a list. If you have just have a single string, just return
   * a one element list of that.
   *
   * <p>If you have a map, the keys are nearest in the hierarchy; add all those first. Then add the
   * values, whatever they may be. Now you have a list, but it might still have other lists or maps
   * within it. Recurse through the function again with this list. See below for what happens when
   * you pass a list.
   *
   * <p>If you have a list, first make a copy. This will be our list of descendants. If it only
   * contains strings, we're done; just return it. If not, iterate through all of the values, and
   * find the values that are maps or lists. Remove them so you are left with a list of only
   * strings. Then, in order that the maps or lists appeared, create <em>flattened versions</em> of
   * them (described below), and append those to the end of our result list. Since this result list
   * might still have maps or lists as members ("flattened versions" aren't fully flat because we
   * don't want to traverse too deep in any one branch or we'll end up adding elements to our list
   * that are further away too soon), we pass this result again to this function and
   * recurse, starting over with that list.
   *
   * <p>The idea of "flattened versions" of those elements is to unravel them a little bit to get
   * the nearest node values and <em>only</em> the nearest node values. We only want "one level" to
   * keep our ordering correct.
   *
   * <p>A flattened version of a map is a list of all of the keys followed by all of the values.
   *
   * <p>A flattened version of a list is a little more complicated. Start off with itself,
   * obviously, since it is already a list. However, if there are any maps in the list, we want the
   * keys of that map to become elements in the list, and the values to be a list within that list.
   *
   * <p>Now we have a list with any of the lists or maps within it "flattened" as per above
   * description. If there were no maps or lists in
   */
  List<String> getAllDescendants(Object hierarchy) {
    if (hierarchy instanceof String) {
      return [hierarchy] as List<String>
    }

    if (hierarchy instanceof Map) {
      return getAllDescendants([].with {
        it.addAll(hierarchy.keySet())
        it.addAll(hierarchy.values())
        return it
      })
    }

    if (hierarchy instanceof List) {
      def descendants = new ArrayList(hierarchy)
      Map<Integer, List> indexesToFlattened = [:]

      for (int i; i < descendants.size(); i++) {
        def descendant = descendants[i]

        if (descendant instanceof Map) {
          indexesToFlattened[i] = [].with {
            it.addAll(descendant.keySet())
            it.addAll(descendant.values())
            return it
          }
        }

        if (descendant instanceof List) {
          def descendantsWithoutMaps = []
          for (def maybeMap in descendant) {
            if (maybeMap instanceof Map) {
              descendantsWithoutMaps.addAll(maybeMap.keySet())
              descendantsWithoutMaps.add(new ArrayList(maybeMap.values()))
            } else {
              descendantsWithoutMaps.add(maybeMap)
            }
          }
          indexesToFlattened[i] = descendantsWithoutMaps
        }
      }

      def removed = 0;
      for (def entry in indexesToFlattened) {
        descendants.removeAt(entry.key - removed)
        descendants.addAll(entry.value)
        removed++

      }

      if (indexesToFlattened.isEmpty()) {
        return descendants
      }

      return getAllDescendants(descendants)
    }

    throw new IllegalArgumentException("Expected hierarchy to be a List, Map, or String, but " +
        "got ${hierarchy}")
  }
/**
   * Flattens a hierarchy of key:values into a single map. It's a "view" of the data from a child's
   * point of view: all keys in the hierarchy, with the values from the nearest ancestor.
   *
   * @param ancestry Ordered list of sources, child first to furthest ancestor last.
   * @param sourceToData Map of sources to their current key:values.
   */
  Map flattenHierarchy(List<String> ancestry, Map<String, Map> sourceToData) {
    if (!sourceToData.keySet().containsAll(ancestry)) {
      def missing = ancestry - ancestry.intersect(sourceToData.keySet())
      throw new IllegalArgumentException("Not all sources in ancestry found in source data.\n" +
          "  Missing sources: ${missing}\n" +
          "  Sources in ancestry: ${ancestry}\n" +
          "  Sources in source data: ${sourceToData.keySet()}");
    }

    Map flattened = [:]

    for (def source in ancestry) {
      for (def entry in sourceToData[source]) {
        flattened[entry.key] = entry.value
      }
    }

    return flattened;
  }

  private static Map generateIgnoringDescendants(Map hierarchy, Iterable<Change> changes,
                                                 String sourceToChange, Map<String, Map> data) {
    def ancestry = getAncestors(sourceToChange, hierarchy);
    def result = deepCopy(data);
    def flattenedSourceData = flattenHierarchy(ancestry, data)

    for (source in ancestry.reverse()) {
      def maybeChange = getChangeForSource(source, changes);

      if (!maybeChange.present) continue;

      def change = maybeChange.get();

      for (entry in change.set) {
        if (flattenedSourceData.containsKey(entry.key) &&
            flattenedSourceData[entry.key] == entry.value) {
          // Is this source an ancestor?
          if (source != sourceToChange) {
            // Ensure not present lower in the hierarchy since it would be redundant
            result[sourceToChange].remove(entry.key)
          }
          continue
        }

        result[sourceToChange][entry.key] = entry.value
      }

      // TODO: Support removing nested keys (keys in a hash)
      for (key in change.remove) {
        result[sourceToChange].remove(key);
      }
    }

    return result;
  }

  private static Map deepCopy(Map<String, Map> data) {
    def copy = [:]
    data.each { k, v ->
      copy[k] = v == null ? new HashMap<>() : new HashMap<>(v)
    }
    return copy
  }
}

class Change {
  final String source
  final Map<String, ?> set
  final List remove

  Change(source, set = [:], remove = []) {
    this.source = source
    this.set = set
    this.remove = remove
  }

  static Change fromMap(Map map) {
    return new Change(map['source'], map['set'] ?: [:], map['remove'] ?: [])
  }
}

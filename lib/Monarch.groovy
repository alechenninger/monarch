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
  Map<String, Map> generateHierarchy(Map hierarchy, Iterable<Change> changes, String pivotSource,
      Map<String, Map> data) {
    def maybeDescendants = getDescendants(pivotSource, hierarchy)
    def result = deepCopy(data)

    // From top-most to inner-most, generate results, taking into account the results from ancestors
    // as we go along.
    for (def descendant in maybeDescendants.get()) {
      result[descendant] = generateSingleSource(hierarchy, changes, descendant, result)
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

  List<String> getAncestors(String source, Map<String, ?> hierarchy) {
    return Hierarchy.fromStringListOrMap(hierarchy).ancestorsOf(source).orElseThrow({ new IllegalArgumentException()})
  }

  /**
   * Gets all descendants of a source in the given hierarchy, flattened, where the closest
   * descendants are first. If descendants are the same distance from the source then there is no
   * significance in their ordering; the only guarantee is that further descendants will be later in
   * the list. See {@link #getAllDescendants(java.lang.Object)} for a deeper explanation of the
   * algorithm.
   */
  Optional<List<String>> getDescendants(String source, Map<String, ?> hierarchy) {
    return Hierarchy.fromStringListOrMap(hierarchy).findDescendant(source).map({
      it.descendantsNearestToFurthest()
    })
  }

  List<String> getAllDescendants(Object hierarchy) {
    return Hierarchy.fromStringListOrMap(hierarchy).descendantsNearestToFurthest()
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
  private Map<String, ?> generateSingleSource(Map hierarchy, Iterable<Change> changes,
      String sourceToChange, Map<String, Map> data) {
    def ancestors = getAncestors(sourceToChange, hierarchy)
    def flattenedSourceData = flattenHierarchy(ancestors, data)
    def original = data[sourceToChange]
    def result = original == null ? new HashMap<>() : new HashMap<>(original)

    for (source in ancestors.reverse()) {
      def maybeChange = getChangeForSource(source, changes);

      if (!maybeChange.present) continue

      def change = maybeChange.get()

      for (entry in change.set) {
        // Have we already inherited this value (or have it set ourselves)?
        if (flattenedSourceData.containsKey(entry.key) &&
            flattenedSourceData[entry.key] == entry.value) {
          // Is the change not for the source we're updating?
          if (source != sourceToChange) {
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

class Hierarchy {
  private final Node hierarchy;

  Hierarchy(List<Node> nodes) {
    if (nodes.size() == 1) {
      this.hierarchy = nodes.first()
    } else {
      hierarchy = new Node(null, null)
      for (def node in nodes) {
        hierarchy.append(node)
      }
    }
  }

  Hierarchy(Node node) {
    this.hierarchy = Objects.requireNonNull(node, "node")
  }

  static Hierarchy fromStringListOrMap(Object object) {
    return new Hierarchy(nodesFromStringListOrMap(object))
  }

  /**
   * Returns all of the leaf node names in order of <em>nearest to furthest</em>. The leaf nodes
   * will be after the "branch" nodes, and later the deeper in the tree they are.
   *
   * <p>For example, given the following tree structure:
   *
   * <pre><code>
   *            foo
   *           /   \
   *         bar   baz
   *        /  \     \
   *       1    2     3
   *                 /  \
   *               fizz buzz
   *                       \
   *                      blue
   * </code></pre>
   *
   * The depth-order is foo, bar, baz, 1, 2, 3, fizz, buzz, blue. Foo is at the top of the tree so
   * it is first. Blue is at the bottom so it is last.
   */
  List<String> descendantsNearestToFurthest() {
    def children = hierarchy.children()
    if (children.empty) {
      return [hierarchy.name()] as List<String>
    }

    return new DescendantsIterator(hierarchy).toList().collect {
      it.name() as String
    }
  }

  Optional<Hierarchy> findDescendant(String name) {
    def found = new DescendantsIterator(hierarchy).findAll { it.name() == name }

    if (found.empty) return Optional.empty()
    if (found.size() == 1) return Optional.of(new Hierarchy(found.first()))

    throw new IllegalStateException()
  }

  Optional<List<String>> ancestorsOf(String name) {
    def found = new DescendantsIterator(hierarchy).findAll { it.name() == name }

    if (found.empty) return Optional.empty()
    if (found.size() > 1) {
      throw new IllegalStateException()
    }

    def ancestors = new AncestorsIterator(found.first()).toList()
    return Optional.of(ancestors.collect {it.name()})
  }

  private class DescendantsIterator implements Iterator<Node> {
    private Queue<Node> currentLevel = new LinkedList<>()
    private Queue<Node> nextLevel = new LinkedList<>()

    DescendantsIterator(Node node) {
      currentLevel.add(node)
    }

    @Override
    boolean hasNext() {
      return !(currentLevel.empty && nextLevel.empty)
    }

    @Override
    Node next() {
      if (currentLevel.empty) {
        currentLevel = nextLevel
        if (currentLevel == null) {
          throw new IndexOutOfBoundsException()
        }
        return next()
      }

      def next = currentLevel.poll()

      def nextChildren = next.children()
      if (!nextChildren.empty) {
        nextLevel.addAll(nextChildren)
      }

      return next
    }
  }

  class AncestorsIterator implements Iterator<Node> {
    private Node next

    AncestorsIterator(Node node) {
      this.next = node
    }

    @Override
    boolean hasNext() {
      return next != null
    }

    @Override
    Node next() {
      if (next == null) throw new IndexOutOfBoundsException()
      def current = next
      next = current.parent()
      return current;
    }
  }

  private static List<Node> nodesFromStringListOrMap(Object object) {
    if (object instanceof String) {
      return [new Node(null, object, new HashMap(), null)]
    }

    if (object instanceof List) {
      return nodesFromList(object)
    }

    if (object instanceof Map) {
      return nodesFromMap(object)
    }

    throw new IllegalArgumentException("Can only parse Strings, Lists, or Maps, but got ${object}")
  }

  private static List<Node> nodesFromMap(Map<String, ?> map) {
    def nodes = []
    for (def entry in map) {
      def keyNode = new Node(null, entry.key)
      def valueNodes = nodesFromStringListOrMap(entry.value)
      if (valueNodes.size() == 1) {
        keyNode.append(valueNodes.first())
      } else {
        for (def node in valueNodes) {
          keyNode.append(node)
        }
      }
      nodes.add(keyNode)
    }
    return nodes
  }

  private static List<Node> nodesFromList(List<?> list) {
    def nodes = []
    for (def element in list) {
      def elementNodes = nodesFromStringListOrMap(element)

      if (elementNodes.size() == 1) {
        nodes.add(elementNodes.first())
      } else {
        nodes.addAll(elementNodes)
      }
    }

    return nodes
  }
}

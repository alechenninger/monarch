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
  List<String> descendants() {
    def children = hierarchy.children()
    if (children.empty) {
      return [hierarchy.name()] as List<String>
    }

    return new DescendantsIterator(hierarchy).toList().collect {
      it.name() as String
    }
  }

  Optional<List<String>> descendantsOf(String source) {
    return hierarchyOf(source).map { it.descendants() }
  }

  Optional<List<String>> ancestorsOf(String source) {
    def found = new DescendantsIterator(hierarchy).findAll { it.name() == source }

    if (found.empty) return Optional.empty()
    if (found.size() > 1) {
      throw new IllegalStateException()
    }

    def ancestors = new AncestorsIterator(found.first()).toList()
    return Optional.of(ancestors.collect { it.name() })
  }

  /**
   * Finds a descendant source node and returns it as the root of a new {@link Hierarchy}.
   */
  Optional<Hierarchy> hierarchyOf(String source) {
    def found = new DescendantsIterator(hierarchy).findAll { it.name() == source }

    if (found.empty) return Optional.empty()
    if (found.size() == 1) return Optional.of(new Hierarchy(found.first()))

    throw new IllegalStateException()
  }

  String toString() {
    return nodeToString(hierarchy)
  }

  private static String nodeToString(Node node, StringBuilder sb = new StringBuilder()) {
    return sb.with {
      append(node)
      def children = node.children()
      if (!children.empty) {
        for (def child in children) {
          append('\n')
          append(nodeToString(child).eachLine { "|- " + it })
        }
      }
      append('\n')
      return it
    }
  }

  private class DescendantsIterator implements Iterator<Node> {
    private Queue<Node> currentLevel = new LinkedList<>()
    private Queue<Node> nextLevel = new LinkedList<>()

    private int depth = 1;

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
        depth++
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

  private class AncestorsIterator implements Iterator<Node> {
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
      for (def node in valueNodes) {
        keyNode.append(node)
      }
      nodes.add(keyNode)
    }
    return nodes
  }

  private static List<Node> nodesFromList(List<?> list) {
    def nodes = []
    for (def element in list) {
      def elementNodes = nodesFromStringListOrMap(element)
      nodes.addAll(elementNodes)
    }

    return nodes
  }
}

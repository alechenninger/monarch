class Hierarchy {
  private final Node rootNode;

  Hierarchy(List<Node> nodes) {
    if (nodes.size() == 1) {
      this.rootNode = Objects.requireNonNull(nodes.first(), "rootNode")
    } else {
      rootNode = new Node(null, null)
      for (def node in nodes) {
        rootNode.append(node)
      }
    }
  }

  Hierarchy(Node rootNode) {
    this.rootNode = Objects.requireNonNull(rootNode, "rootNode")
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
    def children = rootNode.children()
    if (children.empty) {
      return [rootNode.name()] as List<String>
    }

    return new DescendantsIterator(rootNode).toList().collect {
      it.name() as String
    }
  }

  Optional<List<String>> descendantsOf(String source) {
    return hierarchyOf(source).map { it.descendants() }
  }

  Optional<List<String>> ancestorsOf(String source) {
    def found = new DescendantsIterator(rootNode).findAll { it.name() == source }

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
    def found = new DescendantsIterator(rootNode).findAll { it.name() == source }

    if (found.empty) return Optional.empty()
    if (found.size() == 1) return Optional.of(new Hierarchy(found.first()))

    throw new IllegalStateException()
  }

  @Override
  boolean equals(o) {
    if (this.is(o)) return true
    if (getClass() != o.class) return false

    Hierarchy hierarchy = (Hierarchy) o

    if (rootNode != hierarchy.rootNode) return false

    return true
  }

  @Override
  int hashCode() {
    return rootNode.hashCode()
  }

  @Override
  String toString() {
    return "[${nodeToString(rootNode)}]"
  }

  private static String nodeToString(Node node, StringBuilder sb = new StringBuilder()) {
    return sb.with {
      append(node.name())
      def children = node.children()
      if (!children.empty) {
        for (def child in children) {
          append('\n')
          append(nodeToString(child)
              .readLines()
              .collect {"  " + it}
              .inject {string, line -> string + "\n" + line})
        }
      }
      append('\n')
      return it
    }.toString()
  }

  private static class DescendantsIterator implements Iterator<Node> {
    private Queue<Node> currentLevel = new LinkedList<>()

    DescendantsIterator(Node node) {
      currentLevel.add(node)
    }

    @Override
    boolean hasNext() {
      return !currentLevel.empty
    }

    @Override
    Node next() {
      def next = currentLevel.remove()

      def nextChildren = next.children()
      if (!nextChildren.empty) {
        currentLevel.addAll(nextChildren)
      }

      return next
    }
  }

  private static class AncestorsIterator implements Iterator<Node> {
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

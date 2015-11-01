package io.github.alechenninger.monarch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Hierarchy {
  private final Node rootNode;

  public Hierarchy(List<Node> nodes) {
    if (nodes.size() == 1) {
      this.rootNode = Objects.requireNonNull(nodes.get(0), "rootNode");
    } else {
      rootNode = new Node(null);
      for (Node node : nodes) {
        rootNode.append(node);
      }
    }
  }

  public Hierarchy(Node rootNode) {
    this.rootNode = Objects.requireNonNull(rootNode, "rootNode");
  }

  public static Hierarchy fromStringListOrMap(Object object) {
    return new Hierarchy(nodesFromStringListOrMap(object));
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
  public List<String> descendants() {
    Collection<Node> children = rootNode.children();
    if (children.isEmpty()) {
      return Collections.singletonList(rootNode.name());
    }

    return DescendantsIterator.asStream(rootNode)
        .map(Node::name)
        .collect(Collectors.toList());
  }

  Optional<List<String>> descendantsOf(String source) {
    return hierarchyOf(source).map(Hierarchy::descendants);
  }

  Optional<List<String>> ancestorsOf(String source) {
    return DescendantsIterator.asStream(rootNode)
        .filter(n -> Objects.equals(source, n.name()))
        .collect(Collect.<Node>maxOneResultOrThrow(IllegalStateException::new))
        .map(n -> AncestorsIterator.asStream(n).map(Node::name).collect(Collectors.toList()));
  }

  /**
   * Finds a descendant source node and returns it as the root of a new {@link Hierarchy}.
   */
  Optional<Hierarchy> hierarchyOf(String source) {
    return DescendantsIterator.asStream(rootNode)
        .filter(n -> Objects.equals(source, n.name()))
        .collect(Collect.<Node>maxOneResultOrThrow(IllegalStateException::new))
        .map(Hierarchy::new);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Hierarchy hierarchy = (Hierarchy) o;
    return Objects.equals(rootNode, hierarchy.rootNode);
  }

  @Override
  public int hashCode() {
    return rootNode.hashCode();
  }

  @Override
  public String toString() {
    return "[" + nodeToString(rootNode) + "]";
  }

  private static String nodeToString(Node node) {
    StringBuilder sb = new StringBuilder();

    sb.append(node.name());

    for (Node child : node.children()) {
      sb.append('\n');

      Stream<String> lines = Arrays.stream(nodeToString(child).split("\n"));

      sb.append(lines
          .map(s -> "  " + s)
          .reduce("", (string, line) -> string + "\n" + line));
    }

    sb.append('\n');

    return sb.toString();
  }

  private static class DescendantsIterator implements Iterator<Node> {
    private Queue<Node> currentLevel = new LinkedList<>();

    DescendantsIterator(Node node) {
      currentLevel.add(node);
    }

    static Stream<Node> asStream(Node node) {
      Iterable<Node> descendantsIterable = () -> new DescendantsIterator(node);
      return StreamSupport.stream(descendantsIterable.spliterator(), false);
    }

    @Override
    public boolean hasNext() {
      return !currentLevel.isEmpty();
    }

    @Override
    public Node next() {
      Node next = currentLevel.remove();

      Collection<Node> nextChildren = next.children();
      if (!nextChildren.isEmpty()) {
        currentLevel.addAll(nextChildren);
      }

      return next;
    }
  }

  private static class AncestorsIterator implements Iterator<Node> {
    private Node next;

    AncestorsIterator(Node node) {
      this.next = node;
    }

    static Stream<Node> asStream(Node node) {
      Iterable<Node> ancestorsIterable = () -> new AncestorsIterator(node);
      return StreamSupport.stream(ancestorsIterable.spliterator(), false);
    }

    @Override
    public boolean hasNext() {
      return next != null;
    }

    @Override
    public Node next() {
      if (next == null) throw new IndexOutOfBoundsException();
      Node current = next;
      next = current.parent();
      return current;
    }
  }

  private static List<Node> nodesFromStringListOrMap(Object object) {
    if (object instanceof String) {
      return Collections.singletonList(new Node((String) object));
    }

    if (object instanceof List) {
      return nodesFromList((List) object);
    }

    if (object instanceof Map) {
      return nodesFromMap((Map) object);
    }

    throw new IllegalArgumentException("Can only parse Strings, Lists, or Maps, but got " + object);
  }

  private static List<Node> nodesFromMap(Map<String, Object> map) {
    List<Node> nodes = new ArrayList<>();

    for (Map.Entry<String, Object> entry : map.entrySet()) {
      Node keyNode = new Node(entry.getKey());
      List<Node> valueNodes = nodesFromStringListOrMap(entry.getValue());

      for (Node node : valueNodes) {
        keyNode.append(node);
      }

      nodes.add(keyNode);
    }

    return nodes;
  }

  private static List<Node> nodesFromList(List<?> list) {
    List<Node> nodes = new ArrayList<>();

    for (Object element : list) {
      List<Node> elementNodes = nodesFromStringListOrMap(element);
      nodes.addAll(elementNodes);
    }

    return nodes;
  }

}


/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2015  Alec Henninger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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

class StaticHierarchy implements Hierarchy {
  private final List<Node> rootNodes;

  StaticHierarchy(List<Node> rootNodes) {
    Objects.requireNonNull(rootNodes, "rootNodes");
    this.rootNodes = new ArrayList<>(rootNodes);
  }

  StaticHierarchy(Node rootNodes) {
    Objects.requireNonNull(rootNodes, "rootNode");
    this.rootNodes = Collections.singletonList(rootNodes);
  }

  @Override
  public Optional<Source> sourceFor(String source) {
    return DescendantsIterator.asStream(rootNodes)
        .filter(n -> Objects.equals(source, n.name()))
        .map(StaticSource::new)
        .collect(Collect.maxOneResultOrThrow(IllegalStateException::new));
  }

  @Override
  public Optional<Source> sourceFor(Map<String, String> variables) {
    throw new UnsupportedOperationException("Statically defined hierarchies do not support " +
        "identifying a source via variables.");
  }

  public List<Source> descendants() {
    return DescendantsIterator.asStream(rootNodes)
        .map(StaticSource::new)
        .collect(Collectors.toList());
  }

  public Optional<List<String>> ancestorsOf(String source) {
    return DescendantsIterator.asStream(rootNodes)
        .filter(n -> Objects.equals(source, n.name()))
        .collect(Collect.<Node>maxOneResultOrThrow(IllegalStateException::new))
        .map(n -> AncestorsIterator.asStream(n).map(Node::name).collect(Collectors.toList()));
  }

  public Optional<Hierarchy> hierarchyOf(String source) {
    return DescendantsIterator.asStream(rootNodes)
        .filter(n -> Objects.equals(source, n.name()))
        .collect(Collect.<Node>maxOneResultOrThrow(IllegalStateException::new))
        .map(StaticHierarchy::new);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StaticHierarchy hierarchy = (StaticHierarchy) o;
    return Objects.equals(rootNodes, hierarchy.rootNodes);
  }

  @Override
  public int hashCode() {
    return rootNodes.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("[\n");

    for (Node rootNode : rootNodes) {
      sb.append(prefixLines(nodeToString(rootNode), "  "));
    }

    return sb.append("]").toString();
  }

  private static String nodeToString(Node node) {
    StringBuilder sb = new StringBuilder();

    sb.append(node.name());
    sb.append('\n');

    for (Node child : node.children()) {
      sb.append(prefixLines(nodeToString(child), "  "));
    }

    return sb.toString();
  }

  private static String prefixLines(String toPrefix, String prefix) {
    Stream<String> lines = Arrays.stream(toPrefix.split("\n"));

    return lines
        .map(s -> prefix + s)
        .collect(Collectors.joining("\n")) + '\n';
  }

  private static class StaticSource extends AbstractSource {
    private final Node source;

    private StaticSource(Node source) {
      this.source = source;
    }

    @Override
    public String path() {
      return source.name();
    }

    @Override
    public List<Source> lineage() {
      return AncestorsIterator.asStream(source)
          .map(StaticSource::new)
          .collect(Collectors.toList());
    }

    @Override
    public List<Source> descendants() {
      return DescendantsIterator.asStream(Collections.singleton(source))
          .map(StaticSource::new)
          .collect(Collectors.toList());
    }

  }

  private static class DescendantsIterator implements Iterator<Node> {
    private Queue<Node> currentLevel = new LinkedList<>();

    DescendantsIterator(Collection<Node> nodes) {
      currentLevel.addAll(nodes);
    }

    static Stream<Node> asStream(Collection<Node> nodes) {
      Iterable<Node> descendantsIterable = () -> new DescendantsIterator(nodes);
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

  static class Node {
    private final String name;
    private final Collection<Node> children = new LinkedList<>();

    private Node parent;

    Node(String name) {
      this.name = name;
    }

    static List<Node> fromStringListOrMap(Object object) {
      if (object instanceof String) {
        return Collections.singletonList(new Node((String) object));
      }

      if (object instanceof List) {
        return fromList((List) object);
      }

      if (object instanceof Map) {
        return fromMap((Map) object);
      }

      throw new IllegalArgumentException("Can only parse Strings, Lists, or Maps, but got " + object);
    }

    static List<Node> fromMap(Map<String, Object> map) {
      List<Node> nodes = new ArrayList<>();

      for (Map.Entry<String, Object> entry : map.entrySet()) {
        Node keyNode = new Node(entry.getKey());
        List<Node> valueNodes = fromStringListOrMap(entry.getValue());

        for (Node node : valueNodes) {
          keyNode.append(node);
        }

        nodes.add(keyNode);
      }

      return nodes;
    }

    static List<Node> fromList(List<?> list) {
      List<Node> nodes = new ArrayList<>();

      for (Object element : list) {
        List<Node> elementNodes = fromStringListOrMap(element);
        nodes.addAll(elementNodes);
      }

      return nodes;
    }

    Node parent() {
      return parent;
    }

    Node append(Node child) {
      Optional.ofNullable(child.parent).ifPresent(p -> p.children.remove(child));
      child.parent = this;
      children.add(child);
      return this;
    }

    Collection<Node> children() {
      return Collections.unmodifiableCollection(children);
    }

    String name() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Node node = (Node) o;
      if (parent != null) {
        if (node.parent == null) {
          return false;
        }

        if (!parent.name.equals(node.parent.name)) {
          return false;
        }
      }

      return Objects.equals(name, node.name) &&
          Objects.equals(children, node.children);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, children, parent);
    }

    @Override
    public String toString() {
      return "Node{" +
          "name='" + name + '\'' +
          ", children=" + children +
          ", parent=" + (parent == null ? null : parent.name) +
          '}';
    }
  }
}


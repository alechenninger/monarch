package io.github.alechenninger.monarch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final class Node {
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

  public Node parent() {
    return parent;
  }

  public Node append(Node child) {
    Optional.ofNullable(child.parent).ifPresent(p -> p.children.remove(child));
    child.parent = this;
    children.add(child);
    return this;
  }

  public Collection<Node> children() {
    return Collections.unmodifiableCollection(children);
  }

  public String name() {
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
    return Objects.equals(name, node.name) &&
        Objects.equals(children, node.children) &&
        Objects.equals(parent, node.parent);
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
        ", parent=" + parent +
        '}';
  }
}

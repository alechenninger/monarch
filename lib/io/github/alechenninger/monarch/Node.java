package io.github.alechenninger.monarch;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;

final class Node {
  private final String name;
  private final Collection<Node> children = new LinkedList<>();

  private Node parent;

  Node(String name) {
    this.name = name;
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

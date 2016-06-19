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
  public List<String> descendants() {
    return DescendantsIterator.asStream(rootNodes)
        .map(Node::name)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<List<String>> descendantsOf(String source) {
    return hierarchyOf(source).map(Hierarchy::descendants);
  }

  @Override
  public Optional<List<String>> descendantsOf(Map<String, String> variables) {
    return Optional.empty();
  }

  @Override
  public Optional<List<String>> ancestorsOf(String source) {
    return DescendantsIterator.asStream(rootNodes)
        .filter(n -> Objects.equals(source, n.name()))
        .collect(Collect.<Node>maxOneResultOrThrow(IllegalStateException::new))
        .map(n -> AncestorsIterator.asStream(n).map(Node::name).collect(Collectors.toList()));
  }

  @Override
  public Optional<List<String>> ancestorsOf(Map<String, String> variables) {
    return Optional.empty();
  }

  @Override
  public Optional<Hierarchy> hierarchyOf(String source) {
    return DescendantsIterator.asStream(rootNodes)
        .filter(n -> Objects.equals(source, n.name()))
        .collect(Collect.<Node>maxOneResultOrThrow(IllegalStateException::new))
        .map(StaticHierarchy::new);
  }

  @Override
  public Optional<Hierarchy> hierarchyOf(Map<String, String> variables) {
    return Optional.empty();
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

}


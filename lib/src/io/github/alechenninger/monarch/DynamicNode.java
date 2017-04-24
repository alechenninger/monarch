/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2016 Alec Henninger
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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface DynamicNode {
  static List<DynamicNode> fromInterpolated(List<String> nodes) {
    return nodes.stream()
        .map(InterpolatedDynamicNode::new)
        .collect(Collectors.toList());
  }

  List<String> variables();

  default RenderedNode renderOne(Assignments assignments) {
    List<RenderedNode> rendered = render(assignments);
    if (rendered.size() != 1) {
      throw new IllegalArgumentException("Assignments do not cover all variables.");
    }
    return rendered.get(0);
  }

  List<RenderedNode> render(Assignments assignments);

  default Optional<Assignments> assignmentsFor(String source,
      Inventory potentials, Assignments variables) {
    return render(variables).stream()
        .filter(s -> s.path().equals(source))
        .map(RenderedNode::usedAssignments)
        // TODO validate only one found?
        .findFirst()
        .map(potentials::assignAll);
  }

  final class RenderedNode {
    private final DynamicNode node;
    private final String path;
    private final Set<Assignment> usedAssignments;
    private final int hash;

    public RenderedNode(String path, Set<Assignment> usedAssignments, DynamicNode node) {
      this.path = Objects.requireNonNull(path, "path");
      this.usedAssignments = Objects.requireNonNull(usedAssignments, "usedAssignments");
      this.node = Objects.requireNonNull(node, "node");

      hash = Objects.hash(node, path, usedAssignments);
    }

    public String path() {
      return path;
    }

    public Set<Assignment> usedAssignments() {
      return usedAssignments;
    }

    public DynamicNode node() {
      return node;
    }

    @Override
    public String toString() {
      return "RenderedNode{" +
          "node=" + node +
          ", path='" + path + '\'' +
          ", usedAssignments=" + usedAssignments +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      RenderedNode that = (RenderedNode) o;
      return Objects.equals(node, that.node) &&
          Objects.equals(path, that.path) &&
          Objects.equals(usedAssignments, that.usedAssignments);
    }

    @Override
    public int hashCode() {
      return hash;
    }
  }
}

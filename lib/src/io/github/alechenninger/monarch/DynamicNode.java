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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public interface DynamicNode {
  static List<DynamicNode> fromInterpolated(List<String> nodes) {
    return nodes.stream()
        .map(InterpolatedDynamicNode::new)
        .collect(Collectors.toList());
  }

  // TODO: need clearer terminology around variables, their names, and their values
  List<String> variables();

  // TODO: Add renderOne which accepts just variables that we expect to cover all of variables()

  // TODO: Should this consider implied variables?
  List<RenderedNode> render(Map<String, String> variables, Map<String, List<Potential>> potentials);

  default Optional<Map<String, String>> variablesFor(String source,
      Map<String, List<Potential>> potentials, Map<String, String> variables) {
    return render(variables, potentials).stream()
        .filter(s -> s.path().equals(source))
        .map(RenderedNode::variablesUsed)
        // TODO validate only one found?
        .findFirst();
  }

  final class RenderedNode {
    private final String path;
    private final Map<String, String> variablesUsed;

    public RenderedNode(String path, Map<String, String> variablesUsed) {
      this.path = Objects.requireNonNull(path);
      this.variablesUsed = Collections.unmodifiableMap(variablesUsed);
    }

    public String path() {
      return path;
    }

    public Map<String, String> variablesUsed() {
      return variablesUsed;
    }

    @Override
    public String toString() {
      return "RenderedNode{" +
          "path='" + path + '\'' +
          ", variablesUsed=" + variablesUsed +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      RenderedNode that = (RenderedNode) o;
      return Objects.equals(path, that.path) &&
          Objects.equals(variablesUsed, that.variablesUsed);
    }

    @Override
    public int hashCode() {
      return Objects.hash(path, variablesUsed);
    }
  }
}

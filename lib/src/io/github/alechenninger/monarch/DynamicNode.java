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
import java.util.Optional;
import java.util.stream.Collectors;

public interface DynamicNode {
  static List<DynamicNode> fromInterpolated(List<String> nodes) {
    return nodes.stream()
        .map(InterpolatedDynamicNode::new)
        .collect(Collectors.toList());
  }

  List<String> variables();

  List<RenderedNode> render(Map<String, String> variables, Map<String, List<String>> potentials);

  default Optional<Map<String, String>> variablesFor(String source,
      Map<String, List<String>> potentials, Map<String, String> variables) {
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
      this.path = path;
      this.variablesUsed = Collections.unmodifiableMap(variablesUsed);
    }

    public String path() {
      return path;
    }

    public Map<String, String> variablesUsed() {
      return variablesUsed;
    }
  }
}
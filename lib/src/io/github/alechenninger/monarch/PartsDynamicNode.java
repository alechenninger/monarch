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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PartsDynamicNode implements DynamicNode {
  private final List<Part> parts;

  public PartsDynamicNode(List<Part> parts) {
    this.parts = parts;
  }

  public static class Part {
    final String string;
    final boolean isVariable;

    Part(String string, boolean isVariable) {
      this.string = string;
      this.isVariable = isVariable;
    }

    public static Part string(String string) {
      return new Part(string, false);
    }

    public static Part variable(String variable) {
      return new Part(variable, true);
    }

    @Override
    public String toString() {
      return "Part." + (isVariable ? "variable" : "string") + "('" + string + "')";
    }
  }

  @Override
  public List<String> variables() {
    return parts.stream()
        .filter(p -> p.isVariable)
        .map(p -> p.string)
        .collect(Collectors.toList());
  }

  @Override
  public List<RenderedNode> render(Assignments variables) {
    return variables.possibleAssignments(variables()).stream()
        .map(combination -> {
          Set<Assignment> usedAssignments = new HashSet<>();
          StringBuilder path = new StringBuilder();

          for (Part part : parts) {
            if (part.isVariable) {
              if (!combination.isAssigned(part.string)) {
                throw new IllegalArgumentException("No variable value found for variable: " +
                    part.string);
              }

              Assignment assignment = combination.forVariable(part.string);
              usedAssignments.add(assignment);
              path.append(assignment.value());
            } else {
              path.append(part.string);
            }
          }

          return new RenderedNode(path.toString(), usedAssignments);
        })
        .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return parts.toString();
  }

}

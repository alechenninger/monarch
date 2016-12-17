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

import org.bigtesting.interpolatd.Interpolator;
import org.bigtesting.interpolatd.Substitutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class InterpolatedDynamicNode implements DynamicNode {
  private final String expression;
  private final String variableOpening;
  private final String variableClosing;
  private final Optional<String> escapeCharacter;

  private final List<String> variableNames;

  public InterpolatedDynamicNode(String expression) {
    this(expression, "%{", "}", Optional.of("\\"));
  }

  public InterpolatedDynamicNode(String expression, String variableOpening, String variableClosing,
      Optional<String> escapeCharacter) {
    this.expression = expression;
    this.variableOpening = variableOpening;
    this.variableClosing = variableClosing;
    this.escapeCharacter = escapeCharacter;

    List<String> variableNames = new ArrayList<>();
    Interpolator<Void> paramCapture = getInterpolator((captured, arg) -> {
      variableNames.add(captured);
      return null;
    });
    paramCapture.interpolate(expression, null);

    this.variableNames = Collections.unmodifiableList(variableNames);
  }

  @Override
  public List<String> variables() {
    return variableNames;
  }

  @Override
  public List<RenderedNode> render(Assignments assignments) {
    if (variableNames.isEmpty()) {
      return Collections.singletonList(new RenderedNode(expression, Collections.emptySet()));
    }

    return VariableCombinations.stream(variableNames, assignments)
        .map(combination -> {
          Set<Assignment> usedAssignments = new HashSet<>();
          Interpolator<Map<String, String>> interpolator = getInterpolator((captured, arg) -> {
            if (!combination.isAssigned(captured)) {
              throw new IllegalStateException("No value defined for variable: " + captured);
            }
            Assignment assignment = combination.forVariable(captured);
            usedAssignments.add(assignment);
            return assignment.value();
          });

          String path = interpolator.interpolate(expression, combination.toMap());

          // TODO: Using combination here not the same thing as variables used I think
          return new RenderedNode(path, usedAssignments);
        })
        .collect(Collectors.toList());
  }

  private <T> Interpolator<T> getInterpolator(Substitutor<T> substitutor) {
    Interpolator<T> interpolator = new Interpolator<>();
    interpolator.when().enclosedBy(variableOpening).and(variableClosing).handleWith(substitutor);
    escapeCharacter.ifPresent(interpolator::escapeWith);
    return interpolator;
  }

  @Override
  public String toString() {
    return "InterpolatedDynamicNode{" +
        "expression='" + expression + '\'' +
        ", variableOpening='" + variableOpening + '\'' +
        ", variableClosing='" + variableClosing + '\'' +
        ", escapeCharacter=" + escapeCharacter +
        ", variableNames=" + variableNames +
        ", riables=" + variables() +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    InterpolatedDynamicNode that = (InterpolatedDynamicNode) o;
    return Objects.equals(expression, that.expression) &&
        Objects.equals(variableOpening, that.variableOpening) &&
        Objects.equals(variableClosing, that.variableClosing) &&
        Objects.equals(escapeCharacter, that.escapeCharacter) &&
        Objects.equals(variableNames, that.variableNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(expression, variableOpening, variableClosing, escapeCharacter,
        variableNames);
  }
}

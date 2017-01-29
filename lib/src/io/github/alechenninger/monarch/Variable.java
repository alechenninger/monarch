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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Variable {
  private final String name;
  private final List<Assignable> assignables;
  private final Inventory inventory;

  public Variable(String name, List<Assignable> assignables, Inventory inventory) {
    this.name = Objects.requireNonNull(name, "name");
    this.assignables = Objects.requireNonNull(assignables, "assignables");
    this.inventory = Objects.requireNonNull(inventory, "inventory");

    if (assignables.isEmpty()) {
      throw new IllegalArgumentException("Must provide at least one assignable value for " +
          "variable. variable=" + name);
    }

    List<Map.Entry<String, Long>> duplicates = assignables.stream()
        .collect(Collectors.groupingBy(Assignable::value, Collectors.counting()))
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() > 1)
        .collect(Collectors.toList());

    if (!duplicates.isEmpty()) {
      throw new IllegalArgumentException("List of assignables for variable <" + name + "> " +
          "contains duplicate values. Values to occurrences: " + duplicates);
    }
  }

  public String name() {
    return name;
  }

  public Set<String> values(Assignments assignments) {
    if (assignments.isAssigned(name)) {
      return Collections.singleton(assignments.forVariable(name).value());
    }

    Set<String> values = new LinkedHashSet<>(assignables.size());

    for (Assignable assignable : assignables) {
      Assignment assignment = assign(assignable.value());
      if (assignment.conflictsWith(assignments)) continue;
      values.add(assignment.value());
    }

    return values;
  }

  public Assignment assign(String value) {
    Optional<Assignable> assignable = assignables.stream()
        .filter(a -> a.value().equals(value))
        .findFirst();

    if (!assignable.isPresent()) {
      throw new IllegalArgumentException("Cannot assign value <" + value + "> to variable <" +
          name + "> because value is not assignable for this variable. Check your assignment or " +
          "add value to inventory. An inventory needs to be comprehensive so it can be used to " +
          "discover all of the sources in your hierarchy.");
    }

    return new Assignment(inventory, this, assignable.get());
  }

  @Override
  public String toString() {
    return "Variable{" +
        "name='" + name + '\'' +
        ", assignables=" + assignables +
        ", inventory=" + inventory +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Variable variable = (Variable) o;
    return Objects.equals(name, variable.name) &&
        Objects.equals(assignables, variable.assignables) &&
        Objects.equals(inventory, variable.inventory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, assignables, inventory);
  }
}

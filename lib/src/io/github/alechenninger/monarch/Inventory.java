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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Inventory {
  private final Map<String, List<Assignable>> map;

  public static Inventory from(Map<String, List<Assignable>> potentials) {
    return new Inventory(potentials);
  }

  public static Inventory empty() {
    return new Inventory(Collections.emptyMap());
  }

  private Inventory(Map<String, List<Assignable>> map) {
    // TODO: Validate same value doesn't appear twice in potentials
    // TODO: Validate there are no conflicting implied values
    // e.g. foo=bar implies foo=baz (either directly or transitively)
    this.map = new HashMap<>(map);
  }

  public Assignment assign(String variable, String value) {
    if (!map.containsKey(variable)) {
      throw new IllegalArgumentException("Variable not found in inventory: " + variable);
    }

    List<Assignable> assignables = map.get(variable);

    if (assignables == null) {
      throw new IllegalStateException("No assignable values defined for variable: " + variable);
    }

    Optional<Assignable> assignable = assignables.stream()
        .filter(p -> p.value().equals(value))
        .findFirst();

    if (!assignable.isPresent()) {
      throw new IllegalArgumentException("Cannot assign value <" + value + "> to variable <" +
          variable + "> because value is not in inventory for variable. Check your assignment " +
          "or add value to inventory. An inventory needs to be comprehensive it can be used " +
          "to discover all of the sources in your hierarchy.");
    }

    return new Assignment(this, new Variable(variable, assignables, this), assignable.get());
  }

  public Assignments assignAll(Iterable<Assignment> assignments) {
    return new Assignments(this, assignments);
  }

  public Assignments assignAll(Map<String, String> variablesToValues) {
    Set<Assignment> assignments = variablesToValues.entrySet().stream()
        .map(entry -> assign(entry.getKey(), entry.getValue()))
        .collect(Collectors.toSet());
    return new Assignments(this, assignments);
  }

  public boolean hasVariable(Variable variable) {
    return variableByName(variable.name())
        .map(variable::equals)
        .orElse(false);
  }

  public Optional<Variable> variableByName(String name) {
    return Optional.ofNullable(map.get(name))
        .map(assignables -> new Variable(name, assignables, this));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Inventory inventory = (Inventory) o;
    return Objects.equals(map, inventory.map);
  }

  @Override
  public int hashCode() {
    return Objects.hash(map);
  }

  @Override
  public String toString() {
    return "Inventory{" + map + '}';
  }
}

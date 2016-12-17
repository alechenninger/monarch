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
import java.util.Set;
import java.util.stream.Collectors;

public class Inventory {
  private final Map<String, List<Potential>> map;

  public static Inventory from(Map<String, List<Potential>> potentials) {
    return new Inventory(potentials);
  }

  public static Inventory empty() {
    return new Inventory(Collections.emptyMap());
  }

  private Inventory(Map<String, List<Potential>> map) {
    // TODO: Validate same value doesn't appear twice in potentials
    this.map = map;
  }

  public Assignments newAssignments() {
    return new Assignments(this);
  }

  public Assignment assign(String variable, String value) {
    if (!map.containsKey(variable)) {
      throw new IllegalArgumentException("Variable not found in inventory: " + variable);
    }

    List<Potential> potentials = map.get(variable);

    if (potentials == null) {
      throw new IllegalStateException("No potential values defined for variable: " + variable);
    }

    Optional<Potential> potential = potentials.stream()
        .filter(p -> p.value().equals(value))
        .findFirst();

    if (!potential.isPresent()) {
      throw new IllegalArgumentException("Cannot assign value to variable: value is not in " +
          "inventory. variable=" + variable + " value=" + value);
    }

    return new Assignment(this, variable, potential.get());
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
        .map(potentials -> new Variable(name, potentials, this));
  }

  public boolean isAssignable(String variable, String value) {
    throw new UnsupportedOperationException();
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

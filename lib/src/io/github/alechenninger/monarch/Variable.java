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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Variable {
  private final String name;
  private final List<Assignable> assignables;
  private final Inventory inventory;

  public Variable(String name, List<Assignable> assignables, Inventory inventory) {
    this.name = name;
    this.assignables = assignables;
    this.inventory = inventory;
  }

  public String name() {
    return name;
  }

  public Set<String> values(Assignments assignments) {
    if (assignments.isAssigned(name)) {
      return Collections.singleton(assignments.forVariable(name).value());
    }

    Set<String> values = new HashSet<>(assignables.size());

    for (Assignable assignable : assignables) {
      Assignment assignment = assign(assignable.value());
      if (assignment.conflictsWith(assignments)) continue;
      values.add(assignment.value());
    }

    return values;
  }

  public Assignment assign(String value) {
    return inventory.assign(name, value);
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

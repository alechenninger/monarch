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

import java.util.Objects;

public class Assignment {
  private final Inventory inventory;
  private final String variable;
  private final Potential potential;

  public Assignment(Inventory inventory, String variable, Potential potential) {
    this.inventory = inventory;
    this.variable = variable;
    this.potential = potential;
  }

  public Variable variable() {
    return inventory.variableByName(variable).get();
  }

  public String value() {
    return potential.value();
  }

  public boolean conflictsWith(Assignments assignments) {
    return assignments.conflictsWith(this);
  }

  // TODO: Think about explicit / implicit distinction in Assignments some more
  public Assignments implied() {
    return inventory.assignAll(potential.impliedAssignments());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Assignment that = (Assignment) o;
    return Objects.equals(inventory, that.inventory) &&
        Objects.equals(variable, that.variable) &&
        Objects.equals(potential, that.potential);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inventory, variable, potential);
  }

  @Override
  public String toString() {
    return "Assignment{" +
        "variable='" + variable + '\'' +
        ", potential=" + potential +
        '}';
  }
}

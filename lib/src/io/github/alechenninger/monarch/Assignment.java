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
  private final Variable variable;
  private final Assignable assignable;

  Assignment(Inventory inventory, Variable variable, Assignable assignable) {
    this.inventory = inventory;
    this.variable = variable;
    this.assignable = assignable;
  }

  public Variable variable() {
    return variable;
  }

  public String value() {
    return assignable.value();
  }

  public boolean conflictsWith(Assignments assignments) {
    return assignments.conflictsWith(this);
  }

  public Assignments implied() {
    // TODO: cache this
    return inventory.assignAll(assignable.impliedAssignments());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Assignment that = (Assignment) o;
    return Objects.equals(inventory, that.inventory) &&
        Objects.equals(variable, that.variable) &&
        Objects.equals(assignable, that.assignable);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inventory, variable, assignable);
  }

  @Override
  public String toString() {
    return "Assignment{" +
        "variable='" + variable.name() + '\'' +
        ", assignable=" + assignable +
        '}';
  }
}

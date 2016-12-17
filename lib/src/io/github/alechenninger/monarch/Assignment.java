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

public class Assignment {
  private final Inventory inventory;
  private final String variable;
  private final String value;

  public Assignment(Inventory inventory, String variable, String value) {
    this.inventory = inventory;
    this.variable = variable;
    this.value = value;
    if (!inventory.isAssignable(variable, value)) {
      throw new IllegalArgumentException();
    }
  }

  public Variable variable() {
    return inventory.variableByName(variable).get();
  }

  public String value() {
    return value;
  }

  public boolean implies(Assignment assignment) {
    throw new UnsupportedOperationException();
  }

  public boolean conflictsWith(Assignments assignments) {
    return assignments.conflictsWith(this);
  }

  public Assignments implied() {
    throw new UnsupportedOperationException();
  }
}

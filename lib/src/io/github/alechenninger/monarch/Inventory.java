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

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Inventory {
  private Map<String, List<Potential>> map;

  public Assignments newAssignments() {
    return new Assignments(this);
  }

  public Assignment assign(String variable, String value) {
    return new Assignment(this, variable, value);
  }

  public boolean hasVariable(Variable variable) {
    throw new UnsupportedOperationException();
  }

  public Optional<Variable> variableByName(String name) {
    throw new UnsupportedOperationException();
  }

  public static Inventory from(Map<String, List<Potential>> potentials) {
    return null;
  }

  public boolean isAssignable(String variable, String value) {
    throw new UnsupportedOperationException();
  }
}

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

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class Assignments {
  private final Map<String, Assignment> assignmentsByVariableName;

  // TODO: ctor with collection of assignments
  public Assignments(Map<String, Assignment> assignmentsByVariableName) {
    this.assignmentsByVariableName = new HashMap<>(assignmentsByVariableName);
  }

  public Assignment forVariableNamed(String name) {
    if (!assignmentsByVariableName.containsKey(name)) {
      throw new NoSuchElementException(name);
    }

    return assignmentsByVariableName.get(name);
  }

  public Assignment forVariable(Variable variable) {
    return forVariableNamed(variable.name());
  }
}

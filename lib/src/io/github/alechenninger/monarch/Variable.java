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
import java.util.Set;

public class Variable {
  private String name;
  private Set<String> potentials;

  public String name() {
    return name;
  }

  public Set<String> values(Assignments assignments) {
    if (assignments.isAssigned(name)) {
      return Collections.singleton(assignments.forVariable(name).value());
    }

    Set<String> values = new HashSet<>();

    for (String potential : potentials) {
      Assignment assignment = assign(potential);
      if (assignment.conflictsWith(assignments)) continue;
      values.add(assignment.value());
    }

    return values;
  }

  public Set<String> valuesThatImply(Assignment assignment) {
    throw new UnsupportedOperationException();
  }

  public Assignment assign(String value) {
    throw new UnsupportedOperationException();
  }
}

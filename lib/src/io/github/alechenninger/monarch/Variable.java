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
import java.util.Set;

public final class Variable {
  private final String name;
  private final Set<Assignment> potentials;

  public Variable(String name, Set<Assignment> potentials) {
    this.name = name;
    this.potentials = potentials;
  }

  public String name() {
    return name;
  }

  public Set<Assignment> potentials() {
    return potentials;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Variable variable = (Variable) o;
    return Objects.equals(name, variable.name) &&
        Objects.equals(potentials, variable.potentials);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, potentials);
  }

  @Override
  public String toString() {
    return "Variable{" +
        "name='" + name + '\'' +
        ", potentials=" + potentials +
        '}';
  }
}

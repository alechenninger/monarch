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
import java.util.Map;
import java.util.Objects;

public final class Assignment {
  private final Variable variable;
  private final String value;
  private final Map<Variable, Assignment> implications;

  // TODO: better factory
  public Assignment(Variable variable, String value, Map<Variable, Assignment> implications) {
    this.variable = variable;
    this.value = value;
    this.implications = Collections.unmodifiableMap(new HashMap<>(implications));
  }

  public Variable variable() {
    return variable;
  }

  public String value() {
    return value;
  }

  public Map<Variable, Assignment> implications() {
    return implications;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Assignment that = (Assignment) o;
    return Objects.equals(variable, that.variable) &&
        Objects.equals(value, that.value) &&
        Objects.equals(implications, that.implications);
  }

  @Override
  public int hashCode() {
    return Objects.hash(variable, value, implications);
  }

  @Override
  public String toString() {
    return "Assignment{" +
        "variable=" + variable +
        ", value='" + value + '\'' +
        ", implications=" + implications +
        '}';
  }
}

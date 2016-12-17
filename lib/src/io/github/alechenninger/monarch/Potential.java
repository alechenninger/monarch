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

public class Potential {
  private final String value;
  private final Map<String, String> impliedAssignments;

  public Potential(String value) {
    this.value = value;
    this.impliedAssignments = Collections.emptyMap();
  }

  public Potential(String value, Map<String, String> impliedAssignments) {
    this.value = value;
    this.impliedAssignments = Collections.unmodifiableMap(new HashMap<>(impliedAssignments));
  }

  public static Potential of(String value) {
    return new Potential(value);
  }

  public Map<String, String> impliedAssignments() {
    return impliedAssignments;
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Potential potential = (Potential) o;
    return Objects.equals(value, potential.value) &&
        Objects.equals(impliedAssignments, potential.impliedAssignments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, impliedAssignments);
  }

  @Override
  public String toString() {
    return "Potential{" +
        "value='" + value + '\'' +
        ", impliedAssignments=" + impliedAssignments +
        '}';
  }
}

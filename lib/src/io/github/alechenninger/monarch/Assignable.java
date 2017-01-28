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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Assignable {
  private final String value;
  private final Map<String, String> impliedAssignments;

  @SuppressWarnings("unchecked")
  public static List<Assignable> fromStringMapOrList(Object assignables) {
    if (assignables instanceof String) {
      return Assignable.expandToStream((String) assignables, null).collect(Collectors.toList());
    }

    if (assignables instanceof List) {
      return fromList((List<Object>) assignables);
    }

    if (assignables instanceof Map) {
      return fromMap((Map<String, Object>) assignables);
    }

    throw new IllegalArgumentException("Expected assignable(s) in inventory to be a string, " +
        "a list, or a map, but was: " + assignables);
  }

  @SuppressWarnings("unchecked")
  private static List<Assignable> fromMap(Map<String, Object> assignables) {
    return assignables.entrySet().stream()
        .flatMap(e -> Assignable.expandToStream(e.getKey(), (Map<String, String>) e.getValue()))
        .collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  private static List<Assignable> fromList(List<Object> assignables) {
    return assignables.stream()
        .flatMap(assignable -> {
          if (assignable instanceof String) {
            return Assignable.expandToStream((String) assignable, null);
          }

          if (assignable instanceof Map) {
            Map<String, Object> valuesToImplications = (Map) assignable;

            if (valuesToImplications.size() > 1) {
              throw new IllegalArgumentException("Expected 1 value for assignment " +
                  "with implied values. You probably need to correct your YAML " +
                  "indentation by further indenting the implications. Values found were: " +
                  valuesToImplications.keySet());
            }

            Map.Entry<String, Object> valueToImplications =
                valuesToImplications.entrySet().iterator().next();

            String value = valueToImplications.getKey();
            Object implications = valueToImplications.getValue();

            if (implications instanceof Map) {
              return Assignable.expandToStream(value, (Map<String, String>) implications);
            }

            if (implications == null) {
              return Assignable.expandToStream(value, null);
            }

            throw new IllegalArgumentException("Expected implications to be a map");
          }

          throw new IllegalArgumentException("Expected value to be either a string or a map of " +
              "the value to it's implied assignments, but was: " + assignable);
        })
        .collect(Collectors.toList());
  }

  private Assignable(String value) {
    this.value = Objects.requireNonNull(value, "value");
    this.impliedAssignments = Collections.emptyMap();
  }

  private Assignable(String value, Map<String, String> impliedAssignments) {
    this.value = Objects.requireNonNull(value, "value");
    this.impliedAssignments = Collections.unmodifiableMap(impliedAssignments);
  }

  public static Assignable of(String value) {
    return new Assignable(value);
  }

  public static Assignable of(String value, Map<String, String> impliedAssignments) {
    return new Assignable(value, new HashMap<>(impliedAssignments));
  }

  public static Stream<Assignable> expandToStream(String value, Map<String, String> impliedAssignments) {
    if (impliedAssignments == null) {
      return BraceExpand.string(value).stream().map(Assignable::new);
    }

    return BraceExpand.string(value).stream()
        .map(v -> new Assignable(v, BraceExpand.keysOf(impliedAssignments)));
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
    Assignable assignable = (Assignable) o;
    return Objects.equals(value, assignable.value) &&
        Objects.equals(impliedAssignments, assignable.impliedAssignments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, impliedAssignments);
  }

  @Override
  public String toString() {
    return "Assignable{" +
        "value='" + value + '\'' +
        ", impliedAssignments=" + impliedAssignments +
        '}';
  }
}

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
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Inventory {
  private final Map<String, List<Assignable>> map;
  private final Map<String, Variable> cachedVariables;

  private final int hash;

  @SuppressWarnings("unchecked")
  public static Inventory parse(Object data) {
    if (data == null) {
      return Inventory.empty();
    }

    if (!(data instanceof Map)) {
      throw new IllegalArgumentException("Expected map but got " + data);
    }

    Map<String, Object> inventory = BraceExpand.keysOf((Map<String, Object>) data);

    //noinspection Convert2MethodRef
    return new Inventory(inventory.entrySet()
        .stream()
        .collect(Collectors.toMap(
            entry -> entry.getKey(),
            entry -> Assignable.fromStringMapOrList(entry.getValue()))));
  }

  public static Inventory from(Map<String, List<Assignable>> map) {
    return new Inventory(new HashMap<>(map));
  }

  public static Inventory empty() {
    return new Inventory(Collections.emptyMap());
  }

  private Inventory(Map<String, List<Assignable>> map) {
    // TODO: Validate there are no conflicting implied values? This is done lazily currently.
    // e.g. foo=bar implies foo=baz (either directly or transitively)
    //noinspection Convert2MethodRef
    this.map = map;

    cachedVariables = new HashMap<>(map.size());
    hash = map.hashCode();
  }

  public Assignment assign(String variable, String value) {
    return variableByName(variable)
        .orElseThrow(() -> new NoSuchElementException("Variable not found in inventory: " + variable))
        .assign(value);
  }

  public Assignments assignAll(Iterable<Assignment> assignments) {
    return new Assignments(this, assignments);
  }

  public Assignments assignAll(Map<String, String> variablesToValues) {
    Set<Assignment> assignments = variablesToValues.entrySet().stream()
        .map(entry -> assign(entry.getKey(), entry.getValue()))
        .collect(Collectors.toSet());
    return new Assignments(this, assignments);
  }

  public boolean hasVariable(Variable variable) {
    return variableByName(variable.name())
        .map(variable::equals)
        .orElse(false);
  }

  public Optional<Variable> variableByName(String name) {
    if (!cachedVariables.containsKey(name)) {
      List<Assignable> assignables = map.get(name);
      if (assignables == null) {
        cachedVariables.put(name, null);
      } else {
        cachedVariables.put(name, new Variable(name, assignables, this));
      }
    }

    return Optional.ofNullable(cachedVariables.get(name));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Inventory inventory = (Inventory) o;
    return Objects.equals(map, inventory.map);
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    return "Inventory{" + map.values() + '}';
  }
}

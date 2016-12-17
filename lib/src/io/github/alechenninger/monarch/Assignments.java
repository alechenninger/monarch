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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class Assignments implements Iterable<Assignment> {
  private final Set<Assignment> set = new HashSet<>();
  private final Inventory inventory;

  public Assignments(Inventory inventory) {
    this.inventory = Objects.requireNonNull(inventory, "inventory");
  }

  public Assignments(Inventory inventory, Assignment assignment) {
    this.inventory = Objects.requireNonNull(inventory, "inventory");
    add(assignment);
  }

  public Assignments(Inventory inventory, Iterable<Assignment> assignments) {
    this.inventory = Objects.requireNonNull(inventory, "inventory");
    assignments.forEach(this::add);
  }

  // TODO: Should this use empty inventory?
  public static Assignments none() { return new Assignments(new Inventory()); }

  public Assignments with(Assignments assignments) {
    if (!assignments.inventory.equals(inventory)) {
      throw new IllegalArgumentException("Assignments for different inventories cannot be combined.");
    }

    Assignments combination = new Assignments(inventory);
    combination.addAll(this);
    combination.addAll(assignments);
    return combination;
  }

  public Assignments with(Assignment assignment) {
    return with(new Assignments(inventory, assignment));
  }

  public Assignments with(String variable, String value) {
    Assignment assignment = inventory.variableByName(variable)
        .orElseThrow(NoSuchElementException::new)
        .assign(value);

    return with(assignment);
  }

  public Assignments fork(String variable, String value) {
    Assignments fork = new Assignments(inventory);
    for (Assignment assignment : this) {
      if (assignment.variable().name().equals(value)) {
        continue;
      }

      fork.add(assignment);
    }

    return fork.with(variable, value);
  }

  public boolean isAssigned(String variable) {
    return set.stream().anyMatch(a -> a.variable().name().equals(variable));
  }

  public Assignment forVariable(String variable) {
    return set.stream()
        .filter(a -> a.variable().name().equals(variable))
        .findFirst()
        .orElseThrow(NoSuchElementException::new);
  }

  public Set<String> possibleValues(String variable) {
    if (isAssigned(variable)) {
      return Collections.singleton(forVariable(variable).value());
    }

    return inventory.variableByName(variable).get().values(this);
  }

  public boolean containsAll(Assignments assignments) {
    return assignments.set.containsAll(this.set);
  }

  public boolean conflictsWith(Assignment assignment) {
    throw new UnsupportedOperationException();
  }

  public boolean conflictsWith(String variable, String value) {
    return false;
  }

  public boolean assignsOnly(List<String> variables) {
    Assignments assignments = new Assignments(inventory);
    for (String variable : variables) {
      if (!isAssigned(variable)) {
        return false;
      }
      assignments.add(forVariable(variable));
    }
    return equals(assignments);
  }

  public boolean assignsSupersetOf(List<String> variables) {
    Assignments assignments = new Assignments(inventory);
    for (String variable : variables) {
      if (!isAssigned(variable)) {
        return false;
      }
      assignments.add(forVariable(variable));
    }
    return containsAll(assignments);
  }

  public boolean assignsSubsetOf(List<String> variables) {
    Assignments assignments = new Assignments(inventory);
    for (String variable : variables) {
      if (!isAssigned(variable)) {
        return false;
      }
      assignments.add(forVariable(variable));
    }
    return assignments.containsAll(this);
  }

  public boolean isEmpty() {
    return set.isEmpty();
  }

  public Stream<Assignment> stream() {
    return set.stream();
  }

  @Override
  public Iterator<Assignment> iterator() {
    return set.iterator();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Assignments that = (Assignments) o;
    return Objects.equals(set, that.set);
  }

  @Override
  public int hashCode() {
    return Objects.hash(set);
  }

  private void add(Assignment assignment) {
    Variable variable = assignment.variable();

    if (!inventory.hasVariable(variable)) {
      throw new IllegalArgumentException("Assignment must be within same inventory. Expected " +
          "assignment's variable to be configured the same as in known inventory. " +
          "Inventory's copy: " + inventory.variableByName(variable.name()) + " " +
          "Got: " + variable);
    }

    if (isAssigned(variable.name())) {
      if (forVariable(variable.name()).equals(assignment)) {
        return;
      }

      // TODO: Improve exception
      throw new IllegalStateException("Conflicting assignment: " + assignment);
    }


    if (conflictsWith(assignment)) {
      // TODO: improve exception
      throw new IllegalArgumentException("Conflicting assignment: " + assignment);
    }

    set.add(assignment);
  }

  private void addAll(Assignments assignments) {
    assignments.forEach(this::add);
  }
}

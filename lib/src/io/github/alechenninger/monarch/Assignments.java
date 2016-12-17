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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Assignments implements Iterable<Assignment> {
  private final Set<Assignment> explicit = new HashSet<>();
  private final Set<Assignment> implicit = new HashSet<>();
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
  public static Assignments none(Inventory inventory) { return new Assignments(inventory); }

  public Assignments with(Assignments assignments) {
    if (!assignments.inventory.equals(inventory)) {
      throw new IllegalArgumentException("Assignments for different inventories cannot be combined.");
    }

    Assignments combination = new Assignments(inventory);
    combination.addAll(this);
    combination.addAll(assignments);
    return combination;
  }

  public Assignments with(Iterable<Assignment> assignments) {
    Assignments arg = inventory.newAssignments();
    for (Assignment assignment : assignments) {
      arg.add(assignment);
    }
    return with(arg);
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

  public boolean canFork(String variable, String value) {
    // Check if variable is implied, if so cant fork without forking at implier first
    // Also check that this assignment wouldn't conflict with other assignments
    Assignment assignment = inventory.assign(variable, value);
    if (implicit.stream().map(i -> i.variable().name()).anyMatch(variable::equals)) return false;
    Assignments fork = forkAt(variable);
    return !fork.conflictsWith(assignment);
  }

  public Assignments forkAt(String variable) {
    Assignments fork = new Assignments(inventory);
    for (Assignment assignment : explicit) {
      if (assignment.variable().name().equals(variable)) {
        continue;
      }

      fork.add(assignment);
    }
    return fork;
  }

  public Assignments fork(String variable, String value) {
    Assignments fork = new Assignments(inventory);
    for (Assignment assignment : explicit) {
      if (assignment.variable().name().equals(variable)) {
        continue;
      }

      fork.add(assignment);
    }

    return fork.with(variable, value);
  }

  public boolean isAssigned(String variable) {
    return stream().anyMatch(a -> a.variable().name().equals(variable));
  }

  public Assignment forVariable(String variable) {
    return stream().filter(a -> a.variable().name().equals(variable))
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
    return stream().collect(Collectors.toSet())
        .containsAll(assignments.stream().collect(Collectors.toSet()));
  }

  // TODO: Test this a lot
  public boolean conflictsWith(Assignment assignment) {
    Variable variable = assignment.variable();
    if (isAssigned(variable.name()) && !forVariable(variable.name()).equals(assignment)) {
      return true;
    }

    for (Assignment implied : assignment.implied()) {
      if (conflictsWith(implied)) {
        return true;
      }
    }

    return false;
  }

  public boolean conflictsWith(String variable, String value) {
    return conflictsWith(inventory.assign(variable, value));
  }

  /**
   * Returns true if only the provided variables are assigned and no others.
   */
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

  /**
   * Returns true if at least the provided variables are all assigned.
   */
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

  /**
   * Returns true if variables contains a superset of all of the assigned variables.
   * @param variables
   * @return
   */
  public boolean assignsSubsetOf(List<String> variables) {
    Assignments assignments = new Assignments(inventory);
    for (String variable : variables) {
      assignments.add(forVariable(variable));
    }
    return assignments.containsAll(this);
  }

  public boolean isEmpty() {
    return !iterator().hasNext();
  }

  public Map<String, String> toMap() {
    return stream().collect(Collectors.toMap(a -> a.variable().name(), Assignment::value));
  }

  public Stream<Assignment> stream() {
    return Stream.concat(explicit.stream(), implicit.stream());
  }

  @Override
  public Iterator<Assignment> iterator() {
    return new Iterator<Assignment>() {
      Iterator<Assignment> explicit = Assignments.this.explicit.iterator();
      Iterator<Assignment> implicit = Assignments.this.implicit.iterator();

      @Override
      public boolean hasNext() {
        return explicit.hasNext() || implicit.hasNext();
      }

      @Override
      public Assignment next() {
        if (explicit.hasNext()) {
          return explicit.next();
        }

        if (implicit.hasNext()) {
          return implicit.next();
        }

        throw new NoSuchElementException();
      }
    };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Assignments that = (Assignments) o;
    return Objects.equals(explicit, that.explicit) &&
        Objects.equals(implicit, that.implicit) &&
        Objects.equals(inventory, that.inventory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(explicit, implicit, inventory);
  }

  @Override
  public String toString() {
    return "Assignments{" +
        "explicit=" + explicit +
        ", implicit=" + implicit +
        '}';
  }

  private void add(Assignment assignment) {
    Variable variable = assignment.variable();

    if (!inventory.hasVariable(variable)) {
      throw new IllegalArgumentException("Assignment must be within same inventory. Expected " +
          "assignment's variable to be configured the same as in known inventory. " +
          "Inventory's copy: " + inventory.variableByName(variable.name()) + " " +
          "Got: " + variable);
    }

    if (conflictsWith(assignment)) {
      // TODO: improve exception
      throw new IllegalArgumentException("Conflicting assignment: " + assignment);
    }

    if (implicit.contains(assignment)) {
      removeImplicit(assignment);
    }

    if (explicit.contains(assignment)) {
      return;
    }

    explicit.add(assignment);
    assignment.implied().forEach(this::addImplicit);
  }

  private void addImplicit(Assignment assignment) {
    if (conflictsWith(assignment)) {
      // TODO: improve exception
      throw new IllegalArgumentException("Conflicting implicit assignment: " + assignment);
    }

    if (isAssigned(assignment.variable().name())) {
      return;
    }

    this.implicit.add(assignment);
    assignment.implied().forEach(this::addImplicit);
  }

  private void removeImplicit(Assignment assignment) {
    if (implicit.contains(assignment)) {
      assignment.implied().forEach(this::removeImplicit);
      implicit.remove(assignment);
    }
  }

  private void addAll(Assignments assignments) {
    assignments.explicit.forEach(this::add);
  }
}

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Assignments implements Iterable<Assignment> {
  private final Set<Assignment> explicit = new LinkedHashSet<>();
  private final Set<Assignment> implicit = new LinkedHashSet<>();
  private final Inventory inventory;

  Assignments(Inventory inventory) {
    this.inventory = Objects.requireNonNull(inventory, "inventory");
  }

  Assignments(Inventory inventory, Assignment assignment) {
    this.inventory = Objects.requireNonNull(inventory, "inventory");
    add(assignment);
  }

  Assignments(Inventory inventory, Iterable<Assignment> assignments) {
    this.inventory = Objects.requireNonNull(inventory, "inventory");
    assignments.forEach(this::add);
  }

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
    return with(new Assignments(inventory, assignments));
  }

  public Assignments with(Assignment assignment) {
    return with(new Assignments(inventory, assignment));
  }

  public Assignments with(String variable, String value) {
    return with(inventory.assign(variable, value));
  }

  public boolean canForkAt(String variable) {
    // Implicit may not contain all actually implicit assignments if that assignment is also
    // explicit. So checking the implicits of our explicits makes us sure.
    for (Assignment assignment : explicit) {
      if (assignment.variable().name().equals(variable)) {
        continue;
      }

      if (assignment.implied().isAssigned(variable)) {
        return false;
      }
    }
    return true;
  }

  public Assignments forkAt(String variable) {
    Assignments fork = new Assignments(inventory);
    for (Assignment assignment : explicit) {
      if (assignment.variable().name().equals(variable)) {
        continue;
      }

      if (assignment.implied().isAssigned(variable)) {
        throw new IllegalArgumentException("Can't fork at variable which is implied by another " +
            "variable's assignment. Tried to fork at: " + variable + " but conflicts with: " +
            assignment);
      }

      fork.add(assignment);
    }

    return fork;
  }

  public boolean isAssigned(String variable) {
    return stream().anyMatch(a -> a.variable().name().equals(variable));
  }

  public Assignment forVariable(String variable) {
    return stream().filter(a -> a.variable().name().equals(variable))
        .findAny()
        .orElseThrow(NoSuchElementException::new);
  }

  public Set<String> possibleValues(String variable) {
    if (isAssigned(variable)) {
      return Collections.singleton(forVariable(variable).value());
    }

    return inventory.variableByName(variable)
        .map(v -> v.values(this))
        .orElse(Collections.emptySet());
  }

  /**
   * Computes the entire <em>possible</em> set of assignments where all of the provided variables
   * can be assigned, where each set starts from our existing assignments.
   *
   * <p>"Possible" is the operative word: we'll try to use each value of unassigned variables such
   * that those values and their corresponding implications aren't ruled out by other assignments,
   * implied or otherwise.
   */
  public Set<Assignments> possibleAssignments(Collection<String> variables) {
    Set<Assignments> possibilities = Collections.singleton(this);

    for (String variable : variables) {
      if (isAssigned(variable)) continue;

      // Only assignments which have a value that can satisfy the missing variable will survive to
      // potentially become a viable set of assignments for all missing variables.
      Set<Assignments> survivors = new LinkedHashSet<>();
      for (Assignments possibility : possibilities) {
        for (String value : possibility.possibleValues(variable)) {
          if (possibility.isAssigned(variable)) {
            // Either way, the current possibility will remain because it assigns this variable
            survivors.add(possibility);

            // If the new value is different and we can, we'll add a fork of this possibility with
            // the alternate value.
            if (possibility.conflictsWith(variable, value) &&
                possibility.canForkAt(variable)) {
              Assignments fork = possibility.forkAt(variable);
              if (!fork.conflictsWith(variable, value)) {
                survivors.add(fork.with(variable, value));
              }
            }
          } else if (possibility.conflictsWith(variable, value)) {
            // This only happens if the missing var is not assigned, but assigning it this possible
            // value would imply a conflict. I think this may never happen since we ask this
            // possibility for potential values already, and therefore it won't give us values we
            // couldn't assign.
            // TODO: consider removing this branch
            survivors.add(possibility);
          } else {
            survivors.add(possibility.with(variable, value));
          }
        }
      }
      possibilities = survivors;
    }

    return possibilities;
  }

  public boolean contains(Assignment assignment) {
    return stream().collect(Collectors.toSet()).contains(assignment);
  }

  public boolean containsAll(Assignments assignments) {
    return stream().collect(Collectors.toSet())
        .containsAll(assignments.stream().collect(Collectors.toSet()));
  }

  public boolean conflictsWith(Assignment assignment) {
    return conflictOf(assignment).isPresent();
  }

  public boolean conflictsWith(String variable, String value) {
    return conflictsWith(inventory.assign(variable, value));
  }

  // TODO: I think this should actually return a List
  public Optional<Assignment> conflictOf(Assignment assignment) {
    Variable variable = assignment.variable();

    if (isAssigned(variable.name()) && !forVariable(variable.name()).equals(assignment)) {
      return Optional.of(forVariable(variable.name()));
    }

    for (Assignment implied : assignment.implied()) {
      Optional<Assignment> conflict = conflictOf(implied);
      if (conflict.isPresent()) {
        return conflict;
      }
    }

    return Optional.empty();
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
    return explicit.isEmpty() && implicit.isEmpty();
  }

  public int size() {
    return explicit.size() + implicit.size();
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

    Optional<Assignment> conflict = conflictOf(assignment);
    if (conflict.isPresent()) {
      throw new IllegalArgumentException("Assignment " + assignment + " conflicts with " +
          conflict.get());
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
    Optional<Assignment> conflict = conflictOf(assignment);
    if (conflict.isPresent()) {
      throw new IllegalArgumentException("Implicit assignment " + assignment + " conflicts with " +
          conflict.get());
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

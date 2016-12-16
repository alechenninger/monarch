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

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class Assignments implements Iterable<Assignment> {
  private final Set<Assignment> set = new HashSet<>();
  private final Inventory inventory;

  // TODO: Think on being per-inventory a bit...
  public Assignments(Inventory inventory) {
    this.inventory = Objects.requireNonNull(inventory, "inventory");
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

  public Inventory inventory() {
    return inventory;
  }

  public void add(Assignment assignment) {
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

    if (set.add(assignment)) {
      addAll(assignment.implied());
    }
  }

  public void addAll(Assignments assignments) {
    assignments.forEach(this::add);
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

  public boolean containsAll(Assignments assignments) {
    return assignments.set.containsAll(this.set);
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
}

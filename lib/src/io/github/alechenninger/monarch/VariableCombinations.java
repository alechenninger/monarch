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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class VariableCombinations {
  /**
   * Computes each possible set of variables given the predefined {@code variables} and potential
   * values for keys which are in {@code variableNames} but not defined in {@code variables}.
   *
   * <p>We expect {@code variables} to already include all additional values.
   * TODO: make whether or not variables includes implied more explicit
   * Variables is a function of implications and user input.
   */
  static Stream<Assignments> stream(List<String> variableNames, Assignments assignments) {
    List<String> missingVars = variableNames.stream()
        .filter(var -> !assignments.isAssigned(var))
        .collect(Collectors.toList());

    VariableCombinations combos = new VariableCombinations(assignments);

    for (String missingVar : missingVars) {
      Set<String> potentialsForVar = assignments.possibleValues(missingVar);

      if (potentialsForVar.isEmpty()) {
        throw new IllegalStateException("No potentials found for missing variable '" +
            missingVar + "'.");
      }

      for (String potential : potentialsForVar) {
        combos.put(missingVar, potential);
      }
    }

    return combos.stream();
  }

  private List<Assignments> combos = new ArrayList<>();

  private VariableCombinations(Assignments variablesProvided) {
    // TODO: What if there are no combinations actually? We are filtering empty in .stream(), but
    // might be better way.
    combos.add(variablesProvided);
  }

  private void put(String variable, String value) {
    List<Assignments> newCombos = new ArrayList<>();
    for (Assignments combo : combos) {
      if (combo.isAssigned(variable)) {
        if (combo.conflictsWith(variable, value) && combo.canFork(variable, value)) {
          Assignments newCombo = combo.fork(variable, value);
          newCombos.add(combo);
          newCombos.add(newCombo);
        } else {
          newCombos.add(combo);
        }
      } else {
        if (combo.conflictsWith(variable, value)) {
          newCombos.add(combo);
        } else {
          newCombos.add(combo.with(variable, value));
        }
      }
    }
    combos = newCombos;
  }

  private Stream<Assignments> stream() {
    return combos.stream().filter(c -> !c.isEmpty());
  }

  @Override
  public String toString() {
    return "VariableCombinations{" +
        "combos=" + combos +
        '}';
  }
}

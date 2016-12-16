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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
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
  static Stream<Map<String, String>> stream(List<String> variableNames,
      Assignments assignments, Inventory inventory) {
    Set<Assignment> usedAssignments = assignments.stream()
        .filter(a -> variableNames.contains(a.variable().name()))
        .collect(Collectors.toSet());

    List<String> missingVars = variableNames.stream()
        .filter(var -> !usedAssignments.contains(assignments.forVariable(var)))
        .collect(Collectors.toList());

    VariableCombinations combos = new VariableCombinations(usedAssignments, inventory);

    for (String missingVar : missingVars) {
      Set<String> potentialsForVar = inventory.variableByName(missingVar)
          .orElseThrow(NoSuchElementException::new)
          .values();

      if (potentialsForVar == null || potentialsForVar.isEmpty()) {
        throw new IllegalStateException("No potentials found for missing variable '" +
            missingVar + "'.");
      }

      for (String potential : potentialsForVar) {
        // A potential which implies variables that conflict with know variables is not a potential
        // in this context.
        // TODO: Maybe generalize this idea since it likely is useful in more places
        // Once we have variables, we can refine potentials based on implications
        // Ex: if environment=prod, a value that implies environment=qa is not a potential value.
        // Note though we have to continue to do this within each combination.
        if (mapsConflict(potential.getImpliedVariables(), assignments)) {
          continue;
        }

        combos.put(missingVar, potential.getValue());
      }
    }

    return combos.stream();
  }

  private static boolean mapsConflict(Map<String, String> map1, Map<String, String> map2) {
    for (Map.Entry<String, String> map1Entry : map1.entrySet()) {
      if (map2.containsKey(map1Entry.getKey()) &&
          !Objects.equals(map1Entry.getValue(), map2.get(map1Entry.getKey()))) {
        return true;
      }
    }

    return false;
  }

  private final List<Map<String, String>> combos = new ArrayList<>();
  private final Map<String, List<Potential>> potentials;

  private VariableCombinations(Set<Assignment> variablesProvided,
      Inventory potentials) {
    this.potentials = potentials;

    // TODO: What if there are no combinations actually? We are filtering empty in .stream(), but
    // might be better way.
    combos.add(new HashMap<>(variablesProvided));
  }

  private void put(String variable, String value) {
    List<Map<String, String>> newCombos = new ArrayList<>();
    combos: for (Map<String, String> combo : combos) {
      // See if this variable would imply other variables. If so, they get added to this combo
      // automatically.
      if (potentials.containsKey(variable)) {
        // Find the potential for this value
        for (Potential potential : potentials.get(variable)) {
          if (!potential.getValue().equals(value)) {
            continue;
          }

          Map<String, String> impliedAdditions = new HashMap<>();

          for (Map.Entry<String, String> implied : potential.getImpliedVariables().entrySet()) {
            String impliedKey = implied.getKey();
            String impliedValue = implied.getValue();
            if (combo.containsKey(impliedKey)) {
              String currentValue = combo.get(impliedKey);
              if (!Objects.equals(currentValue, impliedValue)) {
                continue combos;
              }
            } else {
              impliedAdditions.put(impliedKey, impliedValue);
            }
          }

          // FIXME: This is wrong because implied values will be carried through to new combinations
          // even though those variables may not imply the same things, see below
          combo.putAll(impliedAdditions);
        }
      }

      if (combo.containsKey(variable)) {
        // Is it already implied in this combo?
        for (Map.Entry<String, String> comboEntry : combo.entrySet()) {
          for (Potential potential : potentials.get(comboEntry.getKey())) {
            if (!potential.getValue().equals(comboEntry.getValue())) {
              continue;
            }

            if (potential.getImpliedVariables().containsKey(variable)) {
              continue combos;
            }
          }
        }

        if (!Objects.equals(combo.get(variable), value)) {
          // FIXME: This is where above breaks: we copy the combo here when we have a conflict.
          // Implications in this combo may not be relevant to this conflict.
          Map<String, String> newCombo = new HashMap<>(combo);
          newCombo.put(variable, value);
          newCombos.add(newCombo);
        }
      } else {
        combo.put(variable, value);
      }
    }
    combos.addAll(newCombos);
  }

  private Stream<Map<String, String>> stream() {
    return combos.stream().filter(c -> !c.isEmpty());
  }

  private class Combination {
    final Map<String, String> explicit;
    final Map<String, String> implicit = new HashMap<>();

    Combination(Map<String, String> explicit) {
      this.explicit = new HashMap<>(explicit);
    }

    Map<String, String> toMap() {
      Map<String, String> combo = new HashMap<>(explicit.size() + implicit.size());
      combo.putAll(implicit);
      combo.putAll(explicit);
      return combo;
    }

    boolean put(String variable, String value) {
      return false;
    }
  }
}

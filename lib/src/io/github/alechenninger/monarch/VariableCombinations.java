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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class VariableCombinations {
  static Stream<Map<String, String>> stream(List<String> variableNames,
      Map<String, String> variables, Map<String, List<Potential>> potentials) {
    Map<String, String> variablesPlusImplied = new HashMap<>(variables);
    Queue<String> varsToExamine = new ArrayDeque<>(variables.keySet());

    while (!varsToExamine.isEmpty()) {
      String var = varsToExamine.poll();
      for (Potential potential : potentials.get(var)) {
        if (!potential.getValue().equals(variablesPlusImplied.get(var))) {
          continue;
        }

        for (Map.Entry<String, String> implied : potential.getImpliedValues().entrySet()) {
          String impliedKey = implied.getKey();
          String impliedValue = implied.getValue();

          if (variablesPlusImplied.containsKey(impliedKey)) {
            String currentValue = variablesPlusImplied.get(impliedKey);
            if (!Objects.equals(currentValue, impliedValue)) {
              throw new IllegalStateException("Conflicting implied values for variable. " +
                  "Variable '" + impliedKey + "' with implied value of '" + impliedValue + "' " +
                  "conflicts with '" + currentValue + "'");
            }
          } else {
            variablesPlusImplied.put(impliedKey, impliedValue);
            varsToExamine.add(impliedKey);
          }
        }
      }
    }

    Map<String, String> usedVars = variablesPlusImplied.entrySet().stream()
        .filter(entry -> variableNames.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    List<String> missingVars = variableNames.stream()
        .filter(var -> !usedVars.keySet().contains(var))
        .collect(Collectors.toList());

    VariableCombinations combos = new VariableCombinations(usedVars, potentials);

    for (String missingVar : missingVars) {
      List<Potential> potentialsForVar = potentials.get(missingVar);

      if (potentialsForVar == null || potentialsForVar.isEmpty()) {
        throw new IllegalStateException("No potentials found for missing variable '" +
            missingVar + "'.");
      }

      for (Potential potential : potentialsForVar) {
        combos.put(missingVar, potential.getValue());
      }
    }

    return combos.stream();
  }

  private final List<Map<String, String>> combos = new ArrayList<>();
  private final Map<String, List<Potential>> potentials;

  private VariableCombinations(Map<String, String> variablesProvided,
      Map<String, List<Potential>> potentials) {
    this.potentials = potentials;

    combos.add(new HashMap<>(variablesProvided));
  }

  private void put(String variable, String value) {
    List<Map<String, String>> newCombos = new ArrayList<>();
    combos: for (Map<String, String> combo : combos) {
      if (potentials.containsKey(variable)) {
        for (Potential potential : potentials.get(variable)) {
          if (!potential.getValue().equals(value)) {
            continue;
          }

          for (Map.Entry<String, String> implied : potential.getImpliedValues().entrySet()) {
            String impliedKey = implied.getKey();
            String impliedValue = implied.getValue();
            if (combo.containsKey(impliedKey)) {
              String currentValue = combo.get(impliedKey);
              if (!Objects.equals(currentValue, impliedValue)) {
                throw new IllegalStateException("Conflicting implied values for variable. " +
                    "Variable '" + impliedKey + "' with implied value of '" + impliedValue + "' " +
                    "conflicts with '" + currentValue + "'");
              }
            } else {
              combo.put(impliedKey, impliedValue);
            }
          }
        }
      }

      if (combo.containsKey(variable)) {
        // Is it already implied in this combo?
        for (Map.Entry<String, String> comboEntry : combo.entrySet()) {
          for (Potential potential : potentials.get(comboEntry.getKey())) {
            if (!potential.getValue().equals(comboEntry.getValue())) {
              continue;
            }

            if (potential.getImpliedValues().containsKey(variable)) {
              continue combos;
            }
          }
        }

        if (!Objects.equals(combo.get(variable), value)) {
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
    return combos.stream();
  }
}

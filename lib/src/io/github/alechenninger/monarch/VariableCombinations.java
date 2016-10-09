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
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class VariableCombinations {
  /**
   * Computes each possible set of variables given the predefined {@code variables} and potential
   * values for keys which are in {@code variableNames} but not defined in {@code variables}.
   */
  static Stream<Map<String, String>> stream(List<String> variableNames,
      Map<String, String> variables, Map<String, List<Potential>> potentials) {
    Map<String, String> usedVars = variables.entrySet().stream()
        .filter(entry -> variableNames.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    List<String> missingVars = variableNames.stream()
        .filter(var -> !usedVars.keySet().contains(var))
        .collect(Collectors.toList());

    VariableCombinations combos = new VariableCombinations(usedVars, potentials, variables);

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
  private final Map<String, String> variables;

  private VariableCombinations(Map<String, String> variablesProvided,
      Map<String, List<Potential>> potentials, Map<String, String> variables) {
    this.potentials = potentials;
    this.variables = variables;

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
              if (variables.containsKey(impliedKey) &&
                  !Objects.equals(variables.get(impliedKey), impliedValue)) {
                return;
              }

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
}

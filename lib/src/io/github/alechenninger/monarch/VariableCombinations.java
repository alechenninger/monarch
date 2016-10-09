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
import java.util.Optional;
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

  private final List<Combination> combos = new ArrayList<>();
  private final Map<String, List<Potential>> potentials;
  private final Map<String, String> variables;

  private VariableCombinations(Map<String, String> variablesProvided,
      Map<String, List<Potential>> potentials, Map<String, String> variables) {
    this.potentials = potentials;
    this.variables = variables;

    // TODO: What if there are no combinations actually? We are filtering empty in .stream(), but
    // might be better way.
    combos.add(new Combination(variablesProvided));
  }

  private void put(String variable, String value) {
    List<Combination> newCombos = new ArrayList<>();

    for (Combination combo : combos) {
      combo.put(variable, value).ifPresent(newCombos::add);
    }
    combos.addAll(newCombos);
  }

  private Stream<Map<String, String>> stream() {
    return combos.stream().map(Combination::toMap).filter(c -> !c.isEmpty());
  }

  private class Combination {
    final Map<String, String> explicit;
    final Map<String, String> implied = new HashMap<>();

    Combination() {
      this.explicit = new HashMap<>();
    }

    /**
     * @param explicit Expected to already include all implied variables as these can be computed in
     *                 advance and shared.
     */
    Combination(Map<String, String> explicit) {
      this.explicit = new HashMap<>(explicit);
    }

    Combination startNewCombinationWith(String variable, String value) {
      Combination combination = new Combination(explicit);
      combination.explicit.put(variable, value);
      return combination;
    }

    Map<String, String> toMap() {
      Map<String, String> combo = new HashMap<>(explicit.size() + implied.size());
      combo.putAll(explicit);
      combo.putAll(implied);
      return combo;
    }

    boolean containsKey(String key) {
      return explicit.containsKey(key) || implied.containsKey(key);
    }

    String get(String key) {
      if (explicit.containsKey(key)) {
        return explicit.get(key);
      }

      return implied.get(key);
    }

    boolean canPut(String variable, String value) {
      
    }

    Optional<Combination> put(String variable, String value) {
      // See if this variable would imply other variables.
      if (potentials.containsKey(variable)) {
        // Find the potential for this value
        for (Potential potential : potentials.get(variable)) {
          if (!potential.getValue().equals(value)) {
            continue;
          }

          Map<String, String> impliedAdditions = new HashMap<>();

          // Loop through all implied variables for this value.
          for (Map.Entry<String, String> implied : potential.getImpliedVariables().entrySet()) {
            String impliedKey = implied.getKey();
            String impliedValue = implied.getValue();

            // If already included as something else, then don't add this value or its implications.
            if (containsKey(impliedKey)) {
              String currentValue = get(impliedKey);
              if (!Objects.equals(currentValue, impliedValue)) {
                return Optional.empty();
              }
            } else {
              // Not part of the combination, but can still be already included in variables.
              // TODO: Maybe just include variables to start, making this check not needed
              if (variables.containsKey(impliedKey) &&
                  !Objects.equals(variables.get(impliedKey), impliedValue)) {
                return Optional.empty();
              }

              // Nothing conflicts with this implication; store it and keeping checking.
              impliedAdditions.put(impliedKey, impliedValue);
            }
          }

          // Nothing conflicted with any implications, store them for good.
          implied.putAll(impliedAdditions);
        }
      }

      if (explicit.containsKey(variable)) {
        if (containsKeyAndValue(variable, value)) {
          return Optional.empty();
        }

        return Optional.of(startNewCombinationWith(variable, value));
      }

      explicit.put(variable, value);

      return Optional.empty();
    }

    boolean containsKeyAndValue(String variable, String value) {
      if (explicit.containsKey(variable)) {
        return Objects.equals(explicit.get(variable), value);
      }

      if (implied.containsKey(variable)) {
        return Objects.equals(implied.get(variable), value);
      }
    }
  }
}

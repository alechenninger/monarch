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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class VariableCombinations {
  static Stream<Map<String, String>> stream(List<String> variableNames,
      Map<String, String> variables, Map<String, List<String>> potentials) {
    Map<String, String> usedVars = variables.entrySet().stream()
        .filter(entry -> variableNames.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    List<String> missingVars = variableNames.stream()
        .filter(var -> !usedVars.keySet().contains(var))
        .collect(Collectors.toList());

    VariableCombinations combos = new VariableCombinations(usedVars);

    for (String missingVar : missingVars) {
      for (String potential :
          Optional.ofNullable(potentials.get(missingVar)).orElse(Collections.emptyList())) {
        combos.put(missingVar, potential);
      }
    }

    return combos.stream();
  }

  private final List<Map<String, String>> combos = new ArrayList<>();

  private VariableCombinations(Map<String, String> variablesProvided) {
    combos.add(new HashMap<>(variablesProvided));
  }

  private void put(String variable, String value) {
    List<Map<String, String>> newCombos = new ArrayList<>();
    for (Map<String, String> combo : combos) {
      if (combo.containsKey(variable)) {
        Map<String, String> newCombo = new HashMap<>(combo);
        newCombo.put(variable, value);
        newCombos.add(newCombo);
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

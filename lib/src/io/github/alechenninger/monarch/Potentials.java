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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

public interface Potentials {
  /** Variable used in dynamic hierarchy. */
  String forVariable();
  List<Potential> asList();
  Map<String, String> getImpliedVariablesFor(String value);

  static Map<String, String> getImpliedVariablesFor(Map<String, String> variables,
      Map<String, Potentials> allPotentials) {
    Map<String, String> variablesPlusImplied = new HashMap<>(variables);
    Queue<String> varsToExamine = new ArrayDeque<>(variables.keySet());

    while (!varsToExamine.isEmpty()) {
      String var = varsToExamine.poll();
      for (Potential potential : allPotentials.get(var).asList()) {
        if (!potential.value().equals(variablesPlusImplied.get(var))) {
          continue;
        }

        for (Map.Entry<String, String> implied : potential.impliedAssignments().entrySet()) {
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

    return variablesPlusImplied;
  }
}

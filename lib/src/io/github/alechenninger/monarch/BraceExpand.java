/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2017 Alec Henninger
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

import me.andrz.brace.BraceExpansion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class BraceExpand {
  private BraceExpand() {}

  static List<String> string(String string) {
    return BraceExpansion.expand(string);
  }

  static <T> Map<String, T> keysOf(Map<String, T> map) {
    Map<String, T> expanded = new HashMap<>(map.size());

    for (Map.Entry<String, T> entry : map.entrySet()) {
      for (String key : BraceExpansion.expand(entry.getKey())) {
        expanded.put(key, entry.getValue());
      }
    }

    return expanded;
  }

  static List<Map<String, String>> keysAndValuesOf(Map<String, String> map) {
    List<Map<String, String>> expanded = new ArrayList<>();
    expanded.add(new HashMap<>());

    for (Map.Entry<String, String> entry : map.entrySet()) {
      for (String key : BraceExpansion.expand(entry.getKey())) {
        List<Map<String, String>> newPermutations = new ArrayList<>();

        for (String value : BraceExpansion.expand(entry.getValue())) {
          for (Map<String, String> permutation : expanded) {
            if (permutation.containsKey(key)) {
              if (Objects.equals(permutation.get(key), value)) {
                continue;
              }

              Map<String, String> newPermutation = new HashMap<>(permutation);
              newPermutation.put(key, value);
              newPermutations.add(newPermutation);
            } else {
              permutation.put(key, value);
            }
          }

        }

        expanded.addAll(newPermutations);
      }
    }

    return expanded;
  }
}

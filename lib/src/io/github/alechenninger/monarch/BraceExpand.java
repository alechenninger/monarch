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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BraceExpand {
  private BraceExpand() {}

  static List<String> string(String string) {
    return BraceExpansion.expand(string);
  }

  static <T> Map<String, T> keysOf(Map<String, T> map) {
    Map<String, T> expanded = new HashMap<>(map.size());

    for (Map.Entry<String, T> entry : map.entrySet()) {
      T value = entry.getValue();
      for (String key : BraceExpansion.expand(entry.getKey())) {
        expanded.put(key, value);
      }
    }

    return expanded;
  }
}

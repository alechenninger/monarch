/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2015  Alec Henninger
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MapMerger implements Merger {
  private final Map<String, Object> merged;

  public MapMerger(Map<String, Object> original) {
    this.merged = new HashMap<>(original);
  }

  @Override
  public void merge(Object toMerge) {
    if (!(toMerge instanceof Map)) {
      throw new IllegalArgumentException("Can only merge a map with other maps!");
    }

    merged.putAll((Map) toMerge);
  }

  @Override
  public void unmerge(Object toUnmerge) {
    if (!(toUnmerge instanceof Map)) {
      throw new IllegalArgumentException("Can only unmerge a map from other maps!");
    }

    Map<String, Object> unmergeMap = (Map) toUnmerge;

    for (String keyToUnmerge : unmergeMap.keySet()) {
      merged.remove(keyToUnmerge);
    }
  }

  @Override
  public boolean contains(Object value) {
    if (!(value instanceof Map)) {
      return false;
    }

    Map<String, Object> valueAsMap = (Map) value;

    for (Map.Entry<String, Object> entryInValue : valueAsMap.entrySet()) {
      String keyInValue = entryInValue.getKey();

      if (!merged.containsKey(keyInValue)) {
        return false;
      }

      if (!Objects.equals(merged.get(keyInValue), entryInValue.getValue())) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Object getMerged() {
    return merged;
  }
}

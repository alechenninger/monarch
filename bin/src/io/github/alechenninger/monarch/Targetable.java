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

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Targetable {
  static Targetable of(Source source) {
    return new Targetable() {
      @Override
      public List<Source> descendants() {
        return source.descendants();
      }

      @Override
      public Map<String, Map<String, Object>> generateSources(Monarch monarch,
          Iterable<Change> changes, Map<String, Map<String, Object>> data, Set<String> mergeKeys) {
        return monarch.generateSources(source, changes, data, mergeKeys);
      }
    };
  }

  static Targetable of(Hierarchy hierarchy) {
    return new Targetable() {
      @Override
      public List<Source> descendants() {
        return hierarchy.allSources();
      }

      @Override
      public Map<String, Map<String, Object>> generateSources(Monarch monarch,
          Iterable<Change> changes, Map<String, Map<String, Object>> data, Set<String> mergeKeys) {
        return monarch.generateSources(hierarchy, changes, data, mergeKeys);
      }
    };
  }

  List<Source> descendants();
  Map<String, Map<String, Object>> generateSources(Monarch monarch, Iterable<Change> changes,
      Map<String, Map<String, Object>> data, Set<String> mergeKeys);
}

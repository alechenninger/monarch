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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface Change {
  static Change forVariables(Map<String, String> variables, Map<String, Object> set,
      Collection<String> remove) {
    return new DefaultChange(SourceSpec.byVariables(variables), set, remove);
  }

  static Change forPath(String path, Map<String, Object> set, Collection<String> remove) {
    return new DefaultChange(SourceSpec.byPath(path), set, remove);
  }

  /** @see #toMap */
  static List<Change> fromMap(Map<String, Object> map) {
    if (map == null) {
      throw new IllegalArgumentException("Cannot create a change from 'null'.");
    }

    Map<String, Object> set = (Map<String, Object>) map.get("set");
    Collection<String> remove = (Collection<String>) map.get("remove");
    List<SourceSpec> sources = SourceSpec.fromStringOrMap(map.get("source"));

    return sources.stream()
        .map(s -> new DefaultChange(s, set, remove))
        .collect(Collectors.toList());
  }

  SourceSpec sourceSpec();
  Map<String, Object> set();
  Set<String> remove();

  /** @see #fromMap(Map) */
  Map<String, Object> toMap();
}


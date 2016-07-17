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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

public interface Change {
  static Change forVariables(Map<String, String> variables, Map<String, Object> set,
      Collection<String> remove) {
    return new DefaultChange(SourceSpec.byVariables(variables), set, remove);
  }

  static Change forPath(String path, Map<String, Object> set, Collection<String> remove) {
    return new DefaultChange(SourceSpec.byPath(path), set, remove);
  }

  /** @see #toMap */
  static Change fromMap(Map<String, Object> map) {
    if (map == null) {
      throw new IllegalArgumentException("Cannot create a change from 'null'.");
    }
    Map<String, Object> set = (Map<String, Object>) map.get("set");
    Collection<String> remove = (Collection<String>) map.get("remove");
    if (set == null) {
      set = Collections.emptyMap();
    }
    if (remove == null) {
      remove = Collections.emptyList();
    }

    SourceSpec source = SourceSpec.fromStringOrMap(map.get("source"));

    return new DefaultChange(source, set, remove);
  }

  SourceSpec sourceSpec();
  Map<String, Object> set();
  Set<String> remove();

  /** @see #fromMap(Map) */
  Map<String, Object> toMap();
}

class DefaultChange implements Change {
  private final SourceSpec source;
  private final Map<String, Object> set;
  private final Set<String> remove;

  DefaultChange(SourceSpec source, Map<String, Object> set, Collection<String> remove) {
    this.source = source;
    this.set = Collections.unmodifiableMap(new TreeMap<>(set));
    this.remove = Collections.unmodifiableSet(new HashSet<>(remove));
  }

  @Override
  public SourceSpec sourceSpec() {
    return source;
  }

  @Override
  public Map<String, Object> set() {
    return set;
  }

  @Override
  public Set<String> remove() {
    return remove;
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("source", source.toStringOrMap());
    if (set != null && !set.isEmpty()) {
      map.put("set", set);
    }
    if (remove != null && !remove.isEmpty()) {
      map.put("remove", remove);
    }
    return Collections.unmodifiableMap(map);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultChange change = (DefaultChange) o;
    return Objects.equals(source, change.source) &&
        Objects.equals(set, change.set) &&
        Objects.equals(remove, change.remove);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, set, remove);
  }

  @Override
  public String toString() {
    return "DefaultChange{" +
        "source='" + source + '\'' +
        ", set=" + set +
        ", remove=" + remove +
        '}';
  }
}

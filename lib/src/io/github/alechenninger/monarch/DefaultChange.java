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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

class DefaultChange implements Change {
  private final SourceSpec source;
  private final Map<String, Object> set;
  private final Set<String> remove;

  DefaultChange(SourceSpec source, Map<String, Object> set, Collection<String> remove) {
    this.source = source;
    this.set = set == null
        ? Collections.emptyMap()
        : Collections.unmodifiableMap(new TreeMap<>(set));
    this.remove = remove == null
        ? Collections.emptySet()
        : Collections.unmodifiableSet(new HashSet<>(remove));
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
      map.put("remove", new ArrayList<>(remove));
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

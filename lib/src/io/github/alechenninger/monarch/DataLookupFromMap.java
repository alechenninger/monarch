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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class DataLookupFromMap implements DataLookup {
  private final Map<String, Map<String, Object>> data;
  private final String source;
  private final Hierarchy hierarchy;
  private final Set<String> mergeKeys;

  public DataLookupFromMap(Map<String, Map<String, Object>> data, String source,
      Hierarchy hierarchy, Set<String> mergeKeys) {
    this.data = data;
    this.source = source;
    this.hierarchy = hierarchy;
    this.mergeKeys = mergeKeys;
  }

  @Override
  public Optional<Object> lookup(String key) {
    if (mergeKeys.contains(key)) {
      Merger merger = null;

      for (String ancestor : sourceAncestry()) {
        Map<String, Object> ancestorData = getDataBySource(ancestor);

        if (ancestorData.containsKey(key)) {
          if (merger == null) {
            merger = Merger.startingWith(ancestorData.get(key));
          } else {
            merger.merge(ancestorData.get(key));
          }
        }
      }
      return Optional.ofNullable(merger).map(Merger::getMerged);
    }

    for (String ancestor : sourceAncestry()) {
      Map<String, Object> ancestorData = getDataBySource(ancestor);
      if (ancestorData.containsKey(key)) {
        return Optional.of(ancestorData.get(key));
      }
    }

    return Optional.empty();
  }

  @Override
  public List<SourceToValue> sourcesOf(String key) {
    List<SourceToValue> sources = new ArrayList<>();

    for (String ancestor : sourceAncestry()) {
      Map<String, Object> ancestorData = getDataBySource(ancestor);
      if (ancestorData.containsKey(key)) {
        sources.add(new SourceToValue(ancestor, ancestorData.get(key)));
      }
    }

    return sources;
  }

  @Override
  public List<SourceToValue> sourcesOf(String key, Object value) {
    List<SourceToValue> sources = new ArrayList<>();

    for (String ancestor : sourceAncestry()) {
      Map<String, Object> ancestorData = getDataBySource(ancestor);
      if (ancestorData.containsKey(key)) {
        Object ancestorValue = ancestorData.get(key);

        if (mergeKeys.contains(key)) {
          Merger merger = Merger.startingWith(ancestorValue);
          if (merger.contains(value)) {
            sources.add(new SourceToValue(ancestor, ancestorValue));
          }
        } else {
          if (Objects.equals(ancestorValue, value)) {
            sources.add(new SourceToValue(ancestor, ancestorValue));
          }
        }
      }
    }

    return sources;
  }

  @Override
  public boolean isValueInherited(String key, Object value) {
    List<SourceToValue> sources = sourcesOf(key, value);
    int sourcesCount = sources.size();

    if (sourcesCount == 1 && sources.get(0).source().equals(source)) {
      return false;
    }

    return sourcesCount > 0;
  }

  private List<String> sourceAncestry() {
    return hierarchy.ancestorsOf(source)
        .orElseThrow(() -> new NoSuchElementException("Could not find pivot source in hierarchy. "
            + "Pivot source: " + source + ". Hierarchy: \n" + hierarchy));
  }

  private Map<String, Object> getDataBySource(String source) {
    return data.getOrDefault(source, Collections.emptyMap());
  }
}

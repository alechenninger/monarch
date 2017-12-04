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
import java.util.stream.Collectors;

public class DataLookupFromMap implements DataLookup {
  private final Map<String, Map<String, Object>> data;
  private final String path;
  private final Source source;
  private final Set<String> mergeKeys;

  public DataLookupFromMap(Map<String, Map<String, Object>> data, Source source,
      Set<String> mergeKeys) {
    this.data = data;
    this.path = source.path();
    this.source = source;
    this.mergeKeys = mergeKeys;
  }

  @Override
  public List<Object> lookup(String key) {
    List<Object> values = new ArrayList<>();

    if (mergeKeys.contains(key)) {
      for (List<Source> line : sourceAncestries()) {
        Merger mergerForLine = null;

        for (Source ancestor : line) {
          Map<String, Object> ancestorData = getDataBySource(ancestor);

          if (ancestorData.containsKey(key)) {
            if (mergerForLine == null) {
              mergerForLine = Merger.startingWith(ancestorData.get(key));
            } else {
              mergerForLine.merge(ancestorData.get(key));
            }
          }
        }

        if (mergerForLine != null) {
          values.add(mergerForLine.getMerged());
        }
      }

      return values;
    }

    for (List<Source> lines : sourceAncestries()) {
      for (Source ancestor : lines) {
        Map<String, Object> ancestorData = getDataBySource(ancestor);
        if (ancestorData.containsKey(key)) {
          values.add(ancestorData.get(key));
          break; // next line...
        }
      }
    }

    return values;
  }

  @Override
  public List<List<SourceToValue>> sourcesOf(String key) {
    List<List<SourceToValue>> byLineage = new ArrayList<>();

    for (List<Source> lineage : sourceAncestries()) {
      List<SourceToValue> sources = new ArrayList<>();

      for (Source ancestor : lineage) {
        Map<String, Object> ancestorData = getDataBySource(ancestor);
        if (ancestorData.containsKey(key)) {
          sources.add(new SourceToValue(ancestor.path(), ancestorData.get(key)));
        }
      }

      byLineage.add(sources);
    }

    return byLineage;
  }

  @Override
  public List<List<SourceToValue>> sourcesOf(String key, Object value) {
    List<List<SourceToValue>> byLineage = new ArrayList<>();

    for (List<Source> lineage : sourceAncestries()) {
      List<SourceToValue> sources = new ArrayList<>();

      for (Source ancestor : lineage) {
        Map<String, Object> ancestorData = getDataBySource(ancestor);
        if (ancestorData.containsKey(key)) {
          Object ancestorValue = ancestorData.get(key);

          if (mergeKeys.contains(key)) {
            Merger merger = Merger.startingWith(ancestorValue);
            if (merger.contains(value)) {
              sources.add(new SourceToValue(ancestor.path(), ancestorValue));
            }
          } else {
            if (Objects.equals(ancestorValue, value)) {
              sources.add(new SourceToValue(ancestor.path(), ancestorValue));
            }
          }
        }
      }

      byLineage.add(sources);
    }

    return byLineage;
  }

  @Override
  public boolean isValueInherited(String key, Object value) {
    List<List<SourceToValue>> lines = sourcesOf(key, value);

    for (List<SourceToValue> sources : lines) {
      int sourcesCount = sources.size();

      if (sourcesCount == 1 && sources.get(0).source().equals(path)) {
        return false;
      }

      if (sourcesCount == 0) {
        return false;
      }
    }

    return true;
  }

  @Override
  public String toString() {
    return "DataLookupFromMap{" +
        "path='" + path + '\'' +
        '}';
  }

  private List<Level> sourceAncestry() {
    return source.lineage();
  }

  private List<List<Source>> sourceAncestries() {
    return source.lineage().get(0).lineages();
  }

  private Map<String, Object> getDataBySource(Source source) {
    return Optional.ofNullable(data.get(source.path())).orElse(Collections.emptyMap());
  }
}

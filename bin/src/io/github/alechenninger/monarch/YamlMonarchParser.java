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

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class YamlMonarchParser implements MonarchParser {
  private final Yaml yaml;

  public YamlMonarchParser(Yaml yaml) {
    this.yaml = Objects.requireNonNull(yaml, "yaml");
  }

  @Override
  public Hierarchy parseHierarchy(InputStream hierarchyInput) {
    Object parsedHierarchy = yaml.load(hierarchyInput);
    return Hierarchy.fromStringListOrMap(parsedHierarchy);
  }

  @Override
  public Iterable<Change> parseChanges(InputStream changesInput) {
    try {
      Iterable<Object> parsedChanges = yaml.loadAll(changesInput);
      List<Change> changes = new ArrayList<>();

      for (Object parsedChange : parsedChanges) {
        if (parsedChange == null) continue;

        Map<String, Object> parsedAsMap = (Map<String, Object>) parsedChange;
        changes.add(Change.fromMap(parsedAsMap));
      }

      return changes;
    } catch (ClassCastException e) {
      throw new MonarchException("Expected changes yaml to parse as a map. See help for example.",
          e);
    }
  }

  @Override
  public Map<String, Map<String, Object>> readData(Collection<String> sources, Path dataDir) {
    Map<String, Map<String, Object>> data = new LinkedHashMap<>(sources.size());

    try {
      for (String source : sources) {
        Path sourcePath = dataDir.resolve(source);
        if (Files.exists(sourcePath)) {
          Map<String, Object> dataForSource = (Map<String, Object>) yaml.load(
              Files.newInputStream(sourcePath));

          if (dataForSource == null) {
            dataForSource = Collections.emptyMap();
          }

          data.put(source, dataForSource);
        }
      }
    } catch (IOException e) {
      throw new MonarchException("Error reading data source.", e);
    } catch (ClassCastException e) {
      throw new MonarchException("Expected data source to parse as map.", e);
    }

    return data;
  }

  @Override
  public Map<String, Object> readAsMap(InputStream inputStream) {
    try {
      return (Map<String, Object>) yaml.load(inputStream);
    } catch (ClassCastException e) {
      throw new MonarchException("Expected path to parse as map.", e);
    }
  }
}

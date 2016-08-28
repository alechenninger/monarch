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

import com.google.common.base.Joiner;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class YamlMonarchParser implements MonarchParser {
  private final Yaml yaml;

  private static final String BEGIN_MONARCH_MANAGED = "# --- Begin managed by monarch";
  private static final String END_MONARCH_MANAGED = "# --- End managed by monarch";

  public YamlMonarchParser(Yaml yaml) {
    this.yaml = Objects.requireNonNull(yaml, "yaml");
  }

  @Override
  public Hierarchy parseHierarchy(InputStream hierarchyInput) {
    Object parsedHierarchy = yaml.load(hierarchyInput);
    return Hierarchy.fromStringListOrMap(parsedHierarchy);
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<Change> parseChanges(InputStream changesInput) {
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
  @SuppressWarnings("unchecked")
  public Map<String, Object> parseMap(InputStream inputStream) {
    try {
      return Optional.ofNullable((Map<String, Object>) yaml.load(inputStream))
          .orElse(Collections.emptyMap());
    } catch (ClassCastException e) {
      throw new MonarchException("Expected inputStream to parse as map.", e);
    }
  }

  @Override
  public SourceData newSourceData() {
    return new YamlSourceData(Collections.emptyMap(), Collections.emptyMap(), "", "");
  }

  @Override
  @SuppressWarnings("unchecked")
  public SourceData parseData(InputStream in) throws IOException {
    Reader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
    try (BufferedReader buffered = new BufferedReader(reader)) {
      String dataString = buffered.lines().collect(Collectors.joining("\n"));
      int managedBegin = dataString.indexOf(BEGIN_MONARCH_MANAGED);
      int managedEnd = dataString.indexOf(END_MONARCH_MANAGED) + END_MONARCH_MANAGED.length();

      if (managedBegin == -1) {
        managedBegin = dataString.length();
        managedEnd = managedBegin;
      }

      String pre = dataString.substring(0, managedBegin).trim();
      String managed = dataString.substring(managedBegin, managedEnd);
      String post = dataString.substring(managedEnd).trim();

      Map<String, Object> unmanagedData = new HashMap<>();
      Map<String, Object> preData = yaml.loadAs(pre, Map.class);
      Map<String, Object> postData = yaml.loadAs(post, Map.class);

      if (preData != null) unmanagedData.putAll(preData);
      if (postData != null) unmanagedData.putAll(postData);

      Map<String, Object> managedData = Optional
          .ofNullable(yaml.loadAs(managed, Map.class))
          .orElse(Collections.emptyMap());

      Map<String, Object> data = new HashMap<>();
      data.putAll(unmanagedData);
      data.putAll(managedData);

      // TODO: Consider warning if unmanaged / managed have overlapping keys
      return new YamlSourceData(data, unmanagedData, pre, post);
    }
  }

  private class YamlSourceData implements SourceData {
    private final Map<String, Object> data;
    private final Map<String, Object> unmanagedData;
    private final String pre;
    private final String post;

    YamlSourceData(Map<String, Object> data, Map<String, Object> unmanagedData, String pre,
        String post) {
      this.data = Collections.unmodifiableMap(data);
      this.unmanagedData = Objects.requireNonNull(unmanagedData);
      this.pre = Objects.requireNonNull(pre);
      this.post = Objects.requireNonNull(post);
    }

    @Override
    public Map<String, Object> data() {
      return data;
    }

    @Override
    public void writeNew(Map<String, Object> newData, OutputStream out) throws IOException {
      MapDifference<String, Object> diff = Maps.difference(this.data, newData);
      Sets.SetView<String> unmanagedDifferingKeys =
          Sets.intersection(unmanagedData.keySet(), diff.entriesDiffering().keySet());
      Sets.SetView<String> unmanagedRemovedKeys =
          Sets.intersection(unmanagedData.keySet(), diff.entriesOnlyOnLeft().keySet());

      // TODO: Flag to allow source to become managed?
      // We could keep out of unmanaged region as long as we didn't need to touch it.
      // If we did, the user may desire monarch manage the whole file instead of fail.
      if (!unmanagedDifferingKeys.isEmpty() || !unmanagedRemovedKeys.isEmpty()) {
        throw new MonarchException("Update would modify unmanaged region(s) of the data " +
            "source. unmanagedDifferingKeys=" + unmanagedDifferingKeys + " " +
            "unmanagedRemovedKeys=" + unmanagedRemovedKeys);
      }

      SortedMap<String, Object> newManaged = new TreeMap<>(newData);
      unmanagedData.keySet().forEach(newManaged::remove);
      String newManagedYaml = yaml.dump(newManaged).trim();

      String newYaml = Joiner.on('\n').join(pre, '\n' + BEGIN_MONARCH_MANAGED, newManagedYaml,
          END_MONARCH_MANAGED + '\n', post);

      try (OutputStreamWriter writer = new OutputStreamWriter(out, Charset.forName("UTF-8"))) {
        writer.write(newYaml);
        writer.flush();
      }
    }
  }
}

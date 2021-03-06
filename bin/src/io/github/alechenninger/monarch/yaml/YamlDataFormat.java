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

package io.github.alechenninger.monarch.yaml;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import io.github.alechenninger.monarch.Change;
import io.github.alechenninger.monarch.DataFormat;
import io.github.alechenninger.monarch.Hierarchy;
import io.github.alechenninger.monarch.MonarchException;
import io.github.alechenninger.monarch.SourceData;
import io.github.alechenninger.monarch.yaml.YamlConfiguration.Isolate;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

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

public class YamlDataFormat implements DataFormat {
  private final Yaml yaml;
  private final UpdateStrategy updateStrategy;

  private static final String BEGIN_MONARCH_MANAGED = "# --- Begin managed by monarch";
  private static final String END_MONARCH_MANAGED = "# --- End managed by monarch";

  public YamlDataFormat() {
    this(YamlConfiguration.DEFAULT);
  }

  public YamlDataFormat(YamlConfiguration config) {
    Objects.requireNonNull(config);

    // TODO: Allow configuring more stuff maybe
    DumperOptions options = new DumperOptions();
    options.setIndent(config.indent());
    options.setPrettyFlow(true);
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    this.yaml = new Yaml(options);
    this.updateStrategy = UpdateStrategy.fromYamlConfiguration(config, yaml);
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
        changes.addAll(Change.fromMap(parsedAsMap));
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
    return new YamlSourceData();
  }

  @Override
  @SuppressWarnings("unchecked")
  public SourceData parseData(InputStream in) throws IOException {
    try (Reader reader = new InputStreamReader(in, Charset.forName("UTF-8"));) {
      return new YamlSourceData(CharStreams.toString(reader));
    }
  }

  private class YamlSourceData implements SourceData {
    private final Map<String, Object> data;
    private final Map<String, Object> managed;
    private final Map<String, Object> unmanaged;
    private final String pre;
    private final String post;

    YamlSourceData() {
      this.data = Collections.emptyMap();
      this.managed = Collections.emptyMap();
      this.unmanaged = Collections.emptyMap();
      this.pre = "";
      this.post = "";
    }

    @SuppressWarnings("unchecked")
    YamlSourceData(String dataString) {
      int managedBegin = dataString.indexOf(BEGIN_MONARCH_MANAGED);
      int managedEnd = dataString.lastIndexOf(END_MONARCH_MANAGED) + END_MONARCH_MANAGED.length();

      if (managedBegin == -1) {
        managedBegin = dataString.length();
        managedEnd = managedBegin;
      } else if (managedEnd < END_MONARCH_MANAGED.length()) {
        managedEnd = dataString.length();
      }

      String managed = dataString.substring(managedBegin, managedEnd);
      this.managed = Optional.ofNullable(yaml.loadAs(managed, Map.class))
          .orElse(Collections.emptyMap());

      this.pre = dataString.substring(0, managedBegin);
      this.post = dataString.substring(managedEnd);
      this.unmanaged = new HashMap<>();

      Map<String, Object> preData = yaml.loadAs(pre, Map.class);
      Map<String, Object> postData = yaml.loadAs(post, Map.class);

      if (preData != null) unmanaged.putAll(preData);
      if (postData != null) unmanaged.putAll(postData);

      // Treat redundancies as unmanaged.
      // TODO: Consider warning if unmanaged / managed have overlapping keys
      unmanaged.keySet().forEach(this.managed::remove);

      Map<String, Object> data = new HashMap<>();
      data.putAll(this.managed);
      data.putAll(unmanaged);
      this.data = Collections.unmodifiableMap(data);
    }

    @Override
    public Map<String, Object> data() {
      return data;
    }

    @Override
    public void writeUpdate(Map<String, Object> update, OutputStream out) throws IOException {
      String updateYaml = updateStrategy.getUpdate(this, update);

      if (updateYaml.trim().isEmpty() && managed.isEmpty() && unmanaged.isEmpty()) {
        out.close();
        return;
      }

      try (OutputStreamWriter writer = new OutputStreamWriter(out, Charset.forName("UTF-8"))) {
        writer.write(updateYaml);
        writer.flush();
      }
    }
  }

  interface UpdateStrategy {
    static UpdateStrategy fromYamlConfiguration(YamlConfiguration config, Yaml yaml) {
      return fromIsolation(config.updateIsolation(), yaml);
    }

    static UpdateStrategy fromIsolation(Isolate isolate, Yaml yaml) {
      switch (isolate) {
        case ALWAYS: return new AlwaysIsolateUpdates(yaml);
        case NEVER: return new NeverIsolateUpdates(yaml);
        default: throw new UnsupportedOperationException("TODO");
      }
    }

    /** @return YAML string of updated source data */
    String getUpdate(YamlSourceData data, Map<String, Object> update);
  }

  static class AlwaysIsolateUpdates implements UpdateStrategy {
    private final Yaml yaml;

    AlwaysIsolateUpdates(Yaml yaml) {
      this.yaml = yaml;
    }

    @Override
    public String getUpdate(YamlSourceData data, Map<String, Object> update) {
      MapDifference<String, Object> unmanagedVsUpdate = Maps.difference(data.unmanaged, update);
      Sets.SetView<String> unmanagedDifferingKeys =
          Sets.intersection(data.unmanaged.keySet(), unmanagedVsUpdate.entriesDiffering().keySet());
      Sets.SetView<String> unmanagedRemovedKeys =
          Sets.intersection(data.unmanaged.keySet(), unmanagedVsUpdate.entriesOnlyOnLeft().keySet());

      if (!unmanagedDifferingKeys.isEmpty() || !unmanagedRemovedKeys.isEmpty()) {
        throw new MonarchException("Update would modify unmanaged region(s) of the data " +
            "source. unmanagedDifferingKeys=" + unmanagedDifferingKeys + " " +
            "unmanagedRemovedKeys=" + unmanagedRemovedKeys);
      }

      SortedMap<String, Object> newManaged = new TreeMap<>(unmanagedVsUpdate.entriesOnlyOnRight());
      StringBuilder answer = new StringBuilder();

      if (!data.pre.isEmpty()) {
        answer.append(data.pre);
      }

      if (!newManaged.isEmpty()/* || !data.managed.isEmpty()*/) {
        answer.append(BEGIN_MONARCH_MANAGED).append('\n');
        if (!newManaged.isEmpty()) {
          answer.append(yaml.dump(newManaged).trim()).append('\n');
        }
        answer.append(END_MONARCH_MANAGED).append(data.managed.isEmpty() ? "\n" : "");
      }

      if (!data.post.isEmpty()) {
        answer.append(data.post);
      }

      return emptyIfBlank(answer.toString());
    }

    private static String emptyIfBlank(String string) {
      return string.trim().isEmpty() ? "" : string;
    }
  }

  static class NeverIsolateUpdates implements UpdateStrategy {
    private final Yaml yaml;

    NeverIsolateUpdates(Yaml yaml) {
      this.yaml = yaml;
    }

    @Override
    public String getUpdate(YamlSourceData data, Map<String, Object> update) {
      if (update.isEmpty()) {
        return "";
      }

      return BEGIN_MONARCH_MANAGED + '\n' +
          yaml.dump(new TreeMap<>(update)).trim() + '\n'+
          END_MONARCH_MANAGED + '\n';
    }
  }
}

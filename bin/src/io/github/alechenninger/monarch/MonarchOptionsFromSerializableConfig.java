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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MonarchOptionsFromSerializableConfig implements MonarchOptions {
  private final Config config;
  private final FileSystem fileSystem;

  public MonarchOptionsFromSerializableConfig(Config config, FileSystem fileSystem) {
    this.config = config;
    this.fileSystem = fileSystem;
  }

  @Override
  public Optional<Hierarchy> hierarchy() {
    return Optional.ofNullable(config.getHierarchy()).map(Hierarchy::fromStringListOrMap);
  }

  @Override
  public Set<String> mergeKeys() {
    return Optional.ofNullable(config.getMergeKeys()).orElse(Collections.emptySet());
  }

  @Override
  public Iterable<Change> changes() {
    return Optional.ofNullable(config.getChanges())
        .map(c -> c.stream().map(Change::fromMap).collect(Collectors.toList()))
        .orElse(Collections.emptyList());
  }

  @Override
  public Optional<String> target() {
    return Optional.ofNullable(config.getTarget());
  }

  @Override
  public Optional<Path> dataDir() {
    return Optional.ofNullable(config.getDataDir()).map(fileSystem::getPath);
  }

  @Override
  public Optional<Path> outputDir() {
    return Optional.ofNullable(config.getOutputDir()).map(fileSystem::getPath);
  }

  public static class Config {
    private Object hierarchy;
    private List<Map<String, Object>> changes;
    private Set<String> mergeKeys;
    private String target;
    private String dataDir;
    private String outputDir;

    public Object getHierarchy() {
      return hierarchy;
    }

    public void setHierarchy(Object hierarchy) {
      this.hierarchy = hierarchy;
    }

    public List<Map<String, Object>> getChanges() {
      return changes;
    }

    public void setChanges(List<Map<String, Object>> changes) {
      this.changes = changes;
    }

    public Set<String> getMergeKeys() {
      return mergeKeys;
    }

    public void setMergeKeys(Set<String> mergeKeys) {
      this.mergeKeys = mergeKeys;
    }

    public String getTarget() {
      return target;
    }

    public void setTarget(String target) {
      this.target = target;
    }

    public String getDataDir() {
      return dataDir;
    }

    public void setData(String dataDir) {
      this.dataDir = dataDir;
    }

    public String getOutputDir() {
      return outputDir;
    }

    public void setOutputDir(String outputDir) {
      this.outputDir = outputDir;
    }
  }
}

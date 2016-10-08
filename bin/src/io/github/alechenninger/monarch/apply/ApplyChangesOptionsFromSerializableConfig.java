/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2016  Alec Henninger
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

package io.github.alechenninger.monarch.apply;

import io.github.alechenninger.monarch.Change;
import io.github.alechenninger.monarch.Hierarchy;
import io.github.alechenninger.monarch.SerializableConfig;
import io.github.alechenninger.monarch.SourceSpec;
import io.github.alechenninger.monarch.yaml.YamlConfiguration;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class ApplyChangesOptionsFromSerializableConfig implements ApplyChangesOptions {
  private final SerializableConfig config;
  private final FileSystem fileSystem;

  public ApplyChangesOptionsFromSerializableConfig(SerializableConfig config, FileSystem fileSystem) {
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
    return Collections.emptyList();
  }

  @Override
  public Optional<SourceSpec> target() {
    return Optional.empty();
  }

  @Override
  public Optional<Path> dataDir() {
    return Optional.ofNullable(config.getDataDir()).map(fileSystem::getPath);
  }

  @Override
  public Optional<Path> outputDir() {
    return Optional.ofNullable(config.getOutputDir()).map(fileSystem::getPath);
  }

  @Override
  public Optional<YamlConfiguration> yamlConfiguration() {
    return Optional.ofNullable(config.getDataFormats())
        .map(SerializableConfig.DataFormats::getYaml)
        .map(SerializableConfig.Yaml::toYamlConfiguration);
  }
}

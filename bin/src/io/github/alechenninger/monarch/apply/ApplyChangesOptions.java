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
import io.github.alechenninger.monarch.MonarchException;
import io.github.alechenninger.monarch.MonarchParsers;
import io.github.alechenninger.monarch.ParsersConfiguration;
import io.github.alechenninger.monarch.SerializableConfig;
import io.github.alechenninger.monarch.SourceSpec;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parsed options for applying a changeset to a target in a hierarchy.
 */
public interface ApplyChangesOptions {
  Optional<Hierarchy> hierarchy();
  Set<String> mergeKeys();
  Iterable<Change> changes();
  Optional<SourceSpec> target();
  Optional<Path> dataDir();
  Optional<Path> outputDir();
//  Optional<ParsersConfiguration> parsersConfiguration();

  default ApplyChangesOptions fallingBackTo(ApplyChangesOptions fallback) {
    return new OverridableApplyChangesOptions(this, fallback);
  }

  static ApplyChangesOptions fromInput(ApplyChangesInput inputs, FileSystem fileSystem, MonarchParsers parsers) {
    return new ApplyChangesOptionsFromInput(inputs, parsers, fileSystem);
  }

  static ApplyChangesOptions fromInputAndConfigFiles(ApplyChangesInput input, FileSystem
      fileSystem, MonarchParsers parsers, Path defaultConfigPath) {
    ApplyChangesOptions options = fromInput(input, fileSystem, parsers);

    List<Path> configPaths = input.getConfigPaths()
        .stream()
        .map(fileSystem::getPath)
        .collect(Collectors.toCollection(ArrayList::new));

    configPaths.add(defaultConfigPath);

    for (Path configPath : configPaths) {
      if (Files.exists(configPath)) {
        // TODO: eventually maybe don't assume YAML
        options = options.fallingBackTo(ApplyChangesOptions.fromYaml(configPath));
      }
    }

    return options;
  }

  static ApplyChangesOptions fromYaml(Path configPath) {
    try {
      SerializableConfig config = (SerializableConfig) new Yaml(new Constructor(SerializableConfig.class))
          .load(Files.newInputStream(configPath));
      return new ApplyChangesOptionsFromSerializableConfig(config, configPath.getFileSystem());
    } catch (IOException e) {
      throw new MonarchException("Unable to read config file: " + configPath, e);
    }
  }
}

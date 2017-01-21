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

package io.github.alechenninger.monarch.set;

import io.github.alechenninger.monarch.Change;
import io.github.alechenninger.monarch.DefaultConfigPaths;
import io.github.alechenninger.monarch.Hierarchy;
import io.github.alechenninger.monarch.MonarchException;
import io.github.alechenninger.monarch.DataFormats;
import io.github.alechenninger.monarch.SerializableConfig;
import io.github.alechenninger.monarch.SourceSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public interface UpdateSetOptions {
  Optional<Hierarchy> hierarchy();
  Optional<Path> outputPath();
  Iterable<Change> changes();
  Set<String> removeFromSet();
  Map<String, Object> putInSet();
  Optional<SourceSpec> source();

  Logger log = LoggerFactory.getLogger(UpdateSetOptions.class);

  default UpdateSetOptions fallingBackTo(UpdateSetOptions fallback) {
    return new OverridableUpdateSetOptions(this, fallback);
  }

  static UpdateSetOptions fromInput(UpdateSetInput input, FileSystem fileSystem,
      DataFormats parsers) {
    return new UpdateSetOptionsFromInput(input, parsers, fileSystem);
  }

  static UpdateSetOptions fromInputAndConfigFiles(UpdateSetInput input,
      FileSystem fileSystem, DataFormats parsers, DefaultConfigPaths defaultConfigPaths) {
    UpdateSetOptions options = fromInput(input, fileSystem, parsers);

    List<Path> configPaths = input.getConfigPaths()
        .stream()
        .map(fileSystem::getPath)
        .collect(Collectors.toCollection(ArrayList::new));

    configPaths.addAll(defaultConfigPaths.get(fileSystem));

    for (Path configPath : configPaths) {
      if (Files.exists(configPath) && !Files.isDirectory(configPath)) {
        // TODO: eventually maybe don't assume YAML
        try {
          options = options.fallingBackTo(UpdateSetOptions.fromYaml(configPath));
        } catch (YAMLException | IOException e) {
          log.warn("Unable to read config file: {}", configPath, e);
        }
      }
    }

    return options;
  }

  static UpdateSetOptions fromYaml(Path configPath) throws IOException {
    SerializableConfig config = (SerializableConfig)
        new Yaml(new Constructor(SerializableConfig.class))
            .load(Files.newInputStream(configPath));
    return new UpdateSetOptionsFromSerializableConfig(config);
  }
}

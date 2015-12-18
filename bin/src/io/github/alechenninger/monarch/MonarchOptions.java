package io.github.alechenninger.monarch;

import static io.github.alechenninger.monarch.MonarchOptionsFromSerializableConfig.Config;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface MonarchOptions {
  Optional<Hierarchy> hierarchy();
  Set<String> mergeKeys();
  Iterable<Change> changes();
  Optional<String> pivotSource();
  Optional<Map<String, Map<String, Object>>> data();
  Optional<Path> outputDir();

  default MonarchOptions fallingBackTo(MonarchOptions fallback) {
    return new OverridableOptions(this, fallback);
  }

  static MonarchOptions fromInputs(Inputs inputs, FileSystem fileSystem) {
    return fromInputs(inputs, fileSystem, new MonarchParsers.Default());
  }

  static MonarchOptions fromInputs(Inputs inputs, FileSystem fileSystem, MonarchParsers parsers) {
    return new MonarchOptionsFromInputs(inputs, parsers, fileSystem);
  }

  static MonarchOptions fromYaml(Path configPath) {
    try {
      Config config = (Config) new Yaml(new Constructor(Config.class))
          .load(Files.newInputStream(configPath));
      return new MonarchOptionsFromSerializableConfig(config, configPath.getFileSystem());
    } catch (IOException e) {
      throw new MonarchException("Unable to read config file: " + configPath, e);
    }
  }
}

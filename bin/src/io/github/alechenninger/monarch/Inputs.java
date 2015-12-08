package io.github.alechenninger.monarch;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public interface Inputs {
  Optional<String> getHierarchyPathOrYaml();

  Optional<String> getChangesPathOrYaml();

  Optional<String> getPivotSource();

  Optional<String> getDataDir();

  Optional<String> getConfigPath();

  Optional<String> getOutputDir();

  Optional<String> getMergeKeys();

  default Inputs fallingBackTo(Inputs inputs) {
    return new OverridableInputs(this, inputs);
  }

  default Inputs overriddenWith(Inputs inputs) {
    return new OverridableInputs(inputs, this);
  }

  static Inputs fromYaml(Path configPath) {
    try {
      return (Inputs) new Yaml(new Constructor(SerializableInputs.class))
          .load(Files.newInputStream(configPath));
    } catch (IOException e) {
      throw new MonarchException("Unable to read config file: " + configPath, e);
    }
  }
}

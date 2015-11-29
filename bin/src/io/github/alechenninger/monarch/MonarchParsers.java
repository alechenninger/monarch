package io.github.alechenninger.monarch;

import org.yaml.snakeyaml.Yaml;

public interface MonarchParsers {
  MonarchParser yaml(Yaml yaml);

  default MonarchParser yaml() {
    return yaml(new Yaml());
  }

  class Default implements MonarchParsers {
    @Override
    public MonarchParser yaml(Yaml yaml) {
      return new YamlMonarchParser(yaml);
    }
  }
}

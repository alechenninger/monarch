package io.github.alechenninger.monarch;

import org.yaml.snakeyaml.Yaml;

import java.nio.file.Path;

public interface MonarchParsers {
  MonarchParser yaml();

  default MonarchParser forPath(Path path) {
    String fileName = path.getFileName().toString();
    String extension = getExtensionForFileName(fileName);
    return forExtension(extension);
  }

  default MonarchParser forExtension(String extension) {
    switch (extension.toLowerCase()) {
      case "yml":
      case "yaml": return yaml();
      default:
        throw new UnsupportedOperationException("Extension not supported: " + extension);
    }
  }

  static String getExtensionForFileName(String fileName) {
    int extensionIndex = fileName.lastIndexOf('.');

    if (extensionIndex < 0) {
      throw new MonarchException("Please use a file extension. I don't know how to parse this "
          + "file: " + fileName);
    }

    return fileName.substring(extensionIndex + 1);
  }

  class Default implements MonarchParsers {
    private final Yaml yaml;

    public Default() {
      this(new Yaml());
    }

    public Default(Yaml yaml) {
      this.yaml = yaml;
    }

    @Override
    public MonarchParser yaml() {
      return new YamlMonarchParser(yaml);
    }
  }
}

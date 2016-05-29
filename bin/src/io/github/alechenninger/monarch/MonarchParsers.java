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

import org.yaml.snakeyaml.Yaml;

import java.nio.file.Path;

/**
 * Represents both a collection of known {@link MonarchParser}s by capability, and a strategy for
 * determining which parser to use given a {@link Path} or file extension, etc.
 */
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

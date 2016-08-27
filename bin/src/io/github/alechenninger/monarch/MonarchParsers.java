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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents both a collection of known {@link MonarchParser}s by capability, and a strategy for
 * determining which parser to use given a {@link Path} or file extension, etc.
 */
public interface MonarchParsers {
  MonarchParser yaml();

  default MonarchParser forPath(Path path) {
    String fileName = path.getFileName().toString();

    int extensionIndex = fileName.lastIndexOf('.');
    if (extensionIndex < 0) {
      throw new MonarchException("Please use a file extension. I don't know how to parse this "
          + "file: " + path);
    }
    String extension = fileName.substring(extensionIndex + 1);

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

  default Hierarchy parseHierarchy(String pathOrParseable, FileSystem fileSystem) {
    try {
      Path path = fileSystem.getPath(pathOrParseable);
      MonarchParser parser = forPath(path);

      try {
        return parser.parseHierarchy(Files.newInputStream(path));
      } catch (Exception e) {
        throw new MonarchFileParseException("hierarchy", path, e);
      }
    } catch (InvalidPathException | MonarchException e) {
      if (e instanceof MonarchFileParseException) {
        throw e;
      }

      // Must not be a path, try parsing directly...
      byte[] parseable = pathOrParseable.getBytes(Charset.forName("UTF-8"));
      ByteArrayInputStream parseableStream = new ByteArrayInputStream(parseable);

      try {
        return yaml().parseHierarchy(parseableStream);
      } catch (Exception parseException) {
        // Failed to parse, then maybe it was a path after all...
        e.addSuppressed(parseException);
        throw new MonarchException("Failed to parse hierarchy from: " + pathOrParseable, e);
      }
    }
  }

  default List<Change> parseChanges(String pathOrParseable, FileSystem fileSystem) {
    try {
      Path path = fileSystem.getPath(pathOrParseable);
      MonarchParser parser = forPath(path);

      if (Files.notExists(path)) {
        return Collections.emptyList();
      }

      try {
        return parser.parseChanges(Files.newInputStream(path));
      } catch (Exception e) {
        throw new MonarchFileParseException("changes", path, e);
      }
    } catch (InvalidPathException | MonarchException e) {
      if (e instanceof MonarchFileParseException) {
        throw e;
      }

      byte[] parseable = pathOrParseable.getBytes(Charset.forName("UTF-8"));
      ByteArrayInputStream parseableStream = new ByteArrayInputStream(parseable);

      try {
        return yaml().parseChanges(parseableStream);
      } catch (Exception parseException) {
        e.addSuppressed(parseException);
        throw new MonarchException("Failed to parse changes from: " + pathOrParseable, e);
      }
    }
  }

  default SourceData parseData(String pathOrParseable, FileSystem fileSystem) {
    try {
      Path path = fileSystem.getPath(pathOrParseable);
      return parseData(path);
    } catch (InvalidPathException | MonarchFileParseException e) {
      byte[] parseable = pathOrParseable.getBytes(Charset.forName("UTF-8"));
      ByteArrayInputStream parseableStream = new ByteArrayInputStream(parseable);

      try {
        return yaml().parseData(parseableStream);
      } catch (Exception parseException) {
        e.addSuppressed(parseException);
        throw new MonarchException("Failed to parse data", e);
      }
    }
  }

  default SourceData parseData(Path path) {
    try {
      if (!Files.exists(path)) {
        Path parent = path.getParent();
        if (parent != null) {
          Files.createDirectories(parent);
        }
        Files.write(path, new byte[]{});
      }
      return forPath(path).parseData(Files.newInputStream(path));
    } catch (Exception e) {
      throw new MonarchFileParseException("data", path, e);
    }
  }

  default Map<String, SourceData> parseDataSourcesInHierarchy(Path dataDir, Hierarchy hierarchy) {
    Map<String, SourceData> data = new HashMap<>();

    hierarchy.descendants().stream()
        .map(Source::path)
        .forEach(source -> {
          Path sourcePath = dataDir.resolve(source);
          SourceData sourceData = parseData(sourcePath);
          data.put(source, sourceData);
        });

    return data;
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

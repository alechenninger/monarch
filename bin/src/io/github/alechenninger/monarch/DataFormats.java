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

import io.github.alechenninger.monarch.yaml.YamlConfiguration;
import io.github.alechenninger.monarch.yaml.YamlDataFormat;
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
import java.util.Optional;

/**
 * Represents both a collection of known {@link DataFormat}s by capability, and a strategy for
 * determining which parser to use given a {@link Path} or file extension, etc.
 */
public interface DataFormats {
  DataFormat yaml();

  /**
   * @return New {@code DataFormats} using the supplied configuration. The current object is not
   * reconfigured.
   */
  DataFormats withConfiguration(DataFormatsConfiguration config);

  default DataFormat forPath(Path path) {
    String fileName = path.getFileName().toString();

    int extensionIndex = fileName.lastIndexOf('.');
    if (extensionIndex < 0) {
      throw new MonarchException("Please use a file extension. I don't know how to parse this "
          + "file: " + path);
    }
    String extension = fileName.substring(extensionIndex + 1);

    return forExtension(extension);
  }

  default DataFormat forExtension(String extension) {
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
      DataFormat parser = forPath(path);

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
      DataFormat parser = forPath(path);

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

  default Map<String, Object> parseMap(String pathOrParseable, FileSystem fileSystem) {
    try {
      Path path = fileSystem.getPath(pathOrParseable);
      return parseMap(path);
    } catch (InvalidPathException | MonarchFileParseException e) {
      byte[] parseable = pathOrParseable.getBytes(Charset.forName("UTF-8"));
      ByteArrayInputStream parseableStream = new ByteArrayInputStream(parseable);

      try {
        return yaml().parseMap(parseableStream);
      } catch (Exception parseException) {
        e.addSuppressed(parseException);
        throw new MonarchException("Failed to parse map", e);
      }
    }
  }

  default Map<String, Object> parseMap(Path path) {
    try {
      return forPath(path).parseMap(Files.newInputStream(path));
    } catch (NoSuchFileException e) {
      return Collections.emptyMap();
    } catch (Exception e) {
      throw new MonarchFileParseException("map", path, e);
    }
  }

  /**
   * If {@code pathOrParseable} is a valid file path but the file does not exist, an empty
   * {@link SourceData} will be returned.
   */
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

  /**
   * If {@code path} does not exist, an empty {@link SourceData} will be returned.
   */
  default SourceData parseData(Path path) {
    try {
      return forPath(path).parseData(Files.newInputStream(path));
    } catch (NoSuchFileException e) {
      return forPath(path).newSourceData();
    } catch (Exception e) {
      throw new MonarchFileParseException("data", path, e);
    }
  }

  default Map<String, SourceData> parseDataSourcesInHierarchy(Path dataDir, Hierarchy hierarchy) {
    Map<String, SourceData> data = new HashMap<>();

    hierarchy.descendants().stream()
        // TODO .parallel() but yaml is not threadsafe
        .map(Source::path)
        .forEach(source -> {
          Path sourcePath = dataDir.resolve(source);
          SourceData sourceData = parseData(sourcePath);
          data.put(source, sourceData);
        });

    return data;
  }

  class Default implements DataFormats {
    private final YamlDataFormat yaml;

    public Default() {
      this.yaml = new YamlDataFormat();
    }

    public Default(DataFormatsConfiguration config) {
      this.yaml = config.yamlConfiguration().map(YamlDataFormat::new).orElse(new YamlDataFormat());
    }

    @Override
    public DataFormat yaml() {
      return yaml;
    }

    @Override
    public DataFormats withConfiguration(DataFormatsConfiguration config) {
      return new Default(config);
    }
  }
}

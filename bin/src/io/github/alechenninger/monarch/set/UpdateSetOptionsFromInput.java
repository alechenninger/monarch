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
import io.github.alechenninger.monarch.Hierarchy;
import io.github.alechenninger.monarch.MonarchException;
import io.github.alechenninger.monarch.MonarchParser;
import io.github.alechenninger.monarch.MonarchParsers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class UpdateSetOptionsFromInput implements UpdateSetOptions {
  private final UpdateSetInput input;
  private final MonarchParsers parsers;
  private final FileSystem fileSystem;

  public UpdateSetOptionsFromInput(UpdateSetInput input, MonarchParsers parsers, FileSystem
      fileSystem) {
    this.input = input;
    this.parsers = parsers;
    this.fileSystem = fileSystem;
  }

  @Override
  public Optional<Hierarchy> hierarchy() {
    return input.getHierarchyPathOrYaml().map(pathOrYaml -> {
      try {
        InputAndParser hierarchyInput = tryGetInputStreamForPathOrString(pathOrYaml);

        return hierarchyInput.parser.parseHierarchy(hierarchyInput.stream);
      } catch (IOException e) {
        throw new MonarchException("Error reading hierarchy file.", e);
      }
    });
  }

  @Override
  public Optional<Path> outputPath() {
    return input.getChangesPath().map(fileSystem::getPath);
  }

  @Override
  public Iterable<Change> changes() {
    return input.getChangesPath().map(pathString -> {
      try {
        Path path = fileSystem.getPath(pathString);

        if (Files.notExists(path)) {
          return Collections.<Change>emptyList();
        }

        MonarchParser parser = parsers.forPath(path);
        InputStream stream = Files.newInputStream(path);
        return parser.parseChanges(stream);
      } catch (IOException e) {
        throw new MonarchException("Error reading hierarchy file.", e);
      }
    }).orElse(Collections.emptyList());
  }

  @Override
  public Set<String> removeFromSet() {
    return new HashSet<>(input.getRemovals());
  }

  @Override
  public Map<String, Object> putInSet() {
    return input.getPutPathsOrYaml().stream()
        .map(pathOrYaml -> {
          try {
            InputAndParser inputAndParser = tryGetInputStreamForPathOrString(pathOrYaml);
            return inputAndParser.parser.readAsMap(inputAndParser.stream);
          } catch (IOException e) {
            throw new MonarchException("Error parsing " + pathOrYaml, e);
          }
        })
        .reduce(new HashMap<>(), (m1, m2) -> { m1.putAll(m2); return m1; });
  }

  @Override
  public Optional<String> source() {
    return input.getSource();
  }

  private InputAndParser tryGetInputStreamForPathOrString(String pathOrYaml) throws IOException {
    try {
      Path path = fileSystem.getPath(pathOrYaml);
      return new InputAndParser(Files.newInputStream(path), parsers.forPath(path));
    } catch (IOException e) {
      return new InputAndParser(new ByteArrayInputStream(pathOrYaml.getBytes("UTF-8")),
          /* Assume yaml */ parsers.yaml());
    }
  }

  public static class InputAndParser {
    public final InputStream stream;
    /** Expressed as file extension. */
    public final MonarchParser parser;

    public InputAndParser(InputStream stream, MonarchParser parser) {
      this.stream = stream;
      this.parser = parser;
    }
  }
}

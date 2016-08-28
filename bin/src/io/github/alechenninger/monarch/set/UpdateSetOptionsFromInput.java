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
import io.github.alechenninger.monarch.MonarchParsers;
import io.github.alechenninger.monarch.SourceSpec;

import java.nio.file.FileSystem;
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
    return input.getHierarchyPathOrYaml()
        .map(pathOrYaml -> parsers.parseHierarchy(pathOrYaml, fileSystem));
  }

  @Override
  public Optional<Path> outputPath() {
    return input.getChangesPath().map(fileSystem::getPath);
  }

  @Override
  public Iterable<Change> changes() {
    return input.getChangesPath()
        .map(pathString -> parsers.parseChanges(pathString, fileSystem))
        .orElse(Collections.emptyList());
  }

  @Override
  public Set<String> removeFromSet() {
    return new HashSet<>(input.getRemovals());
  }

  @Override
  public Map<String, Object> putInSet() {
    return input.getPutPathsOrYaml().stream()
        .map(pathOrYaml -> parsers.parseMap(pathOrYaml, fileSystem))
        .reduce(new HashMap<>(), (m1, m2) -> { m1.putAll(m2); return m1; });
  }

  @Override
  public Optional<SourceSpec> source() {
    return input.getSource();
  }
}

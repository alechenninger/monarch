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
import io.github.alechenninger.monarch.SerializableConfig;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class UpdateSetOptionsFromSerializableConfig implements UpdateSetOptions {
  private final SerializableConfig config;

  public UpdateSetOptionsFromSerializableConfig(SerializableConfig config) {
    this.config = config;
  }

  @Override
  public Optional<Hierarchy> hierarchy() {
    return Optional.ofNullable(config.getHierarchy()).map(Hierarchy::fromStringListOrMap);
  }

  @Override
  public Optional<Path> outputPath() {
    return Optional.empty();
  }

  @Override
  public Iterable<Change> changes() {
    return Collections.emptyList();
  }

  @Override
  public Set<String> removeFromSet() {
    return Collections.emptySet();
  }

  @Override
  public Map<String, Object> putInSet() {
    return Collections.emptyMap();
  }

  @Override
  public Optional<String> source() {
    return Optional.empty();
  }


}

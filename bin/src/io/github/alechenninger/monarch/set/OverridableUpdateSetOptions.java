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
import io.github.alechenninger.monarch.ConcatIterable;
import io.github.alechenninger.monarch.Hierarchy;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class OverridableUpdateSetOptions implements UpdateSetOptions {
  private final UpdateSetOptions override;
  private final UpdateSetOptions fallback;

  public OverridableUpdateSetOptions(UpdateSetOptions override, UpdateSetOptions fallback) {
    this.override = override;
    this.fallback = fallback;
  }

  @Override
  public Optional<Hierarchy> hierarchy() {
    return overridden(UpdateSetOptions::hierarchy);
  }

  @Override
  public Optional<Path> outputPath() {
    return overridden(UpdateSetOptions::outputPath);
  }

  @Override
  public Iterable<Change> changes() {
    return new ConcatIterable<>(override.changes(), fallback.changes());
  }

  @Override
  public Set<String> removeFromSet() {
    Set<String> removals = new HashSet<>();
    removals.addAll(override.removeFromSet());
    removals.addAll(fallback.removeFromSet());
    return removals;
  }

  @Override
  public Map<String, Object> putInSet() {
    Map<String, Object> puts = new HashMap<>();
    puts.putAll(fallback.putInSet());
    puts.putAll(override.putInSet());
    return puts;
  }

  @Override
  public Optional<String> source() {
    return overridden(UpdateSetOptions::source);
  }

  private <T> Optional<T> overridden(Function<UpdateSetOptions, Optional<T>> input) {
    Optional<T> maybeOverride = input.apply(override);

    if (maybeOverride.isPresent()) {
      return maybeOverride;
    }

    return input.apply(fallback);
  }
}

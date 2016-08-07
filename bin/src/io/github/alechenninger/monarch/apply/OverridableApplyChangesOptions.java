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

package io.github.alechenninger.monarch.apply;

import io.github.alechenninger.monarch.Change;
import io.github.alechenninger.monarch.SourceSpec;
import io.github.alechenninger.monarch.util.ConcatIterable;
import io.github.alechenninger.monarch.Hierarchy;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class OverridableApplyChangesOptions implements ApplyChangesOptions {
  private final ApplyChangesOptions override;
  private final ApplyChangesOptions fallback;

  public OverridableApplyChangesOptions(ApplyChangesOptions override,
      ApplyChangesOptions fallback) {
    this.override = override;
    this.fallback = fallback;
  }

  @Override
  public Optional<Hierarchy> hierarchy() {
    return overridden(ApplyChangesOptions::hierarchy);
  }

  @Override
  public Set<String> mergeKeys() {
    Set<String> keys = new HashSet<>();
    keys.addAll(override.mergeKeys());
    keys.addAll(fallback.mergeKeys());
    return keys;
  }

  @Override
  public Iterable<Change> changes() {
    return new ConcatIterable<>(override.changes(), fallback.changes());
  }

  @Override
  public Optional<SourceSpec> target() {
    return overridden(ApplyChangesOptions::target);
  }

  @Override
  public Optional<Path> dataDir() {
    return overridden(ApplyChangesOptions::dataDir);
  }

  @Override
  public Optional<Path> outputDir() {
    return overridden(ApplyChangesOptions::outputDir);
  }

  private <T> Optional<T> overridden(Function<ApplyChangesOptions, Optional<T>> input) {
    Optional<T> maybeOverride = input.apply(override);

    if (maybeOverride.isPresent()) {
      return maybeOverride;
    }

    return input.apply(fallback);
  }
}

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

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class OverridableApplyChangesetOptions implements ApplyChangesetOptions {
  private final ApplyChangesetOptions override;
  private final ApplyChangesetOptions fallback;

  public OverridableApplyChangesetOptions(ApplyChangesetOptions override,
      ApplyChangesetOptions fallback) {
    this.override = override;
    this.fallback = fallback;
  }

  @Override
  public Optional<Hierarchy> hierarchy() {
    return overridden(ApplyChangesetOptions::hierarchy);
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
    return () -> new Iterator<Change>() {
      Iterator<Change> overrideIterator = override.changes().iterator();
      Iterator<Change> fallbackIterator = fallback.changes().iterator();

      @Override
      public boolean hasNext() {
        return overrideIterator.hasNext() || fallbackIterator.hasNext();
      }

      @Override
      public Change next() {
        if (overrideIterator.hasNext()) {
          return overrideIterator.next();
        }

        if (fallbackIterator.hasNext()) {
          return fallbackIterator.next();
        }

        throw new IndexOutOfBoundsException();
      }
    };
  }

  @Override
  public Optional<String> target() {
    return overridden(ApplyChangesetOptions::target);
  }

  @Override
  public Optional<Path> dataDir() {
    return overridden(ApplyChangesetOptions::dataDir);
  }

  @Override
  public Optional<Path> outputDir() {
    return overridden(ApplyChangesetOptions::outputDir);
  }

  private <T> Optional<T> overridden(Function<ApplyChangesetOptions, Optional<T>> input) {
    Optional<T> maybeOverride = input.apply(override);

    if (maybeOverride.isPresent()) {
      return maybeOverride;
    }

    return input.apply(fallback);
  }
}

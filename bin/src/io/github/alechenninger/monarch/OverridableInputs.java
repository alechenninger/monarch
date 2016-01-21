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

import java.util.Optional;
import java.util.function.Function;

public class OverridableInputs implements Inputs {
  private final Inputs override;
  private final Inputs fallback;

  public OverridableInputs(Inputs override, Inputs fallback) {
    this.override = override;
    this.fallback = fallback;
  }

  @Override
  public Optional<String> getHierarchyPathOrYaml() {
    return overridden(Inputs::getHierarchyPathOrYaml);
  }

  @Override
  public Optional<String> getChangesPathOrYaml() {
    return overridden(Inputs::getChangesPathOrYaml);
  }

  @Override
  public Optional<String> getTarget() {
    return overridden(Inputs::getTarget);
  }

  @Override
  public Optional<String> getDataDir() {
    return overridden(Inputs::getDataDir);
  }

  @Override
  public Optional<String> getConfigPath() {
    return overridden(Inputs::getConfigPath);
  }

  @Override
  public Optional<String> getOutputDir() {
    return overridden(Inputs::getOutputDir);
  }

  @Override
  public Optional<String> getMergeKeys() {
    return overridden(Inputs::getMergeKeys);
  }

  private Optional<String> overridden(Function<Inputs, Optional<String>> input) {
    Optional<String> maybeOverride = input.apply(override);

    if (maybeOverride.isPresent()) {
      return maybeOverride;
    }

    return input.apply(fallback);
  }
}

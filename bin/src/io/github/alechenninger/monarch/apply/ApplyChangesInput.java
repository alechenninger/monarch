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

import io.github.alechenninger.monarch.SourceSpec;
import io.github.alechenninger.monarch.yaml.YamlConfiguration;

import java.util.List;
import java.util.Optional;

/** User input for applying a changeset to a target in a hierarchy. */
public interface ApplyChangesInput {
  Optional<String> getHierarchyPathOrYaml();

  Optional<String> getChangesPathOrYaml();

  Optional<SourceSpec> getTarget();

  Optional<String> getDataDir();

  List<String> getConfigPaths();

  Optional<String> getOutputDir();

  List<String> getMergeKeys();

  boolean isHelpRequested();

  String getHelpMessage();

  Optional<YamlConfiguration.Isolate> getYamlIsolate();

}

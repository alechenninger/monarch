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

import io.github.alechenninger.monarch.SourceSpec;

import java.util.List;
import java.util.Optional;

public interface UpdateSetInput {
  /**
   * Path of changes to read from and update (or create, if the path does not exist).
   */
  Optional<String> getChangesPath();

  /**
   * Source for the change we wish to modify or create.
   */
  Optional<SourceSpec> getSource();

  /**
   * A list of key value pairs to add or replace in the set block of the source's change.
   *
   * <p>The list may contain paths to yaml files or inline yaml heterogeneously.
   */
  List<String> getPutPathsOrYaml();

  /**
   * A list of keys to remove from the set block of the source's change.
   */
  List<String> getRemovals();

  Optional<String> getHierarchyPathOrYaml();

  List<String> getConfigPaths();

  boolean isHelpRequested();

  String getHelpMessage();
}

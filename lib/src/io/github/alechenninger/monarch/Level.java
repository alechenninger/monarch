/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2017 Alec Henninger
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

import java.util.List;

public interface Level {
  List<Source> sources();
  // TODO: Do we need this?
  boolean skippable();
  boolean isTargetedBy(SourceSpec spec);
  default boolean isEmpty() {
    return sources().isEmpty();
  }
  default int size() {
    return sources().size();
  }
  default Source get(int index) {
    return sources().get(index);
  }
}

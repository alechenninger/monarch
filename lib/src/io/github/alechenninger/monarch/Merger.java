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

import java.util.Collection;
import java.util.Map;

public interface Merger {
  void merge(Object toMerge);
  void unmerge(Object toUnmerge);
  boolean contains(Object couldMerge);
  Object getMerged();

  static Merger startingWith(Object original) {
    if (original instanceof Collection) {
      return new CollectionMerger((Collection<Object>) original);
    }

    if (original instanceof Map) {
      return new MapMerger((Map<String, Object>) original);
    }

    throw new IllegalArgumentException("Can only merge maps or collections but got: " + original);
  };
}

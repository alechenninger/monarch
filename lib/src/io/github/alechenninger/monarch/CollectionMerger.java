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
import java.util.HashSet;
import java.util.Set;

public class CollectionMerger implements Merger {
  private final Set<Object> merged;

  public CollectionMerger(Collection<Object> original) {
    this.merged = new HashSet<>(original);
  }

  @Override
  public void merge(Object toMerge) {
    if (!(toMerge instanceof Collection)) {
      throw new IllegalArgumentException("Can only merge a collection with other collections!");
    }

    merged.addAll((Collection) toMerge);
  }

  @Override
  public void unmerge(Object toUnmerge) {
    if (!(toUnmerge instanceof Collection)) {
      throw new IllegalArgumentException("Can only unmerge a collection from other collections!");
    }

    merged.removeAll((Collection) toUnmerge);
  }

  @Override
  public boolean contains(Object value) {
    return value instanceof Collection && merged.containsAll((Collection) value);
  }

  @Override
  public Object getMerged() {
    return merged;
  }
}

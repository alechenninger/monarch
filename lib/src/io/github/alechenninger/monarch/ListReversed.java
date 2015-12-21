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

import java.util.Iterator;
import java.util.List;

final class ListReversed<T> implements Iterable<T> {
  private final List<T> list;

  ListReversed(List<T> list) {
    this.list = list;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      private int cursor = list.size();

      @Override
      public boolean hasNext() {
        return cursor > 0;
      }

      @Override
      public T next() {
        return list.get(--cursor);
      }
    };
  }
}

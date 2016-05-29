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

package io.github.alechenninger.monarch.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ConcatIterable<T> implements Iterable<T> {
  private final Iterable<T> iterable1;
  private final Iterable<T> iterable2;

  public ConcatIterable(Iterable<T> iterable1, Iterable<T> iterable2) {
    this.iterable1 = iterable1;
    this.iterable2 = iterable2;
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      Iterator<T> iterator1 = iterable1.iterator();
      Iterator<T> iterator2 = iterable2.iterator();

      @Override
      public boolean hasNext() {
        return iterator1.hasNext() || iterator2.hasNext();
      }

      @Override
      public T next() {
        if (iterator1.hasNext()) {
          return iterator1.next();
        }

        if (iterator2.hasNext()) {
          return iterator2.next();
        }

        throw new IndexOutOfBoundsException();
      }
    };
  }

  @Override
  public String toString() {
    return StreamSupport.stream(spliterator(), false).collect(Collectors.toList()).toString();
  }
}

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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

class DescendantsIterator<T extends HasDescendants<T>> implements Iterator<T> {
  private Queue<T> currentLevel = new LinkedList<>();

  DescendantsIterator(Collection<T> nodes) {
    currentLevel.addAll(nodes);
  }

  static <T extends HasDescendants<T>> Stream<T> asStream(Collection<T> nodes) {
    Iterable<T> descendantsIterable = () -> new DescendantsIterator<>(nodes);
    return StreamSupport.stream(descendantsIterable.spliterator(), false);
  }

  @Override
  public boolean hasNext() {
    return !currentLevel.isEmpty();
  }

  @Override
  public T next() {
    T next = currentLevel.remove();

    Collection<T> nextChildren = next.children();
    if (!nextChildren.isEmpty()) {
      currentLevel.addAll(nextChildren);
    }

    return next;
  }
}

interface HasDescendants<T extends HasDescendants<T>> {
  Collection<T> children();
}
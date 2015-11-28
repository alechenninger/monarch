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

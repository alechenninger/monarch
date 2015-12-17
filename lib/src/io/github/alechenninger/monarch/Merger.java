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

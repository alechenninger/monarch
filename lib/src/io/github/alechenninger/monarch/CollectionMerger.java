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

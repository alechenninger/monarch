package io.github.alechenninger.monarch;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MapMerger implements Merger {
  private final Map<String, Object> merged;

  public MapMerger(Map<String, Object> original) {
    this.merged = new HashMap<>(original);
  }

  @Override
  public void merge(Object toMerge) {
    if (!(toMerge instanceof Map)) {
      throw new IllegalArgumentException("Can only merge a map with other maps!");
    }

    merged.putAll((Map) toMerge);
  }

  @Override
  public void unmerge(Object toUnmerge) {
    if (!(toUnmerge instanceof Map)) {
      throw new IllegalArgumentException("Can only unmerge a map from other maps!");
    }

    Map<String, Object> unmergeMap = (Map) toUnmerge;

    for (String keyToUnmerge : unmergeMap.keySet()) {
      merged.remove(keyToUnmerge);
    }
  }

  @Override
  public boolean contains(Object value) {
    if (!(value instanceof Map)) {
      return false;
    }

    Map<String, Object> valueAsMap = (Map) value;

    for (Map.Entry<String, Object> entryInValue : valueAsMap.entrySet()) {
      String keyInValue = entryInValue.getKey();

      if (!merged.containsKey(keyInValue)) {
        return false;
      }

      if (!Objects.equals(merged.get(keyInValue), entryInValue.getValue())) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Object getMerged() {
    return merged;
  }
}

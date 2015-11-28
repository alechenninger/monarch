package io.github.alechenninger.monarch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Change {
  private final String source;
  private final Map<String, Object> set;
  private final List<String> remove;

  public Change(String source, Map<String, Object> set, List<String> remove) {
    this.source = source;
    this.set = new HashMap<>(set);
    this.remove = new ArrayList<>(remove);
  }

  public static Change fromMap(Map<String, Object> map) {
    return new Change(
        (String) map.get("source"),
        (Map<String, Object>) map.getOrDefault("set", Collections.emptyMap()),
        (List<String>) map.getOrDefault("remove", Collections.emptyList()));
  }

  public String source() {
    return source;
  }

  public Map<String, Object> set() {
    return Collections.unmodifiableMap(set);
  }

  public List<String> remove() {
    return remove;

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Change change = (Change) o;
    return Objects.equals(source, change.source) &&
        Objects.equals(set, change.set) &&
        Objects.equals(remove, change.remove);
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, set, remove);
  }
}

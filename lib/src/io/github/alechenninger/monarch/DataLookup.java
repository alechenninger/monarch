package io.github.alechenninger.monarch;

import java.util.List;
import java.util.Optional;

public interface DataLookup {
  Optional<Object> lookup(String key);
  List<SourceToValue> sourcesOf(String key);
  List<SourceToValue> sourcesOf(String key, Object value);
  boolean isValueInherited(String key, Object value);

  final class SourceToValue {
    private final String source;
    private final Object value;

    public SourceToValue(String source, Object value) {
      this.source = source;
      this.value = value;
    }

    public String source() {
      return source;
    }

    public Object value() {
      return value;
    }
  }
}

package io.github.alechenninger.monarch;

import io.github.alechenninger.monarch.DynamicHierarchy.DynamicSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Hierarchy {
  static Hierarchy fromStringListOrMap(Object object) {
    if (object instanceof Map) {
      if (((Map) object).containsKey("sources")) {
        // Could also make sure there is nothing else besides "sources" and "potentials".
      }
    }

    return new StaticHierarchy(StaticHierarchy.Node.fromStringListOrMap(object));
  }

  static DynamicHierarchy fromDynamicSources(List<DynamicSource> sources,
      Map<String, List<String>> potentials) {
    return new DynamicHierarchy(sources, potentials);
  }

  static DynamicHierarchy fromDynamicSourceExpressions(List<String> sourceExpressions,
      Map<String, List<String>> potentials) {
    return fromDynamicSources(DynamicSource.fromExpressions(sourceExpressions), potentials);
  }

  default Optional<Source> sourceFor(SourceSpec spec) {
    return spec.findSource(this);
  }

  Optional<Source> sourceFor(String source);

  Optional<Source> sourceFor(Map<String, String> variables);

  List<Source> descendants();
}

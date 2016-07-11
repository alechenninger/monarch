package io.github.alechenninger.monarch;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Hierarchy {
  static Hierarchy fromStringListOrMap(Object object) {
    return new StaticHierarchy(StaticHierarchy.Node.fromStringListOrMap(object));
  }

  static DynamicHierarchy fromDynamicSources(List<DynamicHierarchy.DynamicSource> sources,
      Map<String, List<String>> potentials) {
    return new DynamicHierarchy(sources, potentials);
  }

  Optional<Source> sourceFor(String source);

  List<Source> descendants();
}

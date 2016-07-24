package io.github.alechenninger.monarch;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Hierarchy {
  static Hierarchy fromStringListOrMap(Object object) {
    if (object instanceof Map) {
      Map map = (Map) object;
      if (map.containsKey("sources")) {
        // Could also make sure there is nothing else besides "sources" and "potentials".
        List<String> sources = (List<String>) map.get("sources");
        Map<String, List<String>> potentials = Optional
            .ofNullable((Map<String, List<String>>) map.get("potentials"))
            .orElse(Collections.emptyMap());

        return fromDynamicSourceExpressions(sources, potentials);
      }
    }

    return new StaticHierarchy(StaticHierarchy.Node.fromStringListOrMap(object));
  }

  static DynamicHierarchy fromDynamicSources(List<DynamicNode> sources,
      Map<String, List<String>> potentials) {
    return new DynamicHierarchy(sources, potentials);
  }

  static DynamicHierarchy fromDynamicSourceExpressions(List<String> sourceExpressions,
      Map<String, List<String>> potentials) {
    return fromDynamicSources(DynamicNode.fromInterpolated(sourceExpressions), potentials);
  }

  default Optional<Source> sourceFor(SourceSpec spec) {
    return spec.findSource(this);
  }

  Optional<Source> sourceFor(String source);

  Optional<Source> sourceFor(Map<String, String> variables);

  List<Source> descendants();
}

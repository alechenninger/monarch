package io.github.alechenninger.monarch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Hierarchy {

  /**
   * Tries to return the most appropriate hierarchy for the object.
   */
  @SuppressWarnings("unchecked")
  static Hierarchy fromStringListOrMap(Object object) {
    if (object instanceof Map) {
      Map map = (Map) object;

      if (map.containsKey("sources")) {
        // TODO: Deprecate using "potentials"
        if ((map.size() == 2 && (map.containsKey("potentials") || map.containsKey("inventory"))) ||
            map.size() == 1) {
          List<String> sources = (List<String>) map.get("sources");
          Object inventory = map.getOrDefault("inventory", map.get("potentials"));

          return fromDynamicSourceExpressions(sources, Inventory.parse(inventory));
        }
      }

      if (map.containsKey("hierarchy")) {
        Logger log = LoggerFactory.getLogger(Hierarchy.class);
        log.warn("Hierarchy had a 'hierarchy' key, and this is being treated as a data source. " +
            "You might have meant to remove this key and replace it its current value");
      }
    }

    return new StaticHierarchy(StaticHierarchy.Node.fromStringListOrMap(object));
  }

  static DynamicHierarchy fromDynamicSources(List<DynamicNode> sources,
      Inventory inventory) {
    return new DynamicHierarchy(sources, inventory);
  }

  static DynamicHierarchy fromDynamicSourceExpressions(List<String> sourceExpressions,
      Inventory inventory) {
    return fromDynamicSources(DynamicNode.fromInterpolated(sourceExpressions), inventory);
  }

  default Optional<Source> sourceFor(SourceSpec spec) {
    return spec.findSource(this);
  }

  Optional<Source> sourceFor(String source);

  Optional<Source> sourceFor(Map<String, String> assignments);

  Optional<Source> sourceFor(Assignments assignments);

  List<Source> descendants();
}

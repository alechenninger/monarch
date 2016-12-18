package io.github.alechenninger.monarch;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public interface Hierarchy {

  /**
   * Tries to return the most appropriate hierarchy for the object.
   */
  static Hierarchy fromStringListOrMap(Object object) {
    if (object instanceof Map) {
      Map map = (Map) object;

      if (map.containsKey("sources")) {
        if ((map.size() == 2 && map.containsKey("potentials")) || map.size() == 1) {
          List<String> sources = (List<String>) map.get("sources");
          Map<String, List<Assignable>> potentials = Optional
              .ofNullable((Map<String, Object>) map.get("potentials"))
              .orElse(Collections.emptyMap())
              .entrySet()
              .stream()
              .collect(Collectors.toMap(Map.Entry::getKey, entry -> {
                Object value = entry.getValue();
                if (value instanceof List) {
                  return ((List<Object>) value).stream()
                      .map(potentialForKey -> {
                        if (potentialForKey instanceof String) {
                          return new Assignable((String) potentialForKey);
                        }

                        if (potentialForKey instanceof Map) {
                          Map<String, Object> potentialToImplications = (Map) potentialForKey;

                          if (potentialToImplications.size() > 1) {
                            throw new IllegalArgumentException("Expected 1 key for potential " +
                                "with implied values. You probably need to correct your YAML " +
                                "indentation. Keys found were: " +
                                potentialToImplications.keySet());
                          }

                          Map.Entry<String, Object> potentialAndImplications =
                              potentialToImplications.entrySet().iterator().next();
                          Object implications = potentialAndImplications.getValue();

                          if (implications instanceof Map) {
                            return new Assignable(
                                potentialAndImplications.getKey(),
                                (Map<String, String>) implications);
                          }

                          if (implications == null) {
                            return new Assignable(potentialAndImplications.getKey());
                          }

                          throw new IllegalArgumentException("Expected implications to be a map");
                        }

                        throw new IllegalArgumentException("Expected potential to be either a string or map");
                      })
                      .collect(Collectors.toList());
                }

                if (value instanceof Map) {
                  return ((Map<String, Object>) value).entrySet().stream()
                      .map(valueEntry -> {
                        String potentialKey = valueEntry.getKey();
                        Map<String, String> implicationsForPotential = (Map) valueEntry.getValue();
                        if (implicationsForPotential == null) {
                          return new Assignable(potentialKey);
                        }

                        return new Assignable(potentialKey, implicationsForPotential);
                      })
                      .collect(Collectors.toList());
                }

                throw new IllegalArgumentException("Expected potentials to be either a list or a map.");
              }));


          return fromDynamicSourceExpressions(sources, Inventory.from(potentials));
        }
      }

      if (map.containsKey("hierarchy")) {
        Logger log = Logger.getAnonymousLogger();
        log.warning("Hierarchy had a 'hierarchy' key, and this is being treated as a data source. " +
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

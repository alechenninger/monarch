package io.github.alechenninger.monarch;

import java.util.List;
import java.util.Optional;

public interface Hierarchy {
  static Hierarchy fromStringListOrMap(Object object) {
    return new StaticHierarchy(Node.fromStringListOrMap(object));
  }

  List<String> descendants();

  Optional<List<String>> descendantsOf(String source);

  Optional<List<String>> ancestorsOf(String source);

  Optional<Hierarchy> hierarchyOf(String source);
}

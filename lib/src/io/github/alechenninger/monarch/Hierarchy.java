package io.github.alechenninger.monarch;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface Hierarchy {
  static Hierarchy fromStringOrSingleKeyMap(Object object) {
    List<Hierarchy> hierarchies = fromStringListOrMap(object);

    if (hierarchies.size() != 1) {
      throw new IllegalArgumentException("Expected exactly one root in parsed hierarcy but got " +
          hierarchies.size());
    }

    return hierarchies.get(0);
  }

  static List<Hierarchy> fromStringListOrMap(Object object) {
    return StaticHierarchy.Node.fromStringListOrMap(object)
        .stream()
        .map(StaticHierarchy::new)
        .collect(Collectors.toList());
  }

  static Hierarchy fromDynamicSources(List<DynamicHierarchy.DynamicSource> sources,
      Map<String, List<String>> potentials, Map<String, String> args) {
    return new DynamicHierarchy(sources, potentials, args);
  }

  String target();

  /**
   * Returns all of the node names in order of <em>nearest to furthest</em>, including the root
   * node. The leaf nodes will be after the "branch" nodes, and later the deeper in the tree they
   * are.
   *
   * <p>For example, given the following tree structure:
   *
   * <pre><code>
   *            foo
   *           /   \
   *         bar   baz
   *        /  \     \
   *       1    2     3
   *                 /  \
   *               fizz buzz
   *                       \
   *                      blue
   * </code></pre>
   *
   * The depth-order is foo, bar, baz, 1, 2, 3, fizz, buzz, blue. Foo is at the top of the tree so
   * it is first. Blue is at the bottom so it is last.
   */
  List<String> descendants();

  /**
   * Following the semantics of {@link #descendants()}, but starting from a different {@code source}
   * in the tree than this hierarchy's root. As with {@code descendants()}, the start of the tree is
   * included in the result list.
   */
  // TODO maybe generalize source as common type whether static or variable based
  // TODO maybe just return empty list instead of empty optional
  default Optional<List<String>> descendantsOf(String source) {
    return hierarchyOf(source).map(Hierarchy::descendants);
  }

  default Optional<List<String>> descendantsOf(Map<String, String> variables) {
    return hierarchyOf(variables).map(Hierarchy::descendants);
  }

  /**
   * Includes the {@code source} passed in as the first element, furthest ancestors last.
   */
  Optional<List<String>> ancestorsOf(String source);

  Optional<List<String>> ancestorsOf(Map<String, String> variables);

  /**
   * Finds a descendant source node and returns it as the root of a new {@link Hierarchy}.
   */
  Optional<Hierarchy> hierarchyOf(String source);

  Optional<Hierarchy> hierarchyOf(Map<String, String> variables);
}

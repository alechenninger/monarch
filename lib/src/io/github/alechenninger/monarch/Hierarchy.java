package io.github.alechenninger.monarch;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface Hierarchy {
  static Hierarchy fromStringListOrMap(Object object) {
    return new StaticHierarchy(StaticHierarchy.Node.fromStringListOrMap(object));
  }

  static Hierarchy fromInterpolatedList(List<String> sources,
      Map<String, List<String>> variableValues) {
    return null; // TODO
  }

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
  Optional<List<String>> descendantsOf(String source);

  Optional<List<String>> descendantsOf(Map<String, String> variables);

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

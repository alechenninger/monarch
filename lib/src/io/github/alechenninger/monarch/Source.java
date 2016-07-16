package io.github.alechenninger.monarch;

import java.util.List;

public interface Source {
  String path();

  /**
   * Returns this source and its ancestors in a single line up to a furthest root.
   */
  List<Source> lineage();

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
  List<Source> descendants();

  /**
   * A Source should be equal to another Source if they share the same path and hierarchy.
   */
  @Override
  boolean equals(Object other);

  /**
   * In the format, Source(${path()}).
   */
  @Override
  String toString();
}

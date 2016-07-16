package io.github.alechenninger.monarch;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

public interface SourceSpec {
  boolean isFor(Change change);

  default boolean isNotFor(Change change) {
    return !isFor(change);
  }

  Change toChange(Map<String, Object> set, Collection<String> remove);

  Optional<Source> findSource(Hierarchy hierarchy);

  default Optional<Change> findChange(Iterable<Change> changes) {
    return StreamSupport.stream(changes.spliterator(), false)
        .filter(this::isFor)
        .findFirst();
  }

  static SourceSpec byVariables(Map<String, String> variables) {
    return new SourceSpec() {
      @Override
      public boolean isFor(Change change) {
        return change.isFor(variables);
      }

      @Override
      public Change toChange(Map<String, Object> set, Collection<String> remove) {
        return Change.forVariables(variables, set, remove);
      }

      @Override
      public Optional<Source> findSource(Hierarchy hierarchy) {
        return hierarchy.sourceFor(variables);
      }
    };
  }

  static SourceSpec byPath(String path) {
    return new SourceSpec() {
      @Override
      public boolean isFor(Change change) {
        return change.isFor(path);
      }

      @Override
      public Change toChange(Map<String, Object> set, Collection<String> remove) {
        return Change.forPath(path, set, remove);
      }

      @Override
      public Optional<Source> findSource(Hierarchy hierarchy) {
        return hierarchy.sourceFor(path);
      }
    };
  }
}

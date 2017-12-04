package io.github.alechenninger.monarch;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public interface SourceSpec {

  Change toChange(Map<String, Object> set, Collection<String> remove);

  Optional<Source> findSource(Hierarchy hierarchy);

  Optional<Level> findLevel(Hierarchy hierarchy);

  Object toStringOrMap();

  @SuppressWarnings("unchecked")
  static List<SourceSpec> fromStringOrMap(Object object) {
    if (object instanceof String) {
      return BraceExpand.string((String) object).stream()
          .map(SourceSpec::byPath)
          .collect(Collectors.toList());
    }

    if (object instanceof Map) {
      return BraceExpand.keysAndValuesOf((Map<String, String>) object).stream()
          .map(SourceSpec::byVariables)
          .collect(Collectors.toList());
    }

    throw new IllegalArgumentException("Expected String or Map but got: " + object);
  }

  /**
   * @param expressions One or more "key=value" expressions or a single path.
   */
  static SourceSpec fromExpressions(List<String> expressions) {
    if (expressions.isEmpty()) {
      throw new IllegalArgumentException("Cannot parse a SourceSpec from an empty expressions " +
          "list. Provide at least one expression.");
    }

    if (expressions.size() == 1) {
      String expression = expressions.get(0);

      String[] keyValue = expression.split("=", 2);
      if (keyValue.length == 2) {
        return byVariables(Collections.singletonMap(keyValue[0], keyValue[1]));
      }

      return byPath(expression);
    }

    Map<String, String> variables = expressions.stream()
        .map(expression -> {
          String[] keyValue = expression.split("=", 2);

          if (keyValue.length != 2) {
            throw new IllegalArgumentException("Multiple source spec expressions provided but " +
                "one of them was not a key=value pair. Cannot parse '" + expression + "' in " +
                expressions);
          }

          return keyValue;
        })
        .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));

    return byVariables(variables);
  }

  static SourceSpec byVariables(Map<String, String> variables) {
    return new VariableSourceSpec(variables);
  }

  static SourceSpec byPath(String path) {
    return new PathSourceSpec(path);
  }

  class VariableSourceSpec implements SourceSpec {
    private final Map<String, String> variables;

    VariableSourceSpec(Map<String, String> variables) {
      this.variables = Collections.unmodifiableMap(new HashMap<>(variables));
    }

    @Override
    public Change toChange(Map<String, Object> set, Collection<String> remove) {
      return Change.forVariables(variables, set, remove);
    }

    @Override
    public Optional<Source> findSource(Hierarchy hierarchy) {
      return hierarchy.sourceFor(variables);
    }

    @Override
    public Optional<Level> findLevel(Hierarchy hierarchy) {
      return hierarchy.levelFor(variables);
    }

    @Override
    public Object toStringOrMap() {
      return variables;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      VariableSourceSpec that = (VariableSourceSpec) o;
      return Objects.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
      return Objects.hash(variables);
    }

    @Override
    public String toString() {
      return "VariableSourceSpec{" +
          "variables=" + variables +
          '}';
    }
  }

  class PathSourceSpec implements SourceSpec {
    private final String path;

    PathSourceSpec(String path) {
      this.path = path;
    }

    @Override
    public Change toChange(Map<String, Object> set, Collection<String> remove) {
      return Change.forPath(path, set, remove);
    }

    @Override
    public Optional<Source> findSource(Hierarchy hierarchy) {
      return hierarchy.sourceFor(path);
    }

    @Override
    public Optional<Level> findLevel(Hierarchy hierarchy) {
//      throw new UnsupportedOperationException("TODO");
      return hierarchy.levelFor(path);
    }

    @Override
    public Object toStringOrMap() {
      return path;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      PathSourceSpec that = (PathSourceSpec) o;
      return Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
      return Objects.hash(path);
    }

    @Override
    public String toString() {
      return "PathSourceSpec{" +
          "path='" + path + '\'' +
          '}';
    }
  }
}

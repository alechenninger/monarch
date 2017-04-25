package io.github.alechenninger.monarch;

import io.github.alechenninger.monarch.DynamicNode.RenderedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DynamicHierarchy implements Hierarchy {
  private final List<DynamicNode> nodes;
  private final Inventory inventory;

  // ----- These things are expensive to compute, so cache them. ------
  private Map<String, Assignments> cachedPaths = new HashMap<>();
  private Map<Map.Entry<String, String>, Assignment> cachedAssignments = new HashMap<>();
  private Map<Assignments, Source> cachedSources = new HashMap<>();

  /**
   * Value is RenderedSource or an exception. WTB union types. See {@link #sourceFor(RenderedNode,
   * int)}
   */
  private Map<RenderedSourceCacheKey, Object> cachedRenderedSources = new HashMap<>();
  private List<Source> cachedAll = null;

  private static final Logger log = LoggerFactory.getLogger(DynamicHierarchy.class);

  /**
   *
   * @param nodes Ancestors first, descendants last.
   * @param inventory For each variable, a List of known possible values. When a variable is found
   *                  but not supplied, all possible values are used if needed.
   */
  DynamicHierarchy(List<DynamicNode> nodes, Inventory inventory) {
    this.nodes = nodes;
    this.inventory = inventory;
  }

  @Override
  public Optional<Source> sourceFor(String source) {
    if (!cachedPaths.containsKey(source)) {

      List<Assignments> satisfyingVars = nodes.stream()
          .flatMap(node -> node.assignmentsFor(source, inventory, Assignments.none(inventory))
              .map(Stream::of).orElse(Stream.empty()))
          .collect(Collectors.toList());

      if (satisfyingVars.isEmpty()) {
        cachedPaths.put(source, null);
      }

      Assignments allVariables = new Assignments(inventory);
      // TODO: Instead, we can do this in loop over nodes
      for (Assignments assignments : satisfyingVars) {
        allVariables = allVariables.with(assignments);
      }

      cachedPaths.put(source, allVariables);
    }

    return Optional.ofNullable(cachedPaths.get(source)).flatMap(this::sourceFor);
  }

  @Override
  public Optional<Source> sourceFor(Map<String, String> assignments) {
    List<Assignment> assignmentList = new ArrayList<>(assignments.size());

    for (Map.Entry<String, String> entry : assignments.entrySet()) {
      if (!cachedAssignments.containsKey(entry)) {
        try {
          cachedAssignments.put(entry, inventory.assign(entry.getKey(), entry.getValue()));
        } catch (IllegalArgumentException e) {
          log.warn("Invalid assignments {}: {}", assignments, e.getLocalizedMessage());
          cachedAssignments.put(entry, null);
          return Optional.empty();
        }
      }

      Assignment assignment = cachedAssignments.get(entry);
      if (assignment == null) {
        // Must have been invalid.
        return Optional.empty();
      }
      assignmentList.add(assignment);
    }

    return sourceFor(inventory.assignAll(assignmentList));
  }

  @Override
  public Optional<Source> sourceFor(Assignments assignments) {
    if (cachedSources.containsKey(assignments)) {
      return Optional.ofNullable(cachedSources.get(assignments));
    }

    RenderedSource target = null;

    // First find target, if any.
    for (int i = 0; i < nodes.size(); i++) {
      DynamicNode node = nodes.get(i);
      if (assignments.assignsOnly(node.variables())) {
        try {
          target = sourceFor(node.renderOne(assignments), i);
          break;
        } catch (UnreachableSourceException ignored) {
          // TODO: error
          break;
        }
      }
    }

    if (target == null) {
      cachedSources.put(assignments, null);
      return Optional.empty();
    }

    cachedSources.put(assignments, target);
    return Optional.of(target);
  }

  @Override
  public List<Source> allSources() {
    if (cachedAll != null) {
      return cachedAll;
    }

    if (nodes.isEmpty()) {
      return cachedAll = Collections.emptyList();
    }

    List<Source> descendants = new ArrayList<>();

    for (int i = 0; i < nodes.size(); i++) {
      DynamicNode dynamicNode = nodes.get(i);

      for (RenderedNode rendered : dynamicNode.render(Assignments.none(inventory))) {
        try {
          descendants.add(sourceFor(rendered, i));
        } catch (UnreachableSourceException ignored) {
          // Fall through
        }
      }
    }

    return cachedAll = descendants;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DynamicHierarchy that = (DynamicHierarchy) o;
    return Objects.equals(nodes, that.nodes) &&
        Objects.equals(inventory, that.inventory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nodes, inventory);
  }

  @Override
  public String toString() {
    return "DynamicHierarchy{" +
        "nodes=" + nodes +
        ", inventory=" + inventory +
        '}';
  }

  /**
   * @throws UnreachableSourceException If render at this level is shadowed by a descendant with the
   * same path, then there is no node at this level; the path will always be inherited lower in the
   * hierarchy.
   */
  private RenderedSource sourceFor(RenderedNode render, int level) {
    RenderedSourceCacheKey key = new RenderedSourceCacheKey(render, level);

    if (cachedRenderedSources.containsKey(key)) {
      Object sourceOrException = cachedRenderedSources.get(key);
      if (sourceOrException instanceof RenderedSource) {
        return (RenderedSource) sourceOrException;
      }
      //noinspection ConstantConditions
      throw (UnreachableSourceException) sourceOrException;
    }

    try {
      RenderedSource source = new RenderedSource(render, level);
      cachedRenderedSources.put(key, source);
      return source;
    } catch (UnreachableSourceException e) {
      cachedRenderedSources.put(key, e);
      throw e;
    }
  }

  private static class RenderedSourceCacheKey {
    private final RenderedNode render;
    private final int level;
    private final int hash;

    private RenderedSourceCacheKey(RenderedNode render, int level) {
      this.render = render;
      this.level = level;
      hash = Objects.hash(render, level);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      RenderedSourceCacheKey renderedSourceCacheKey = (RenderedSourceCacheKey) o;
      return level == renderedSourceCacheKey.level &&
          Objects.equals(render, renderedSourceCacheKey.render);
    }

    @Override
    public int hashCode() {
      return hash;
    }
  }

  private class Root implements Source {

    @Override
    public String path() {
      return null;
    }

    @Override
    public List<Source> lineage() {
      return null;
    }

    @Override
    public List<Source> descendants() {
      return null;
    }

    @Override
    public boolean isTargetedBy(SourceSpec spec) {
      return false;
    }
  }

  private class RenderedSource implements Source {
    private final RenderedNode render;
    private final Assignments assignments;
    private final int level;

    private List<RenderedSource> lineage;
    private List<RenderedSource> descendants;

    private RenderedSource(RenderedNode render, int level) {
      this.render = render;
      this.assignments = inventory.assignAll(render.usedAssignments());
      this.level = level;

      Optional<RenderedSource> maybeConflict = renderedDescendants().stream()
          .skip(1)
          .filter(s -> s.path().equals(render.path()))
          .findAny();
      if (maybeConflict.isPresent()) {
        RenderedSource conflict = maybeConflict.get();
        log.warn("Repeat source path '{}' at nodes {} and descendant {}. " +
                "Ancestor is unreachable at this level in the hierarchy since it is shadowed by " +
                "a descendant.",
            render.path(), render.node(), conflict.node());
        throw new UnreachableSourceException(render, level, conflict);
      }
    }

    DynamicNode node() {
      return render.node();
    }

    @Override
    public String path() {
      return render.path();
    }

    @Override
    public List<Source> lineage() {
      if (lineage == null) {
        lineage = new ArrayList<>(level + 1 /* == how many in lineage + me */);
        lineage.add(this);

        for (int parentLevel = level - 1; parentLevel >= 0; parentLevel--) {
          DynamicNode node = nodes.get(parentLevel);
          if (assignments.assignsSupersetOf(node.variables())) {
            RenderedNode parentRender = node.renderOne(assignments);
            if (lineage == null) {
              lineage = new ArrayList<>();
            }
            try {
              lineage.add(sourceFor(parentRender, parentLevel));
            } catch (UnreachableSourceException ignored) {
              // Fall through
            }
          }
        }
      }

      return Collections.unmodifiableList(lineage);
    }

    @Override
    public List<Source> descendants() {
      return Collections.unmodifiableList(renderedDescendants());
    }

    List<RenderedSource> renderedDescendants() {
      if (descendants == null) {
        descendants = new ArrayList<>();
        descendants.add(this);

        for (int childLevel = level + 1; childLevel < nodes.size(); childLevel++) {
          List<RenderedNode> childRenders = nodes.get(childLevel).render(assignments);

          for (RenderedNode childRender : childRenders) {
            Assignments childAssigns = inventory.assignAll(childRender.usedAssignments());
            if (assignments.isEmpty() || childAssigns.containsAll(assignments)) {
              if (descendants == null ) {
                descendants = new ArrayList<>(childRenders.size());
              }

              try {
                descendants.add(sourceFor(childRender, childLevel));
              } catch (UnreachableSourceException ignored) {
                // Fall through
              }
            }
          }
        }
      }

      return descendants;
    }

    @Override
    public boolean isTargetedBy(SourceSpec spec) {
      return spec.findSource(DynamicHierarchy.this)
          .map(found -> found.path().equals(path()))
          .orElse(false);
    }

    @Override
    public String toString() {
      return "RenderedSource{" +
          "node=" + render.node() +
          ", path=" + render.path() +
          '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      RenderedSource that = (RenderedSource) o;
      return level == that.level &&
          Objects.equals(render, that.render);
    }

    @Override
    public int hashCode() {
      return Objects.hash(render, level);
    }
  }

  private static class UnreachableSourceException extends RuntimeException {
    private final RenderedNode render;
    private final int level;
    private final RenderedSource conflict;

    UnreachableSourceException(RenderedNode render, int level, RenderedSource conflict) {
      this.render = render;
      this.level = level;
      this.conflict = conflict;
    }

    public RenderedNode render() {
      return render;
    }

    public int level() {
      return level;
    }

    public RenderedSource conflict() {
      return conflict;
    }
  }
}

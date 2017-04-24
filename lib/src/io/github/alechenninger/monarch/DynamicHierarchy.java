package io.github.alechenninger.monarch;

import io.github.alechenninger.monarch.DynamicNode.RenderedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DynamicHierarchy implements Hierarchy {
  private final List<DynamicNode> nodes;
  private final Inventory inventory;

  private Map<String, Assignments> cachedPaths = new HashMap<>();
  private Map<Map.Entry<String, String>, Assignment> cachedAssignments = new HashMap<>();
  private Map<Assignments, Source> cachedSources = new HashMap<>();
  private Map<SourceCacheKey, Object> cachedRenderedSources = new HashMap<>();
  private List<Source> all = null;

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
          target = RenderedSource.newOrCached(node.renderOne(assignments), i, this);
          break;
        } catch (RenderedSource.UnreachableSourceException ignored) {
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
    if (all != null) {
      return all;
    }

    if (nodes.isEmpty()) {
      return all = Collections.emptyList();
    }

    List<Source> descendants = new ArrayList<>();

    for (int i = 0; i < nodes.size(); i++) {
      DynamicNode dynamicNode = nodes.get(i);

      for (RenderedNode rendered : dynamicNode.render(Assignments.none(inventory))) {
        try {
          descendants.add(RenderedSource.newOrCached(rendered, i, this));
        } catch (RenderedSource.UnreachableSourceException ignored) {
          // Fall through
        }
      }
    }

    return all = descendants;
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

  static class SourceCacheKey {
    final RenderedNode render;
    final int level;

    public SourceCacheKey(RenderedNode render, int level) {
      this.render = render;
      this.level = level;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      SourceCacheKey sourceCacheKey = (SourceCacheKey) o;
      return level == sourceCacheKey.level &&
          Objects.equals(render, sourceCacheKey.render);
    }

    @Override
    public int hashCode() {
      return Objects.hash(render, level);
    }
  }

  static class RenderedSource implements Source {
    private final RenderedNode render;
    private final Assignments assignments;
    private final int level;
    private final DynamicHierarchy hierarchy;

    private List<RenderedSource> lineage;
    private List<RenderedSource> descendants;

    static RenderedSource newOrCached(RenderedNode render, int level, DynamicHierarchy hierarchy) {
      SourceCacheKey key = new SourceCacheKey(render, level);
      if (hierarchy.cachedRenderedSources.containsKey(key)) {
        Object sourceOrException = hierarchy.cachedRenderedSources.get(key);
        if (sourceOrException instanceof RenderedSource) {
          return (RenderedSource) sourceOrException;
        }
        throw (RuntimeException) sourceOrException;
      }

      try {
        RenderedSource source = new RenderedSource(render, level, hierarchy);
        hierarchy.cachedRenderedSources.put(key, source);
        return source;
      } catch (UnreachableSourceException e) {
        hierarchy.cachedRenderedSources.put(key, e);
        throw e;
      }
    }

    private static class UnreachableSourceException extends RuntimeException {
      private final RenderedNode render;
      private final int level;
      private final RenderedSource conflict;

      public UnreachableSourceException(RenderedNode render, int level, RenderedSource conflict) {
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

    private RenderedSource(RenderedNode render, int level, DynamicHierarchy hierarchy) {
      this.render = render;
      this.assignments = hierarchy.inventory.assignAll(render.usedAssignments());
      this.level = level;
      this.hierarchy = hierarchy;

      Optional<RenderedSource> maybeConflict = descendantsRendered().stream()
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

    public DynamicNode node() {
      return render.node();
    }

    @Override
    public String path() {
      return render.path();
    }

    @Override
    public List<Source> lineage() {
      if (lineage == null) {
        lineage = new ArrayList<>();
        lineage.add(this);

        for (int parentLevel = level - 1; parentLevel >= 0; parentLevel--) {
          DynamicNode node = hierarchy.nodes.get(parentLevel);
          if (assignments.assignsSupersetOf(node.variables())) {
            RenderedNode parentRender = node.renderOne(assignments);
            if (lineage == null) {
              lineage = new ArrayList<>();
            }
            try {
              lineage.add(RenderedSource.newOrCached(parentRender, parentLevel, hierarchy));
            } catch (UnreachableSourceException ignored) {
              // Fall through
            }
          }
        }
      }

      return Collections.unmodifiableList(lineage);
    }

    public List<RenderedSource> descendantsRendered() {
      if (descendants == null) {
        descendants = new ArrayList<>();
        descendants.add(this);

        for (int childLevel = level + 1; childLevel < hierarchy.nodes.size(); childLevel++) {
          List<RenderedNode> childRenders = hierarchy.nodes.get(childLevel).render(assignments);

          for (RenderedNode childRender : childRenders) {
            Assignments childAssigns = hierarchy.inventory.assignAll(childRender.usedAssignments());
            if (assignments.isEmpty() || childAssigns.containsAll(assignments)) {
              if (descendants == null ) {
                descendants = new ArrayList<>(childRenders.size());
              }
              try {
                descendants.add(RenderedSource.newOrCached(childRender, childLevel, hierarchy));
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
    public List<Source> descendants() {
      return Collections.unmodifiableList(descendantsRendered());
    }

    @Override
    public boolean isTargetedBy(SourceSpec spec) {
      return spec.findSource(hierarchy)
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
  }
}

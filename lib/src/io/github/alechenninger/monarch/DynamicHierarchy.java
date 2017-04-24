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
  private Map<SourceCacheKey, RenderedSource> cachedRenderedSources = new HashMap<>();
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
        }
      }
    }

    if (target == null) {
      cachedSources.put(assignments, null);
      return Optional.empty();
    }

    Level current = null;
    DynamicSource targetSource = null;

    // Build hierarchy from bottom up.
//    for (DynamicNode node : new ListReversed<>(nodes)) {
//      current = current == null ? new Level() : current.parent();
//
//      // Adding descendants of a target has different logic than ancestors.
//      // While target source is null, it means we haven't found it yet, so we're still adding
//      // descendants.
//      if (targetSource == null) {
//        List<RenderedNode> renders = node.render(assignments);
//
//        for (RenderedNode render : renders) {
//          Assignments renderAssigns = inventory.assignAll(render.usedAssignments());
//          if (assignments.isEmpty() || renderAssigns.containsAll(assignments)) {
//            Optional<DynamicSource> maybeSource = current.addMember(render, renderAssigns);
//            if (maybeSource.isPresent()) {
//              DynamicSource source = maybeSource.get();
//              if (source.path().equals(target.path())) {
//                if (source.render.equals(target)) {
//                  targetSource = source;
//                } else {
//                  log.error("Found source for assignments, but it is shadowed by a descendant " +
//                          "with a duplicate path. Impossible to refer to desired position in " +
//                          "hierarchy. Path was '{}'. Desired target for assignments {} was at " +
//                          "node {}. Shadowed by same path at descendant {} from assignments {}.",
//                      target.path(), assignments.toMap(), target.node(), source.node(),
//                      source.assignments.toMap());
//                }
//              }
//            }
//          }
//        }
//      } else {
//        if (assignments.assignsSupersetOf(node.variables())) {
//          RenderedNode render = node.renderOne(assignments);
//          current.addMember(render, assignments);
//        }
//      }
//    }

    cachedSources.put(assignments, target);
    return Optional.ofNullable(target);
  }

  @Override
  public List<Source> descendants() {
    if (all != null) {
      return all;
    }

    if (nodes.isEmpty()) {
      return all = Collections.emptyList();
    }

    Level current = null;

    for (int i = nodes.size() - 1; i >= 0; i--) {
      DynamicNode dynamicNode = nodes.get(i);
      Level level = current == null ? new Level() : current.parent();

      for (RenderedNode rendered : dynamicNode.render(Assignments.none(inventory))) {
        Assignments assignments = inventory.assignAll(rendered.usedAssignments());
        level.addMember(rendered, assignments);
      }

      if (!level.isEmpty()) {
        current = level;
      }
    }

    if (current == null) {
      return all = Collections.emptyList();
    }

    return all = current.descendants().stream()
        .flatMap(l -> l.members().stream())
        .collect(Collectors.toList());
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
        RenderedSource source = hierarchy.cachedRenderedSources.get(key);
        if (source == null) {
          throw new UnreachableSourceException(render, level);
        }
        return source;
      }

      try {
        RenderedSource source = new RenderedSource(render, level, hierarchy);
        hierarchy.cachedRenderedSources.put(key, source);
        return source;
      } catch (UnreachableSourceException e) {
        hierarchy.cachedRenderedSources.put(key, null);
        throw e;
      }
    }

    private static class UnreachableSourceException extends RuntimeException {
      public UnreachableSourceException(RenderedNode render, int level) {
      }
    }

    private RenderedSource(RenderedNode render, int level, DynamicHierarchy hierarchy) {
      this.render = render;
      this.assignments = hierarchy.inventory.assignAll(render.usedAssignments());
      this.level = level;
      this.hierarchy = hierarchy;

      if (descendants().stream().skip(1).anyMatch(d -> render.path().equals(d.path()))) {
        throw new UnreachableSourceException(render, level);
      }
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
              // TODO: warn
            }
          }
        }
      }

      return Collections.unmodifiableList(lineage);
    }

    @Override
    public List<Source> descendants() {
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
                // TODO: warn
              }
            }
          }
        }
      }

      return Collections.unmodifiableList(descendants);
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

  class DynamicSource implements Source {
    private final Assignments assignments;
    private final RenderedNode render;
    private final Level level;

    DynamicSource(Assignments assignments, RenderedNode render, Level level) {
      this.assignments = assignments;
      this.render = render;
      this.level = level;
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
      List<Source> lineage = new ArrayList<>();
      lineage.add(this);
      DynamicSource ancestor = parent();
      while (ancestor != null) {
        lineage.add(ancestor);
        ancestor = ancestor.parent();
      }
      return lineage;
    }

    @Override
    public List<Source> descendants() {
      return level.descendants()
          .stream()
          .flatMap(l -> l.members().stream())
          .collect(Collectors.toList());
    }

    @Override
    public boolean isTargetedBy(SourceSpec spec) {
      return spec.findSource(DynamicHierarchy.this)
          .map(found -> found.path().equals(path()))
          .orElse(false);
    }

    @Override
    public String toString() {
      return "DynamicSource{" +
          "node=" + render.node() +
          ", rendered=" + render.path() +
          '}';
    }

    // TODO: equals, hashCode

    private DynamicSource parent() {
      for (Level ancestorLevel : level.ancestors()) {
        for (DynamicSource parent : ancestorLevel.parent()) {
          if (assignments.containsAll(parent.assignments)) {
            return parent;
          }
        }
      }

      return null;
    }
  }

  class Level implements Iterable<DynamicSource> {
    final List<DynamicSource> members = new ArrayList<>();
    final Level child;

    Level parent;

    Level() {
      this(null);
    }

    private Level(Level child) {
      this.child = child;
      if (child != null) child.parent = this;
    }

    Optional<DynamicSource> addMember(RenderedNode render, Assignments assignments) {
      if (!parent().isEmpty()) {
        // TODO: Handle this case. Would require moving members around if added a repeat at a
        // lower level.
        throw new IllegalStateException("Cannot add members to level once a level has a " +
            "non-empty parent.");
      }

      DynamicSource newMember = new DynamicSource(assignments, render, this);
      Optional<DynamicSource> match = descendants().stream()
          .flatMap(l -> l.members().stream())
          .filter(s -> s.path().equals(newMember.path()))
          .findAny();

      if (match.isPresent()) {
        log.warn("Repeat source path '{}' at nodes {} and descendant {}. Ignoring ancestor node.",
            newMember.path(), newMember.node(), match.get().node());
        return Optional.empty();
      }

      members.add(newMember);
      return Optional.of(newMember);
    }

    List<DynamicSource> members() {
      return Collections.unmodifiableList(members);
    }

    List<Level> ancestors() {
      List<Level> ancestors = new ArrayList<>();
      ancestors.add(this);
      Level ancestor = this.parent;
      while (ancestor != null && !ancestor.isEmpty()) {
        ancestors.add(ancestor);
        ancestor = ancestor.parent;
      }
      return ancestors;
    }

    List<Level> descendants() {
      List<Level> descendants = new ArrayList<>();
      descendants.add(this);
      Level child = this.child;
      while (child != null) {
        descendants.add(child);
        child = child.child;
      }
      return descendants;
    }

    Level parent() {
      if (members.isEmpty()) {
        return this;
      }

      if (parent != null) {
        return parent;
      }

      return new Level(this);
    }

    boolean isEmpty() {
      return members.isEmpty();
    }

    @Override
    public Iterator<DynamicSource> iterator() {
      return members().iterator();
    }

    // TODO: Better to string
    // TODO: equals, hashCode
    @Override
    public String toString() {
      return "Level{" +
          "members=" + members +
          '}';
    }
  }
}

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

  private Map<Assignments, Source> cachedSources = new HashMap<>();

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
    return variablesFor(source).flatMap(this::sourceFor);
  }

  @Override
  public Optional<Source> sourceFor(Map<String, String> assignments) {
    List<Assignment> assignmentList = assignments.entrySet()
        .stream()
        .map(entry -> inventory.assign(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
    return sourceFor(inventory.assignAll(assignmentList));
  }

  @Override
  public Optional<Source> sourceFor(Assignments assignments) {
    if (cachedSources.containsKey(assignments)) {
      return Optional.of(cachedSources.get(assignments));
    }

    RenderedNode target = null;

    // First find target, if any.
    for (DynamicNode node : nodes) {
      if (assignments.assignsOnly(node.variables())) {
        target = node.renderOne(assignments);
        break;
      }
    }

    if (target == null) {
      return Optional.empty();
    }

    Level current = null;
    DynamicSource targetSource = null;

    // Build hierarchy from bottom up.
    for (DynamicNode node : new ListReversed<>(nodes)) {
      current = current == null ? new Level() : current.parent();

      // Adding descendants of a target has different logic than ancestors.
      // While target source is null, it means we haven't found it yet, so we're still adding
      // descendants.
      if (targetSource == null) {
        List<RenderedNode> renders = node.render(assignments);

        for (RenderedNode render : renders) {
          Assignments renderAssigns = inventory.assignAll(render.usedAssignments());
          if (assignments.isEmpty() || renderAssigns.containsAll(assignments)) {
            Optional<DynamicSource> maybeSource = current.addMember(render, renderAssigns);
            if (maybeSource.isPresent()) {
              DynamicSource source = maybeSource.get();
              if (source.path().equals(target.path())) {
                if (!source.render.equals(target)) {
                  throw new IllegalStateException("Found source for assignments, but it is " +
                      "shadowed by a descendant with a duplicate path. Impossible to refer to " +
                      "desired position in hierarchy. Path was '" + target.path() + "'. Desired " +
                      "target for assignments " + assignments.toMap() + " was at node " +
                      target.node() + ". Shadowed by same path at descendant " + source.node() +
                      ".");
                }

                targetSource = source;
              }
            }
          }
        }
      } else {
        if (assignments.assignsSupersetOf(node.variables())) {
          RenderedNode render = node.renderOne(assignments);
          current.addMember(render, assignments);
        }
      }
    }

    if (targetSource == null) {
      throw new IllegalStateException(
          "Bug! Found a target but not the source? Doesn't make sense.");
    }

    cachedSources.put(assignments, targetSource);

    return Optional.of(targetSource);
  }

  @Override
  public List<Source> descendants() {
    if (nodes.isEmpty()) {
      return Collections.emptyList();
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
      return Collections.emptyList();
    }

    return current.descendants().stream()
        .flatMap(l -> l.members().stream())
        .collect(Collectors.toList());
  }

  private Optional<Assignments> variablesFor(String source) {
    List<Assignments> satisfyingVars = nodes.stream()
        .flatMap(s -> s.assignmentsFor(source, inventory, Assignments.none(inventory))
            .map(Stream::of).orElse(Stream.empty()))
        .collect(Collectors.toList());

    if (satisfyingVars.isEmpty()) {
      return Optional.empty();
    }

    Assignments allVariables = new Assignments(inventory);
    // TODO: Instead, we can do this in loop over nodes
    for (Assignments assignments : satisfyingVars) {
      allVariables = allVariables.with(assignments);
    }
    return Optional.of(allVariables);
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

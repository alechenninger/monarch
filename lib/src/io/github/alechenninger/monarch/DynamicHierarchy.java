package io.github.alechenninger.monarch;

import io.github.alechenninger.monarch.DynamicNode.RenderedNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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


    for (int iTarget = 0; iTarget < nodes.size(); iTarget++) {
      DynamicNode targetNode = nodes.get(iTarget);

      if (assignments.assignsOnly(targetNode.variables())) {
        RenderedNode target = targetNode.renderOne(assignments);
        Level current = null;
        DynamicSource targetSource = null;

        for (int i = nodes.size() - 1; i >= 0; i--) {
          DynamicNode node = nodes.get(i);

          if (i >= iTarget) {
            Level next = null;
            List<RenderedNode> renders = node.render(assignments);

            for (RenderedNode render : renders) {
              Assignments renderAssigns = inventory.assignAll(render.usedAssignments());
              if (assignments.isEmpty() || renderAssigns.containsAll(assignments)) {
                if (current != null && current.descendants().stream()
                    .flatMap(l -> l.members().stream())
                    .map(Source::path)
                    .anyMatch(render.path()::equals)) {
                  // TODO: add details
                  log.warn("Repeat source ignored {}", render.path());
                } else {
                  if (next == null) {
                    current = next = new Level(current);
                  }
                  DynamicSource source = current.add(render, renderAssigns);
                  if (source.path().equals(target.path())) {
                    targetSource = source;
                  }
                }
              }
            }
          } else {
            if (assignments.assignsSupersetOf(node.variables())) {
              RenderedNode render = node.renderOne(assignments);
              if (current != null && current.descendants().stream()
                  .flatMap(l -> l.members().stream())
                  .map(Source::path)
                  .anyMatch(render.path()::equals)) {
                // TODO: add details
                log.warn("Repeat source ignored {}", render.path());
              } else {
                current = new Level(current);
                DynamicSource source = current.add(render, assignments);
                if (source.path().equals(target.path())) {
                  targetSource = source;
                }
              }
            }
          }
        }

        return Optional.of(targetSource);

        //return Optional.of(new RenderedSource(assignments, nodes, inventory, iTarget));
      }
    }

    return Optional.empty();
  }

  @Override
  public List<Source> descendants() {
    if (nodes.isEmpty()) {
      return Collections.emptyList();
    }

    List<Source> descendants = new ArrayList<>();

    for (int i = 0; i < nodes.size(); i++) {
      DynamicNode dynamicNode = nodes.get(i);
      for (RenderedNode rendered : dynamicNode.render(Assignments.none(inventory))) {
        Assignments variables = inventory.assignAll(rendered.usedAssignments());
        descendants.add(new RenderedSource(variables, nodes, inventory, i, rendered));
      }
    }

    return descendants;
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

  private static class RenderedSource extends AbstractSource {
    private final Assignments assignments;
    private final List<DynamicNode> nodes;
    private final Inventory inventory;
    private final int index;
    private final RenderedNode rendered;

    private List<Source> lineage;
    private List<Source> descendants;

    private RenderedSource(Assignments assignments, List<DynamicNode> nodes,
        Inventory inventory, int index) {
      this.assignments = assignments;
      this.nodes = nodes;
      this.inventory = inventory;
      this.index = index;

      DynamicNode dynamicNode = nodes.get(index);
      List<RenderedNode> renders = dynamicNode.render(this.assignments);

      if (renders.size() != 1) {
        throw new IllegalArgumentException("Expected source with all variables provided to " +
            "produce a single source.");
      }

      this.rendered = renders.get(0);
    }

    private RenderedSource(Assignments assignments, List<DynamicNode> nodes,
        Inventory inventory, int index, RenderedNode rendered) {
      this.assignments = assignments;
      this.nodes = nodes;
      this.inventory = inventory;
      this.index = index;
      this.rendered = rendered;
      // TODO if (!variables.containsAll(rendered.usedAssignments()) throw
    }

    @Override
    public String path() {
      return rendered.path();
    }

    @Override
    public List<Source> lineage() {
      if (lineage != null) {
        return lineage;
      }

      lineage = new ArrayList<>(index);
      lineage.add(this);

      for (int i = index - 1; i >= 0; i--) {
        DynamicNode node = nodes.get(i);
        if (assignments.assignsSupersetOf(node.variables())) {
          RenderedSource newAncestor = new RenderedSource(assignments, nodes, inventory, i);

          // Don't bother adding this ancestor if one with the same path is already in lineage.
          // It will have been used already and no source can't have itself as an ancestor.
          for (Source ancestor : lineage) {
            if (ancestor.path().equals(newAncestor.path())) {
              log.warn("Repeat source path at {} and ancestor {} using assignments {}. " +
                      "Ignoring ancestor.",
                  ancestor, newAncestor, newAncestor.rendered.usedAssignments());
              newAncestor = null;
              break;
            }
          }

          if (newAncestor != null) {
            lineage.add(newAncestor);
          }
        }
      }

      return lineage;
    }

    @Override
    public List<Source> descendants() {
      if (descendants != null) {
        return descendants;
      }

      descendants = new ArrayList<>();
      descendants.add(this);

      for (int i = index + 1; i < nodes.size(); i++) {
        DynamicNode node = nodes.get(i);
        List<RenderedNode> renders = node.render(assignments);
        for (RenderedNode render : renders) {
          Assignments renderAssigns = inventory.assignAll(render.usedAssignments());
          if (assignments.isEmpty() || renderAssigns.containsAll(assignments)) {
            Assignments descendantAssigns = assignments.with(renderAssigns);
            RenderedSource newDescendant =
                new RenderedSource(descendantAssigns, nodes, inventory, i, render);

            // Exclude source if descendants already contains a source with this path.
            for (Iterator<Source> it = descendants.iterator(); it.hasNext();) {
              Source descendant = it.next();

              if (descendant.path().equals(render.path())) {
                log.warn("Repeat source path at {} and descendant {} using assignments {}. " +
                        "Using descendant node instead.",
                    descendant, newDescendant, render.usedAssignments());
                it.remove();
                break;
              }
            }

            descendants.add(newDescendant);
          }
        }
      }

      return descendants;
    }

    @Override
    public boolean isTargetedBy(SourceSpec spec) {
      return spec.findSource(new DynamicHierarchy(nodes, inventory))
          .map(this::equals)
          .orElse(false);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      RenderedSource that = (RenderedSource) o;
      // Assignments comparison deliberately omitted. We are not comparing the entire tree is equal,
      // just this source within any potential tree.
      return index == that.index &&
          Objects.equals(nodes, that.nodes) &&
          Objects.equals(inventory, that.inventory) &&
          Objects.equals(rendered, that.rendered);
    }

    @Override
    public int hashCode() {
      return Objects.hash(nodes, inventory, index, rendered);
    }

    @Override
    public String toString() {
      return "RenderedSource{" +
          "node=" + nodes.get(index) + ", " +
          "path='" + path() + '\'' +
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

    private DynamicSource parent() {
      // TODO: should make sure this could only ever be one or null
      for (Level ancestorLevel : level.ancestors()) {
        for (DynamicSource parent : ancestorLevel.parent().orElse(new Level())) {
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

    Level(Level child) {
      this.child = child;
      if (child != null) child.parent = this;
    }

    DynamicSource add(RenderedNode render, Assignments assignments) {
      DynamicSource source = new DynamicSource(assignments, render, this);
      members.add(source);
      return source;
    }

    List<DynamicSource> members() {
      return Collections.unmodifiableList(members);
    }

    List<Level> ancestors() {
      List<Level> ancestors = new ArrayList<>();
      ancestors.add(this);
      Level ancestor = this.parent;
      while (ancestor != null) {
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

    public Optional<Level> parent() {
      return Optional.ofNullable(parent);
    }

    public Optional<Level> child() {
      return Optional.ofNullable(child);
    }

    @Override
    public Iterator<DynamicSource> iterator() {
      return members().iterator();
    }
  }
}

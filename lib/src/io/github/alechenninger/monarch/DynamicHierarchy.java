package io.github.alechenninger.monarch;

import io.github.alechenninger.monarch.DynamicNode.RenderedNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DynamicHierarchy implements Hierarchy {
  private final List<DynamicNode> nodes;
  private final Inventory inventory;

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
    return sourceFor(inventory.assignAll(assignments.entrySet().stream()
        .map(entry -> inventory.assign(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList())));
  }

  @Override
  public Optional<Source> sourceFor(Assignments assignments) {
    RenderedSource match = null;

    for (int i = 0; i < nodes.size(); i++) {
      DynamicNode node = nodes.get(i);
      if (assignments.assignsOnly(node.variables())) {
        match = new RenderedSource(assignments, nodes, inventory, i);
        break;
      }
    }

    // If we have a match, it's possible that with its implications there is a source higher in the
    // hierarchy that has the same path. If this is the case, then we can't target the exact source
    // since that level doesn't really exist: it is overridden by a node higher in the hierarchy
    // that lives at the same source.
    return Optional.ofNullable(match)
        .flatMap(s -> ListReversed.stream(s.lineage())
            .filter(a -> a.path().equals(s.path()))
            .findFirst());
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
      List<Source> lineage = new ArrayList<>(index);

      for (int i = index; i >= 0; i--) {
        if (assignments.assignsSupersetOf(nodes.get(i).variables())) {
          RenderedSource newAncestor = new RenderedSource(assignments, nodes, inventory, i);
          // Replace an ancestor with the same path, since this ancestor will always override it.
          lineage.removeIf(ancestor -> ancestor.path().equals(newAncestor.path()));
          lineage.add(newAncestor);
        }
      }

      return lineage;
    }

    @Override
    public List<Source> descendants() {
      List<Source> descendants = new ArrayList<>();

      descendants.add(this);

      for (int i = index + 1; i < nodes.size(); i++) {
        DynamicNode dynamicNode = nodes.get(i);
        List<RenderedNode> renders = dynamicNode.render(assignments);
        for (RenderedNode render : renders) {
          Assignments renderAssigns = inventory.assignAll(render.usedAssignments());
          if (assignments.isEmpty() || renderAssigns.containsAll(assignments)) {
            // Exclude source if descendants already contains a source with this path.
            if (descendants.stream().map(Source::path).anyMatch(render.path()::equals)) {
              continue;
            }

            Assignments descendantAssigns = assignments.with(renderAssigns);
            descendants.add(new RenderedSource(descendantAssigns, nodes, inventory, i, render));
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
  }
}

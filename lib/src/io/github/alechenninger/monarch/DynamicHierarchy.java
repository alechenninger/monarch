package io.github.alechenninger.monarch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO consider supporting implied args or arg groups or something of the sort
// Ex: we know qa.foo.com has "environment" of "qa", so if you target host=qa.foo.com you should see
// environment=qa in ancestry.
public class DynamicHierarchy implements Hierarchy {
  private final List<DynamicNode> nodes;
  private final Inventory inventory;

  /**
   *
   * @param nodes Ancestors first, descendants last.
   * @param inventory For each variable, a List of known possible values. When a variable is found
   *                  but not supplied, all possible values are used if needed.
   */
  public DynamicHierarchy(List<DynamicNode> nodes, Inventory inventory) {
    this.nodes = nodes;
    this.inventory = inventory;
  }

  @Override
  public Optional<Source> sourceFor(String source) {
    return variablesFor(source).flatMap(this::sourceFor);
  }

  @Override
  public Optional<Source> sourceFor(Map<String, String> assignments) {
    return sourceFor(inventory.newAssignments().with(assignments.entrySet().stream()
        .map(entry -> inventory.assign(entry.getKey(), entry.getValue()))
        .collect(Collectors.toSet())));
  }

  @Override
  public Optional<Source> sourceFor(Assignments assignments) {
    for (int i = 0; i < nodes.size(); i++) {
      DynamicNode source = nodes.get(i);

      if (assignments.assignsOnly(source.variables())) {
        return Optional.of(new RenderedSource(assignments, nodes, inventory, i));
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
      for (DynamicNode.RenderedNode rendered :
          dynamicNode.render(Assignments.none(inventory), inventory)) {
        Assignments variables = rendered.variablesUsed();
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
    private final DynamicNode.RenderedNode rendered;

    private RenderedSource(Assignments assignments, List<DynamicNode> nodes,
        Inventory inventory, int index) {
      this.assignments = assignments;
      this.nodes = nodes;
      this.inventory = inventory;
      this.index = index;

      DynamicNode dynamicNode = nodes.get(index);
      List<DynamicNode.RenderedNode> renders = dynamicNode.render(this.assignments, inventory);

      if (renders.size() != 1) {
        throw new IllegalArgumentException("Expected source with all variables provided to " +
            "produce a single source.");
      }

      this.rendered = renders.get(0);
    }

    private RenderedSource(Assignments assignments, List<DynamicNode> nodes,
        Inventory inventory, int index, DynamicNode.RenderedNode rendered) {
      this.assignments = assignments;
      this.nodes = nodes;
      this.inventory = inventory;
      this.index = index;
      this.rendered = rendered;
      // TODO if (!variables.containsAll(rendered.variablesUsed()) throw
    }

    @Override
    public String path() {
      return rendered.path();
    }

    @Override
    public List<Source> lineage() {
      List<Source> lineage = new ArrayList<>(index);

      for (int i = index; i >= 0; i--) {
        // if (nodes.get(i).renderOne(assignments).ifPresent(....)
        if (assignments.assignsSupersetOf(nodes.get(i).variables())) {
          lineage.add(new RenderedSource(assignments, nodes, inventory, i));
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
        List<DynamicNode.RenderedNode> renders = dynamicNode.render(assignments, inventory);
        for (DynamicNode.RenderedNode render : renders) {
          Assignments renderAssigns = render.variablesUsed();
          // TODO: Figure out this condition
          // Want the assignments used to either use one of our assigns or implications as a variable
          // Or one of the additional assigns to imply one of our assigns
          //if (assignments.containsSomeOf(renderAssigns)) {
            descendants.add(new RenderedSource(renderAssigns, nodes, inventory, i, render));
          //}
        }

        // is a test of specificity
        // dynamicNode.mayDescend(variables)
//        if (assignments.assignsSubsetOf(dynamicNode.variables())) {
//          List<DynamicNode.RenderedNode> renders = dynamicNode.render(assignments, inventory);
//
//          for (DynamicNode.RenderedNode render : renders) {
//            Assignments descendantAssigns = render.variablesUsed();
//            // Necessary? descendantAssigns.addAll(assignments); I don't think so
//            descendants.add(new RenderedSource(descendantAssigns, nodes, inventory, i, render));
//          }
//        } else {
//          // See if any variables used in source have implied values that match what are defined
//          // So if host/foo.com implies environment=prod
//          // And we have environment=prod
//          // Which means that we know this would be a descendant
//          // (in other words, host/foo.com has environment=prod in its lineage)
//          // What about other variables?
//          // host/{area}/{host}
//          // host/vary/foo.com with implied environment=prod area=vary
//          // Given environment=prod
//          // For area, none of the implied are included (it could in some scenarios)
//          // For host, foo.com implies environment=prod
//          // So what would be a descendant?
//          // host/vary/foo.com what about other areas (if area=vary wasn't implied)?
//          // host/phx2/foo.com if foo.com implies prod, and we don't have an area,
//          // then this is fair game. So we'd expect both.
//          // It's basically as if host=foo.com was defined.
//
//          // Go through each unassigned variable, find potentials which have implied values matching
//          // defined
//          // Use them as variables.
//          // If multiple potentials, we need to treat each case.
//
//          // variable.assignmentsThatImply(var, value);
//          // assignment
//
//
//          // so we have some variables that arent assigned
//          // do any of their possible values have implications that are assigned?
//          // fqdn has possible values prod.foo.com, stage.foo.com, bar.com
//          // prod.foo.com implies prod
//          // stage.foo.com implies stage
//          // bar.com doesnt imply any env ... so it doesnt conflict, but shouldnt include it still
//          dynamicNode.variables()
//          if (!potentialAssignment.implications().isEmpty() &&
//              assignments.containsSubsetOf(potentialAssignment.implications()) &&
//              !assignments.conflictsWith(potentialAssignment.implications())) {
//
//          }
//        }
      }

      return descendants;
    }

    @Override
    public boolean isTargetedBy(SourceSpec spec) {
      return spec.findSource(new DynamicHierarchy(nodes, inventory))
          .map(this::equals)
          .orElse(false);
    }
  }
}

package io.github.alechenninger.monarch;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO consider supporting implied args or arg groups or something of the sort
// Ex: we know qa.foo.com has "environment" of "qa", so if you target host=qa.foo.com you should see
// environment=qa in ancestry.
public class DynamicHierarchy implements Hierarchy {
  private final List<DynamicNode> sources;
  private final Map<String, List<Potential>> potentials;

  /**
   *
   * @param sources Ancestors first, descendants last.
   * @param potentials For each variable, a List of known possible values. When a variable is found
   *                   but not supplied, all possible values are used if needed.
   */
  public DynamicHierarchy(List<DynamicNode> sources, Map<String, List<Potential>> potentials) {
    this.sources = sources;
    this.potentials = potentials;
  }

  @Override
  public Optional<Source> sourceFor(String source) {
    return variablesFor(source).flatMap(this::sourceFor);
  }

  @Override
  public Optional<Source> sourceFor(Map<String, String> variables) {
    Set<String> variableKeys = variables.keySet();

    for (int i = 0; i < sources.size(); i++) {
      DynamicNode source = sources.get(i);
      List<String> sourceVariables = source.variables();

      if (sourceVariables.containsAll(variableKeys) &&
          variableKeys.containsAll(sourceVariables)) {
        // TODO: consider first class type for this constructs
        // Variables vars = new Variables(variables, potentials);
        Map<String, String> variablesPlusImplied = withImpliedVariables(variables);
        // TODO: also refine potentials based on variables
        return Optional.of(new RenderedSource(variablesPlusImplied, sources, potentials, i));
      }
    }

    return Optional.empty();
  }

  @Override
  public List<Source> descendants() {
    if (sources.isEmpty()) {
      return Collections.emptyList();
    }

    List<Source> descendants = new ArrayList<>();

    for (int i = 0; i < sources.size(); i++) {
      DynamicNode dynamicNode = sources.get(i);
      for (DynamicNode.RenderedNode rendered :
          dynamicNode.render(Collections.emptyMap(), potentials)) {
        Map<String, String> variables = rendered.variablesUsed();
        descendants.add(new RenderedSource(variables, sources, potentials, i, rendered));
      }
    }

    return descendants;
  }

  private Optional<Map<String, String>> variablesFor(String source) {
    List<Map<String, String>> satisfyingVars = sources.stream()
        .flatMap(s -> s.variablesFor(source, potentials, Collections.emptyMap())
            .map(Stream::of).orElse(Stream.empty()))
        .collect(Collectors.toList());

    if (satisfyingVars.isEmpty()) {
      return Optional.empty();
    }

    Map<String, String> allVariables = new HashMap<>();
    satisfyingVars.forEach(allVariables::putAll);
    return Optional.of(allVariables);
  }

  private Map<String, String> withImpliedVariables(Map<String, String> variables) {
    Map<String, String> variablesPlusImplied = new HashMap<>(variables);
    Queue<String> varsToExamine = new ArrayDeque<>(variables.keySet());

    while (!varsToExamine.isEmpty()) {
      String var = varsToExamine.poll();
      for (Potential potential : potentials.get(var)) {
        if (!potential.getValue().equals(variablesPlusImplied.get(var))) {
          continue;
        }

        for (Map.Entry<String, String> implied : potential.getImpliedVariables().entrySet()) {
          String impliedKey = implied.getKey();
          String impliedValue = implied.getValue();

          if (variablesPlusImplied.containsKey(impliedKey)) {
            String currentValue = variablesPlusImplied.get(impliedKey);
            if (!Objects.equals(currentValue, impliedValue)) {
              throw new IllegalStateException("Conflicting implied values for variable. " +
                  "Variable '" + impliedKey + "' with implied value of '" + impliedValue + "' " +
                  "conflicts with '" + currentValue + "'");
            }
          } else {
            variablesPlusImplied.put(impliedKey, impliedValue);
            varsToExamine.add(impliedKey);
          }
        }
      }
    }

    return variablesPlusImplied;
  }

  private static class RenderedSource extends AbstractSource {
    private final Map<String, String> variables;
    private final List<DynamicNode> sources;
    private final Map<String, List<Potential>> potentials;
    private final int index;
    private final DynamicNode.RenderedNode rendered;

    private RenderedSource(Map<String, String> variables, List<DynamicNode> sources,
        Map<String, List<Potential>> potentials, int index) {
      this.variables = variables;
      this.sources = sources;
      this.potentials = potentials;
      this.index = index;

      DynamicNode dynamicNode = sources.get(index);
      List<DynamicNode.RenderedNode> renders = dynamicNode.render(this.variables, potentials);

      if (renders.size() != 1) {
        throw new IllegalArgumentException("Expected source with all variables provided to " +
            "produce a single source.");
      }

      this.rendered = renders.get(0);
    }

    private RenderedSource(Map<String, String> variables, List<DynamicNode> sources,
        Map<String, List<Potential>> potentials, int index, DynamicNode.RenderedNode rendered) {
      this.variables = variables;
      this.sources = sources;
      this.potentials = potentials;
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
        if (variables.keySet().containsAll(sources.get(i).variables())) {
          lineage.add(new RenderedSource(variables, sources, potentials, i));
        }
      }

      return lineage;
    }

    @Override
    public List<Source> descendants() {
      List<Source> descendants = new ArrayList<>();

      descendants.add(this);

      for (int i = index + 1; i < sources.size(); i++) {
        DynamicNode dynamicNode = sources.get(i);

        if (dynamicNode.variables().containsAll(variables.keySet())) {
          List<DynamicNode.RenderedNode> renders = dynamicNode.render(variables, potentials);

          for (DynamicNode.RenderedNode render : renders) {
            Map<String, String> descendantVars = new HashMap<>(variables);
            descendantVars.putAll(render.variablesUsed());
            descendants.add(new RenderedSource(descendantVars, sources, potentials, i, render));
          }
        } else {
          // See if any variables used in source have implied values that match what are defined
          // So if host/foo.com implies environment=prod
          // And we have environment=prod
          // Which means that we know this would be a descendant
          // (in other words, host/foo.com has environment=prod in its lineage)
          // What about other variables?
          // host/{area}/{host}
          // host/vary/foo.com with implied environment=prod area=vary
          // Given environment=prod
          // For area, none of the implied are included (it could in some scenarios)
          // For host, foo.com implies environment=prod
          // So what would be a descendant?
          // host/vary/foo.com what about other areas (if area=vary wasn't implied)?
          // host/phx2/foo.com if foo.com implies prod, and we don't have an area,
          // then this is fair game. So we'd expect both.
          // It's basically as if host=foo.com was defined.

          // Go through each variable, find potentials which have implied values matching defined
          // Use them as variables.
          // If multiple potentials, we need to treat each case.


          Map<String, String> variablesPlusImplied = new HashMap<>(variables);

          for (String variableInNode : dynamicNode.variables()) {
            
          }
        }
      }

      return descendants;
    }

    @Override
    public boolean isTargetedBy(SourceSpec spec) {
      return spec.findSource(new DynamicHierarchy(sources, potentials))
          .map(this::equals)
          .orElse(false);
    }

    private boolean allImpliedValuesMatchSuppliedVariables(DynamicNode dynamicNode) {
      // TODO: should this be here?
      if (dynamicNode.variables().isEmpty()) {
        return false;
      }

      for (String variable : dynamicNode.variables()) {
        if (!hasImpliedValuePresent(variable)) {
          return false;
        }
      }

      return true;
    }

    private boolean hasImpliedValuePresent(String variable) {
      for (Potential potential : potentials.get(variable)) {
        for (Map.Entry<String, String> implied : potential.getImpliedVariables().entrySet()) {
          if (variables.containsKey(implied.getKey()) &&
              Objects.equals(variables.get(implied.getKey()), implied.getValue())) {
            return true;
          }
        }
      }

      return false;
    }
  }
}

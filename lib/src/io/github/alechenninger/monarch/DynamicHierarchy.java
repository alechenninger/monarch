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
      List<String> sourceParameters = source.variables();

      if (sourceParameters.containsAll(variableKeys) &&
          variableKeys.containsAll(sourceParameters)) {
        return Optional.of(new RenderedSource(variables, sources, potentials, i));
      }
    }

    return Optional.empty();
  }

  @Override
  public List<Source> descendants() {
    List<Source> descendants = new ArrayList<>();

    for (int i = 0; i < sources.size(); i++) {
      DynamicNode dynamicNode = sources.get(i);
      for (DynamicNode.RenderedNode rendered : dynamicNode.render(Collections.emptyMap(), potentials)) {
        descendants.add(new RenderedSource(rendered.variablesUsed(), sources, potentials, i));
      }
    }

    return descendants;
  }

  private Optional<Map<String, String>> variablesFor(String source) {
    List<Map<String, String>> satisfyingVars = sources.stream()
        .flatMap(s -> s.variablesFor(source, potentials, Collections.emptyMap()).map(Stream::of).orElse(Stream.empty()))
        .collect(Collectors.toList());

    if (satisfyingVars.isEmpty()) {
      return Optional.empty();
    }

    Map<String, String> allVariables = new HashMap<>();
    satisfyingVars.forEach(allVariables::putAll);
    return Optional.of(allVariables);
  }

  private static class RenderedSource extends AbstractSource {
    private final Map<String, String> variables;
    private final List<DynamicNode> sources;
    private final Map<String, List<Potential>> potentials;
    private final int index;
    private final DynamicNode.RenderedNode rendered;

    private RenderedSource(Map<String, String> variables, List<DynamicNode> sources,
        Map<String, List<Potential>> potentials, int index) {
      Map<String, String> variablesPlusImplied = new HashMap<>(variables);
      Queue<String> varsToExamine = new ArrayDeque<>(variables.keySet());

      while (!varsToExamine.isEmpty()) {
        String var = varsToExamine.poll();
        for (Potential potential : potentials.get(var)) {
          if (!potential.getValue().equals(variablesPlusImplied.get(var))) {
            continue;
          }

          for (Map.Entry<String, String> implied : potential.getImpliedValues().entrySet()) {
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

      this.variables = variablesPlusImplied;
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

      for (int i = index; i < sources.size(); i++) {
        DynamicNode dynamicNode = sources.get(i);

        if (dynamicNode.variables().containsAll(variables.keySet())) {
          List<DynamicNode.RenderedNode> rendered = dynamicNode.render(variables, potentials);

          for (DynamicNode.RenderedNode source : rendered) {
            Map<String, String> descendantVars = new HashMap<>(variables);
            descendantVars.putAll(source.variablesUsed());
            descendants.add(new RenderedSource(descendantVars, sources, potentials, i));
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
  }

}

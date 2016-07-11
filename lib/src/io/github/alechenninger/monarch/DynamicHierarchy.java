package io.github.alechenninger.monarch;

import io.github.alechenninger.monarch.DynamicHierarchy.SimpleDynamicSource.RenderedSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO consider supporting implied args or arg groups or something of the sort
// Ex: we know qa.foo.com has "environment" of "qa", so if you target host=qa.foo.com you should see
// environment=qa in ancestry.
public class DynamicHierarchy implements Hierarchy {
  private final List<DynamicSource> sources;
  private final Map<String, List<String>> potentials;

  /**
   *
   * @param sources Ancestors first, descendants last.
   * @param potentials For each variable, a List of known possible values. When a variable is found
   *                   but not supplied, all possible values are used if needed.
   */
  public DynamicHierarchy(List<DynamicSource> sources, Map<String, List<String>> potentials) {
    this.sources = sources;
    this.potentials = potentials;
  }

  @Override
  public Optional<Source> sourceFor(String source) {
    return argsFor(source).flatMap(this::sourceFor);
  }

  @Override
  public List<Source> descendants() {
    List<Source> descendants = new ArrayList<>();

    for (int i = 0; i < sources.size(); i++) {
      DynamicSource dynamicSource = sources.get(i);
      for (RenderedSource rendered : dynamicSource.toRenderedSources(Collections.emptyMap(), potentials)) {
        descendants.add(new SingleDynamicSource(rendered.argsUsed, sources, potentials, i));
      }
    }

    return descendants;
  }

  public Optional<Source> sourceFor(Map<String, String> variables) {
    Set<String> variableKeys = variables.keySet();

    for (int i = 0; i < sources.size(); i++) {
      DynamicSource source = sources.get(i);
      List<String> sourceParameters = source.parameters();

      if (sourceParameters.containsAll(variableKeys) &&
          variableKeys.containsAll(sourceParameters)) {
        return Optional.of(new SingleDynamicSource(variables, sources, potentials, i));
      }
    }

    return Optional.empty();
  }

  private Optional<Map<String, String>> argsFor(String source) {
    List<Map<String, String>> satisfyingArgs = sources.stream()
        .flatMap(s -> s.argsFor(source, potentials, Collections.emptyMap()).map(Stream::of).orElse(Stream.empty()))
        .collect(Collectors.toList());

    if (satisfyingArgs.isEmpty()) {
      return Optional.empty();
    }

    Map<String, String> allArgs = new HashMap<>();
    satisfyingArgs.forEach(allArgs::putAll);
    return Optional.of(allArgs);
  }

  interface DynamicSource {
    List<String> parameters();
    List<String> toStaticSources(Map<String, String> args, Map<String, List<String>> potentials);
    List<RenderedSource> toRenderedSources(Map<String, String> args, Map<String, List<String>> potentials);
    Optional<Map<String, String>> argsFor(String source, Map<String, List<String>> potentials,
        Map<String, String> args);
  }

  public static class SimpleDynamicSource implements DynamicSource {
    private final List<Part> parts;

    public SimpleDynamicSource(List<Part> parts) {
      this.parts = parts;
    }

    public static class Part {
      final String string;
      final boolean isVariable;

      Part(String string, boolean isVariable) {
        this.string = string;
        this.isVariable = isVariable;
      }

      public static Part string(String string) {
        return new Part(string, false);
      }

      public static Part variable(String variable) {
        return new Part(variable, true);
      }

      @Override
      public String toString() {
        return "Part." + (isVariable ? "variable" : "string") + "('" + string + "')";
      }
    }

    @Override
    public List<String> parameters() {
      return parts.stream()
          .filter(p -> p.isVariable)
          .map(p -> p.string)
          .collect(Collectors.toList());
    }

    @Override
    public List<String> toStaticSources(Map<String, String> args,
        Map<String, List<String>> potentials) {
      return toRenderedSources(args, potentials).stream()
          .map(s -> s.builder.toString())
          .collect(Collectors.toList());
    }

    @Override
    public List<RenderedSource> toRenderedSources(Map<String, String> args,
        Map<String, List<String>> potentials) {
      List<RenderedSource> sources = new ArrayList<>();
      sources.add(new RenderedSource());

      for (Part part : parts) {
        List<RenderedSource> sourcesBeforePotentials = new ArrayList<>(sources);
        for (RenderedSource source : sourcesBeforePotentials) {
          if (!part.isVariable) {
            source.append(part.string);
          } else {
            if (args.containsKey(part.string)) {
              String value = args.get(part.string);
              source.append(value);
              source.putArg(part.string, value);
            } else {
              if (!potentials.containsKey(part.string)) {
                throw new IllegalArgumentException("Unable to determine potential values for " +
                    "variable in source: " + part.string);
              }

              List<String> partPotentials = potentials.get(part.string);
              RenderedSource sourceBeforePotential = source.copy();
              for (int iPotential = 0; iPotential < partPotentials.size(); iPotential++) {
                String potential  = partPotentials.get(iPotential);

                if (iPotential == 0) {
                  source.append(potential);
                  source.putArg(part.string, potential);
                } else {
                  RenderedSource newSource = new RenderedSource(sourceBeforePotential);
                  sources.add(newSource);
                  newSource.append(potential);
                  newSource.putArg(part.string, potential);
                }
              }
            }
          }
        }
      }

      return sources;
    }

    @Override
    public Optional<Map<String, String>> argsFor(String source,
        Map<String, List<String>> potentials, Map<String, String> args) {
      return toRenderedSources(args, potentials).stream()
          .filter(s -> s.builder.toString().equals(source))
          .map(s -> s.argsUsed)
          // TODO validate only one found?
          .findFirst();
    }

    @Override
    public String toString() {
      return parts.toString();
    }

    // TODO: Might be better if RenderingSource (mutable) + Rendered (immutable)
    static class RenderedSource {
      final StringBuilder builder = new StringBuilder();
      final Map<String, String> argsUsed = new HashMap<>();

      RenderedSource() {}

      RenderedSource(RenderedSource copy) {
        append(copy.builder.toString());
        argsUsed.putAll(copy.argsUsed);
      }

      void append(String string) {
        builder.append(string);
      }

      void putArg(String arg, String value) {
        argsUsed.put(arg, value);
      }

      RenderedSource copy() {
        return new RenderedSource(this);
      }
    }
  }

  private static class SingleDynamicSource implements Source {
    private final Map<String, String> variables;
    private final List<DynamicSource> sources;
    private final Map<String, List<String>> potentials;
    private final int index;
    private final RenderedSource rendered;

    private SingleDynamicSource(Map<String, String> variables, List<DynamicSource> sources,
        Map<String, List<String>> potentials, int index) {
      this.variables = variables;
      this.sources = sources;
      this.potentials = potentials;
      this.index = index;

      DynamicSource dynamicSource = sources.get(index);
      List<RenderedSource> renders = dynamicSource.toRenderedSources(variables, potentials);

      if (renders.size() != 1) {
        throw new IllegalArgumentException("Expected source with all parameters provided to " +
            "produce a single source.");
      }

      this.rendered = renders.get(0);
    }

    @Override
    public String path() {
      return rendered.builder.toString();
    }

    @Override
    public List<Source> lineage() {
      List<Source> lineage = new ArrayList<>(index);

      for (int i = index; i >= 0; i--) {
        if (variables.keySet().containsAll(sources.get(i).parameters())) {
          lineage.add(new SingleDynamicSource(variables, sources, potentials, i));
        }
      }

      return lineage;
    }

    @Override
    public List<Source> descendants() {
      List<Source> descendants = new ArrayList<>();

      for (int i = index; i < sources.size(); i++) {
        DynamicSource dynamicSource = sources.get(i);

        if (dynamicSource.parameters().containsAll(variables.keySet())) {
          List<RenderedSource> rendered = dynamicSource.toRenderedSources(variables, potentials);

          for (RenderedSource source : rendered) {
            Map<String, String> variables = new HashMap<>(this.variables);
            variables.putAll(source.argsUsed);
            descendants.add(new SingleDynamicSource(variables, sources, potentials, i));
          }
        }
      }

      return descendants;
    }

    @Override
    public String toString() {
      return "SingleDynamicSource{" +
          "variables=" + variables +
          ", sources=" + sources +
          ", potentials=" + potentials +
          ", index=" + index +
          '}';
    }
  }
}

package io.github.alechenninger.monarch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO consider supporting implied args or arg groups or something of the sort
// Ex: we know qa.foo.com has "environment" of "qa", so if you target host=qa.foo.com you should see
// environment=qa in ancestry.
public class DynamicHierarchy implements Hierarchy {
  private final List<DynamicSource> sources;
  private final Map<String, String> args;
  private final Map<String, List<String>> potentials;

  /**
   *
   * @param sources Ancestors first, descendants last.
   * @param potentials For each variable, a List of known possible values. When a variable is found
   *                   but not supplied, all possible values are used if needed.
   */
  public DynamicHierarchy(List<DynamicSource> sources, Map<String, List<String>> potentials) {
    this.sources = sources;
    this.args = Collections.emptyMap();
    this.potentials = potentials;
  }

  public DynamicHierarchy(List<DynamicSource> sources, Map<String, List<String>> potentials,
      Map<String, String> args) {
    this.sources = sources;
    this.args = args;
    this.potentials = potentials;
  }

  @Override
  public Optional<String> target() {
    // TODO: this is not quite right
    List<String> targets = sources.get(0).toStaticSources(args, potentials);

    if (targets.size() != 1) {
      return Optional.empty();
    }

    return Optional.of(targets.get(0));
  }

  @Override
  public List<Hierarchy> currentLevel() {
    // TODO: this is not quite right
    return sources.get(0).toRenderedSources(args, potentials)
        .stream()
        .map(source -> {
          Map<String, String> mergedArgs = new HashMap<>(args);
          mergedArgs.putAll(source.argsUsed);
          return new DynamicHierarchy(sources, potentials, mergedArgs);
        })
        .collect(Collectors.toList());
  }

  @Override
  public List<Hierarchy> descendants() {
    return sources.stream()
        .flatMap(source -> source.toRenderedSources(args, potentials)
            .stream()
            .map(rendered -> {
              Map<String, String> mergedArgs = new HashMap<>(args);
              mergedArgs.putAll(rendered.argsUsed);
              return new DynamicHierarchy(sources, potentials, mergedArgs);
            })
        ).collect(Collectors.toList());
  }

  @Override
  public Optional<List<String>> ancestorsOf(String source) {
    return argsFor(source).flatMap(this::ancestorsOf);
  }

  public Optional<List<String>> ancestorsOf(Map<String, String> variables) {
    Map<String, String> args = new HashMap<>(this.args);
    args.putAll(variables);

    return ListReversed.stream(sources)
        .flatMap(s -> {
          if (args.keySet().containsAll(s.parameters())) {
            return s.toStaticSources(args, potentials).stream();
          }

          return Stream.empty();
        })
        .collect(Collectors.collectingAndThen(
            Collectors.toList(),
            l -> l.isEmpty() ? Optional.empty() : Optional.of(l)
        ));
  }

  @Override
  public Optional<Hierarchy> hierarchyOf(String source) {
    return argsFor(source).flatMap(this::hierarchyOf);
  }

  public Optional<Hierarchy> hierarchyOf(Map<String, String> variables) {
    Map<String, String> merged = new HashMap<>(args);
    merged.putAll(variables);

    List<DynamicSource> limited = sources.stream()
        .filter(s -> s.parameters().containsAll(variables.keySet()))
        .collect(Collectors.toList());

    return Optional.of(new DynamicHierarchy(limited, potentials, merged));
  }

  private Optional<Map<String, String>> argsFor(String source) {
    List<Map<String, String>> satisfyingArgs = sources.stream()
        .flatMap(s -> s.argsFor(source, potentials, args).map(Stream::of).orElse(Stream.empty()))
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
    List<SimpleDynamicSource.RenderedSource> toRenderedSources(Map<String, String> args, Map<String, List<String>> potentials);
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
}

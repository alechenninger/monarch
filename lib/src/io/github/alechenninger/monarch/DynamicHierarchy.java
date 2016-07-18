package io.github.alechenninger.monarch;

import io.github.alechenninger.monarch.DynamicHierarchy.SimpleDynamicSource.RenderedSource;
import org.bigtesting.interpolatd.Interpolator;
import org.bigtesting.interpolatd.Substitutor;

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

  @Override
  public List<Source> descendants() {
    List<Source> descendants = new ArrayList<>();

    for (int i = 0; i < sources.size(); i++) {
      DynamicSource dynamicSource = sources.get(i);
      for (RenderedSource rendered : dynamicSource.render(Collections.emptyMap(), potentials)) {
        descendants.add(new SingleDynamicSource(rendered.argsUsed, sources, potentials, i));
      }
    }

    return descendants;
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

  public interface DynamicSource {
    static List<DynamicSource> fromExpressions(List<String> expressions) {
      return expressions.stream()
          .map(InterpolatedSource::new)
          .collect(Collectors.toList());
    }

    List<String> parameters();
    List<RenderedSource> render(Map<String, String> args, Map<String, List<String>> potentials);
    default Optional<Map<String, String>> argsFor(String source, Map<String, List<String>> potentials,
        Map<String, String> args) {
      return render(args, potentials).stream()
          .filter(s -> s.builder.toString().equals(source))
          .map(s -> s.argsUsed)
          // TODO validate only one found?
          .findFirst();
    }
  }

  public static class InterpolatedSource implements DynamicSource {
    private final String expression;
    private final String variableOpening;
    private final String variableClosing;
    private final Optional<String> escapeCharacter;

    private final List<String> parameters;

    public InterpolatedSource(String expression) {
      this(expression, "%{", "}", Optional.of("\\"));
    }

    public InterpolatedSource(String expression, String variableOpening, String variableClosing,
        Optional<String> escapeCharacter) {
      this.expression = expression;
      this.variableOpening = variableOpening;
      this.variableClosing = variableClosing;
      this.escapeCharacter = escapeCharacter;

      List<String> parameters = new ArrayList<>();
      Interpolator<Void> paramCapture = new Interpolator<>();
      handleInterpolator(paramCapture, (captured, arg) -> {
        parameters.add(captured);
        return null;
      });
      paramCapture.interpolate(expression, null);

      this.parameters = Collections.unmodifiableList(parameters);
    }

    @Override
    public List<String> parameters() {
      return parameters;
    }

    @Override
    public List<RenderedSource> render(Map<String, String> args,
        Map<String, List<String>> potentials) {
      Set<String> providedParams = args.keySet();
      List<String> missingParams = parameters().stream()
          .filter(p -> !providedParams.contains(p))
          .collect(Collectors.toList());

      ArgCombo combos = new ArgCombo(args);
      for (String missing : missingParams) {
        for (String potential : potentials.get(missing)) {
          combos.put(missing, potential);
        }
      }

      return combos.combos.stream()
          .map(c -> {
            Interpolator<Map<String, String>> interpolator = new Interpolator<>();
            RenderedSource rendered = new RenderedSource();
            handleInterpolator(interpolator, (captured, arg) -> {
              String value = c.get(captured);
              rendered.putArg(captured, value);
              return value;
            });
            rendered.append(interpolator.interpolate(expression, c));
            return rendered;
          })
          .collect(Collectors.toList());
    }

    private <T> void handleInterpolator(Interpolator<T> interpolator, Substitutor<T> substitutor) {
      interpolator.when().enclosedBy(variableOpening).and(variableClosing).handleWith(substitutor);
      escapeCharacter.ifPresent(interpolator::escapeWith);
    }

    static class ArgCombo {
      List<Map<String, String>> combos = new ArrayList<>();

      ArgCombo(Map<String, String> args) {
        combos.add(new HashMap<>(args));
      }

      void put(String arg, String value) {
        List<Map<String, String>> newCombos = new ArrayList<>();
        for (Map<String, String> combo : combos) {
          if (combo.containsKey(arg)) {
            Map<String, String> newCombo = new HashMap<>(combo);
            newCombo.put(arg, value);
            newCombos.add(newCombo);
          } else {
            combo.put(arg, value);
          }
        }
        combos.addAll(newCombos);
      }
    }
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
    public List<RenderedSource> render(Map<String, String> args,
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

  private static class SingleDynamicSource extends AbstractSource {
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
      List<RenderedSource> renders = dynamicSource.render(variables, potentials);

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
          List<RenderedSource> rendered = dynamicSource.render(variables, potentials);

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
    public boolean isTargetedBy(SourceSpec spec) {
      return spec.findSource(new DynamicHierarchy(sources, potentials))
          .map(this::equals)
          .orElse(false);
    }
  }
}

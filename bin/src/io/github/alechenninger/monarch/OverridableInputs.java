package io.github.alechenninger.monarch;

import java.util.Optional;
import java.util.function.Function;

public class OverridableInputs implements Inputs {
  private final Inputs override;
  private final Inputs fallback;

  public OverridableInputs(Inputs override, Inputs fallback) {
    this.override = override;
    this.fallback = fallback;
  }

  @Override
  public Optional<String> getHierarchyPathOrYaml() {
    return overridden(Inputs::getHierarchyPathOrYaml);
  }

  @Override
  public Optional<String> getChangesPathOrYaml() {
    return overridden(Inputs::getChangesPathOrYaml);
  }

  @Override
  public Optional<String> getPivotSource() {
    return overridden(Inputs::getPivotSource);
  }

  @Override
  public Optional<String> getDataDir() {
    return overridden(Inputs::getDataDir);
  }

  @Override
  public Optional<String> getConfigPath() {
    return overridden(Inputs::getConfigPath);
  }

  @Override
  public Optional<String> getOutputDir() {
    return overridden(Inputs::getOutputDir);
  }

  @Override
  public Optional<String> getMergeKeys() {
    return overridden(Inputs::getMergeKeys);
  }

  private Optional<String> overridden(Function<Inputs, Optional<String>> input) {
    Optional<String> maybeOverride = input.apply(override);

    if (maybeOverride.isPresent()) {
      return maybeOverride;
    }

    return input.apply(fallback);
  }
}

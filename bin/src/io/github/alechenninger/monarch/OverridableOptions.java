package io.github.alechenninger.monarch;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class OverridableOptions implements MonarchOptions {
  private final MonarchOptions override;
  private final MonarchOptions fallback;

  public OverridableOptions(MonarchOptions override, MonarchOptions fallback) {
    this.override = override;
    this.fallback = fallback;
  }

  @Override
  public Optional<Hierarchy> hierarchy() {
    return overridden(MonarchOptions::hierarchy);
  }

  @Override
  public Set<String> mergeKeys() {
    Set<String> keys = new HashSet<>();
    keys.addAll(override.mergeKeys());
    keys.addAll(fallback.mergeKeys());
    return keys;
  }

  @Override
  public Iterable<Change> changes() {
    return () -> new Iterator<Change>() {
      Iterator<Change> overrideIterator = override.changes().iterator();
      Iterator<Change> fallbackIterator = fallback.changes().iterator();

      @Override
      public boolean hasNext() {
        return overrideIterator.hasNext() || fallbackIterator.hasNext();
      }

      @Override
      public Change next() {
        if (overrideIterator.hasNext()) {
          return overrideIterator.next();
        }

        if (fallbackIterator.hasNext()) {
          return fallbackIterator.next();
        }

        throw new IndexOutOfBoundsException();
      }
    };
  }

  @Override
  public Optional<String> pivotSource() {
    return overridden(MonarchOptions::pivotSource);
  }

  @Override
  public Optional<Map<String, Map<String, Object>>> data() {
    return overridden(MonarchOptions::data);
  }

  @Override
  public Optional<Path> outputDir() {
    return overridden(MonarchOptions::outputDir);
  }

  private <T> Optional<T> overridden(Function<MonarchOptions, Optional<T>> input) {
    Optional<T> maybeOverride = input.apply(override);

    if (maybeOverride.isPresent()) {
      return maybeOverride;
    }

    return input.apply(fallback);
  }
}

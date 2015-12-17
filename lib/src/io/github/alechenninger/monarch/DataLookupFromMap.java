package io.github.alechenninger.monarch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class DataLookupFromMap implements DataLookup {
  private final Map<String, Map<String, Object>> data;
  private final String source;
  private final Hierarchy hierarchy;
  private final Set<String> mergeKeys;

  public DataLookupFromMap(Map<String, Map<String, Object>> data, String source,
      Hierarchy hierarchy, Set<String> mergeKeys) {
    this.data = data;
    this.source = source;
    this.hierarchy = hierarchy;
    this.mergeKeys = mergeKeys;
  }

  @Override
  public Optional<Object> lookup(String key) {
    if (mergeKeys.contains(key)) {
      return lookupMerged(key).map(Merger::getMerged);
    }

    for (String ancestor : sourceAncestry()) {
      Map<String, Object> ancestorData = getDataBySource(ancestor);
      if (ancestorData.containsKey(key)) {
        return Optional.of(ancestorData.get(key));
      }
    }

    return Optional.empty();
  }

  @Override
  public List<SourceToValue> sourcesOf(String key) {
    List<SourceToValue> sources = new ArrayList<>();

    for (String ancestor : sourceAncestry()) {
      Map<String, Object> ancestorData = getDataBySource(ancestor);
      if (ancestorData.containsKey(key)) {
        sources.add(new SourceToValue(ancestor, ancestorData.get(key)));
      }
    }

    return sources;
  }

  @Override
  public List<SourceToValue> sourcesOf(String key, Object value) {
    List<SourceToValue> sources = new ArrayList<>();

    for (String ancestor : sourceAncestry()) {
      Map<String, Object> ancestorData = getDataBySource(ancestor);
      if (ancestorData.containsKey(key)) {
        Object ancestorValue = ancestorData.get(key);

        if (mergeKeys.contains(key)) {
          Merger merger = Merger.startingWith(ancestorValue);
          if (merger.contains(value)) {
            sources.add(new SourceToValue(ancestor, ancestorValue));
          }
        } else {
          if (Objects.equals(ancestorValue, value)) {
            sources.add(new SourceToValue(ancestor, ancestorValue));
          }
        }
      }
    }

    return sources;
  }

  @Override
  public boolean isValueInherited(String key, Object value) {
    List<SourceToValue> sources = sourcesOf(key, value);
    int sourcesCount = sources.size();

    if (sourcesCount == 1 && sources.get(0).source().equals(source)) {
      return false;
    }

    return sourcesCount > 0;
  }

  private List<String> sourceAncestry() {
    return hierarchy.ancestorsOf(source)
        .orElseThrow(() -> new NoSuchElementException("Could not find pivot source in hierarchy. "
            + "Pivot source: " + source + ". Hierarchy: \n" + hierarchy));
  }

  private Map<String, Object> getDataBySource(String source) {
    return data.getOrDefault(source, Collections.emptyMap());
  }

  private Optional<Merger> lookupMerged(String key) {
    Merger merger = null;

    for (String ancestor : sourceAncestry()) {
      Map<String, Object> ancestorData = getDataBySource(ancestor);

      if (ancestorData.containsKey(key)) {
        if (merger == null) {
          merger = Merger.startingWith(ancestorData.get(key));
        } else {
          merger.merge(ancestorData.get(key));
        }
      }
    }
    return Optional.ofNullable(merger);
  }
}

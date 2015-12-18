package io.github.alechenninger.monarch;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MonarchOptionsFromSerializableConfig implements MonarchOptions {
  private final Config config;
  private final FileSystem fileSystem;

  public MonarchOptionsFromSerializableConfig(Config config, FileSystem fileSystem) {
    this.config = config;
    this.fileSystem = fileSystem;
  }

  @Override
  public Optional<Hierarchy> hierarchy() {
    return Optional.ofNullable(config.getHierarchy()).map(Hierarchy::fromStringListOrMap);
  }

  @Override
  public Set<String> mergeKeys() {
    return Optional.ofNullable(config.getMergeKeys()).orElse(Collections.emptySet());
  }

  @Override
  public Iterable<Change> changes() {
    return Optional.ofNullable(config.getChanges())
        .map(c -> c.stream().map(Change::fromMap).collect(Collectors.toList()))
        .orElse(Collections.emptyList());
  }

  @Override
  public Optional<String> pivotSource() {
    return Optional.ofNullable(config.getPivotSource());
  }

  @Override
  public Optional<Path> dataDir() {
    return Optional.ofNullable(config.getDataDir()).map(fileSystem::getPath);
  }

  @Override
  public Optional<Path> outputDir() {
    return Optional.ofNullable(config.getOutputDir()).map(fileSystem::getPath);
  }

  public static class Config {
    private Object hierarchy;
    private List<Map<String, Object>> changes;
    private Set<String> mergeKeys;
    private String pivotSource;
    private String dataDir;
    private String outputDir;

    public Object getHierarchy() {
      return hierarchy;
    }

    public void setHierarchy(Object hierarchy) {
      this.hierarchy = hierarchy;
    }

    public List<Map<String, Object>> getChanges() {
      return changes;
    }

    public void setChanges(List<Map<String, Object>> changes) {
      this.changes = changes;
    }

    public Set<String> getMergeKeys() {
      return mergeKeys;
    }

    public void setMergeKeys(Set<String> mergeKeys) {
      this.mergeKeys = mergeKeys;
    }

    public String getPivotSource() {
      return pivotSource;
    }

    public void setPivotSource(String pivotSource) {
      this.pivotSource = pivotSource;
    }

    public String getDataDir() {
      return dataDir;
    }

    public void setData(String dataDir) {
      this.dataDir = dataDir;
    }

    public String getOutputDir() {
      return outputDir;
    }

    public void setOutputDir(String outputDir) {
      this.outputDir = outputDir;
    }
  }
}

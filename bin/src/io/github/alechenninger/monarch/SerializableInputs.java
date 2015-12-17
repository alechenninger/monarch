package io.github.alechenninger.monarch;

import java.util.Optional;

public class SerializableInputs implements Inputs {
  private String hierarchyPathOrYaml;
  private String changesPathOrYaml;
  private String pivotSource;
  private String dataDir;
  private String configPath;
  private String outputDir;
  private String mergeKeys;

  @Override
  public Optional<String> getHierarchyPathOrYaml() {
    return Optional.ofNullable(hierarchyPathOrYaml);
  }

  @Override
  public Optional<String> getChangesPathOrYaml() {
    return Optional.ofNullable(changesPathOrYaml);
  }

  @Override
  public Optional<String> getPivotSource() {
    return Optional.ofNullable(pivotSource);
  }

  @Override
  public Optional<String> getDataDir() {
    return Optional.ofNullable(dataDir);
  }

  @Override
  public Optional<String> getConfigPath() {
    return Optional.ofNullable(configPath);
  }

  @Override
  public Optional<String> getOutputDir() {
    return Optional.ofNullable(outputDir);
  }

  @Override
  public Optional<String> getMergeKeys() {
    return Optional.ofNullable(mergeKeys);
  }

  public void setHierarchy(String hierarchyPathOrYaml) {
    this.hierarchyPathOrYaml = hierarchyPathOrYaml;
  }

  public void setChanges(String changesPathOrYaml) {
    this.changesPathOrYaml = changesPathOrYaml;
  }

  public void setPivotSource(String pivotSource) {
    this.pivotSource = pivotSource;
  }

  public void setDataDir(String dataDir) {
    this.dataDir = dataDir;
  }

  public void setConfigPath(String configPath) {
    this.configPath = configPath;
  }

  public void setOutputDir(String outputDir) {
    this.outputDir = outputDir;
  }

  public void setMergeKeys(String mergeKeys) {
    this.mergeKeys = mergeKeys;
  }
}

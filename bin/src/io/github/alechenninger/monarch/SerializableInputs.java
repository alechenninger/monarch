/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2015  Alec Henninger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.alechenninger.monarch;

import java.util.Optional;

public class SerializableInputs implements Inputs {
  private String hierarchyPathOrYaml;
  private String changesPathOrYaml;
  private String target;
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
  public Optional<String> getTarget() {
    return Optional.ofNullable(target);
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

  public void setTarget(String target) {
    this.target = target;
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

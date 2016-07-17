/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2016  Alec Henninger
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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Serializable" means it follows conventions of most reflection based serialization libraries,
 * and therefore all fields may be null.
 */
public class SerializableConfig {
  private Object hierarchy;
  private Set<String> mergeKeys;
  private String dataDir;
  private String outputDir;

  /**
   * @return May be List, String, or Map
   * @see Hierarchy#fromStringListOrMap(Object)
   */
  public Object getHierarchy() {
    return hierarchy;
  }

  /**
   * @param hierarchy May be List, String, or Map
   * @see Hierarchy#fromStringListOrMap(Object)
   */
  public void setHierarchy(Object hierarchy) {
    this.hierarchy = hierarchy;
  }

  public Set<String> getMergeKeys() {
    return mergeKeys;
  }

  public void setMergeKeys(Set<String> mergeKeys) {
    this.mergeKeys = mergeKeys;
  }

  public String getDataDir() {
    return dataDir;
  }

  public void setDataDir(String dataDir) {
    this.dataDir = dataDir;
  }

  public String getOutputDir() {
    return outputDir;
  }

  public void setOutputDir(String outputDir) {
    this.outputDir = outputDir;
  }
}

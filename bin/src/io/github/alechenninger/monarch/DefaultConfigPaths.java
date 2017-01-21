/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2017 Alec Henninger
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

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DefaultConfigPaths {
  private final String global;
  private final String working;

  /**
   * Looks in working directory (and parents) for .monarch, and finally ~/.monarch/config.yaml, in
   * that order.
   */
  public static DefaultConfigPaths standard() {
    return new DefaultConfigPaths(
        System.getProperty("user.home") + "/.monarch/config.yaml",
        ".monarch");
  }

  public DefaultConfigPaths(String global, String working) {
    this.global = global;
    this.working = working;
  }

  public List<Path> get(FileSystem fs) {
    List<Path> paths = new ArrayList<>();

    Path working = fs.getPath(this.working);
    paths.add(working);

    for (Path parentToPwd = working.toAbsolutePath().getParent();
         parentToPwd != null;
         parentToPwd = parentToPwd.getParent()) {
      paths.add(parentToPwd.resolve(this.working));
    }

    paths.add(fs.getPath(global));

    return paths;
  }
}

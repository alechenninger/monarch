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

package io.github.alechenninger.monarch.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MoreFiles {
  public static Path createDirectoriesAndWrite(Path path, byte[] bytes) throws IOException {
    createParentDirectories(path);
    return Files.write(path, bytes);
  }

  public static BufferedWriter createDirectoriesForWriter(Path path) throws IOException {
    createParentDirectories(path);
    return Files.newBufferedWriter(path);
  }

  public static void createParentDirectories(Path path) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
  }
}

/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2016 Alec Henninger
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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface SourceData {

  Map<String, Object> data();
  
  default boolean isEmpty() {
    return data().isEmpty();
  }

  default boolean isNotEmpty() {
    return !isEmpty();
  }

  /**
   * Outputs {@code update} to {@code out} in the same data format as the source.
   *
   * <p>Some implementations may use the original source to influence the output, such as by
   * maintaining comments, formatting, or style, etc.
   *
   * <p>Closes {@code out} when done.
   */
  void writeUpdate(Map<String, Object> update, OutputStream out) throws IOException;
}

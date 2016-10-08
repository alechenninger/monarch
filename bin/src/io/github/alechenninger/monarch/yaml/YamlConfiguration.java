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

package io.github.alechenninger.monarch.yaml;

public interface YamlConfiguration {
  YamlConfiguration DEFAULT = new Default();

  default int indent() {
    return 2;
  }

  default Isolate updateIsolation() {
    return Isolate.ALWAYS;
  }

  enum Isolate {
    ALWAYS,
    // TODO: Support WHEN_POSSIBLE
    // This involves modifying unmanaged portion while maintaining formatting and whitespace.
    // Not trivial to do.
    //WHEN_POSSIBLE,
    NEVER
  }

  /** Extendable for reusable toString */
  class Default implements YamlConfiguration {
    @Override
    public String toString() {
      return "YamlConfiguration{" +
          "indent=" + indent() +
          ", updateIsolation=" + updateIsolation() +
          '}';
    }
  }
}


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

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class Variables {
  private final Map<String, Variable> variablesByName;

  public Variables(Map<String, Variable> variablesByName) {
    this.variablesByName = new HashMap<>(variablesByName);
  }

  public Variable byName(String name) {
    if (!variablesByName.containsKey(name)) {
      throw new NoSuchElementException(name);
    }

    return variablesByName.get(name);
  }
}

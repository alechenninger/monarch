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

import java.util.List;
import java.util.Optional;

public interface DataLookup {
  // TODO: lookup may return multiple values depending on assignments for ancestor
  Optional<Object> lookup(String key);
  List<SourceToValue> sourcesOf(String key);
  List<SourceToValue> sourcesOf(String key, Object value);
  // TODO: multiple values may be inherited for same key depending on other assignments
  boolean isValueInherited(String key, Object value);

  final class SourceToValue {
    private final String source;
    private final Object value;

    public SourceToValue(String source, Object value) {
      this.source = source;
      this.value = value;
    }

    public String source() {
      return source;
    }

    public Object value() {
      return value;
    }
  }
}

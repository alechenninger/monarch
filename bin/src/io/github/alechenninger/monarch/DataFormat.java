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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Abstracts a data format that can be used for defining hierarchies, changes, and data sources.
 */
public interface DataFormat {
  Hierarchy parseHierarchy(InputStream hierarchyInput);
  List<Change> parseChanges(InputStream changesInput);
  Map<String, Object> parseMap(InputStream inputStream);
  SourceData newSourceData();
  SourceData parseData(InputStream inputStream) throws IOException;
}

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

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * Parses some kind(s) of {@link InputStream} into monarch primitives like {@link Hierarchy} and
 * {@link Change}.
 *
 * <p>For example, the {@link YamlMonarchParser} can parse YAML files. Other parsers capable of
 * parsing other kinds of input streams may exist.
 */
public interface MonarchParser {
  Hierarchy parseHierarchy(InputStream hierarchyInput);
  Iterable<Change> parseChanges(InputStream changesInput);
  Map<String, Map<String, Object>> readData(Collection<String> sources, Path dataDir);
}

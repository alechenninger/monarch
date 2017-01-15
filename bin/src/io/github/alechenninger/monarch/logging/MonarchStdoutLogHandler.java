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

package io.github.alechenninger.monarch.logging;

import java.io.OutputStream;
import java.util.logging.Level;

// TODO: Output errors to stderr
public class MonarchStdoutLogHandler extends ImmediateFlushStreamHandler {
  public MonarchStdoutLogHandler(OutputStream stdout) {
    super(stdout, new MonarchLogFormatter());
    setLevel(Level.ALL);
    setFilter(record -> record.getLevel().intValue() < Level.WARNING.intValue());
  }
}

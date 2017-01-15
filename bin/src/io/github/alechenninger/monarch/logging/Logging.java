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
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Logging {
  private static Logger rootLogger = Logger.getLogger("");

  /**
   * Configures warnings and above to {@code stderr}, everything else to {@code stdout}. For either
   * output stream, only log record at or above {@code logLevel} are written.
   */
  public static void outputTo(OutputStream stdout, OutputStream stderr) {
    for (Handler handler : rootLogger.getHandlers()) {
      rootLogger.removeHandler(handler);
    }

    rootLogger.addHandler(new MonarchStdoutLogHandler(stdout));
    rootLogger.addHandler(new MonarchStderrLogHandler(stderr));
  }

  public static void setLevel(Level logLevel) {
    rootLogger.setLevel(logLevel);
  }
}

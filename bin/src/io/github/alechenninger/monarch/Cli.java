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

import io.github.alechenninger.monarch.apply.ApplyChangesInput;
import io.github.alechenninger.monarch.apply.ApplyChangesOptions;
import io.github.alechenninger.monarch.apply.ApplyChangesService;
import io.github.alechenninger.monarch.logging.Logging;
import io.github.alechenninger.monarch.set.UpdateSetInput;
import io.github.alechenninger.monarch.set.UpdateSetOptions;
import io.github.alechenninger.monarch.set.UpdateSetService;
import io.github.alechenninger.monarch.yaml.YamlConfiguration;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.logging.Level;

public class Cli {
  private final DefaultConfigPaths defaultConfigPaths;
  private final FileSystem fileSystem;
  private final DataFormats dataFormats;
  private final MonarchArgParser parser;
  private final ApplyChangesService applyChangesService;
  private final UpdateSetService updateSetService;

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(Cli.class);

  public Cli(ApplyChangesService applyChangesService, UpdateSetService updateSetService,
      DataFormats dataFormats, OutputStream stdout, OutputStream stderr,
      DefaultConfigPaths defaultConfigPaths, FileSystem fileSystem) {
    this.dataFormats = dataFormats;
    this.defaultConfigPaths = defaultConfigPaths;
    this.fileSystem = fileSystem;
    this.parser = new ArgParseMonarchArgParser(new DefaultAppInfo());
    this.applyChangesService = applyChangesService;
    this.updateSetService = updateSetService;

    Logging.outputTo(stdout, stderr);
    Logging.setLevel(Level.INFO);
  }

  public int run(String argsSpaceDelimited) {
    return run(argsSpaceDelimited.split(" "));
  }

  public int run(String... args) {
    final CommandInput commandInput;

    try {
      commandInput = parser.parse(args);
    } catch (MonarchArgParserException e) {
      log.error("Error parsing arguments.", e);
      log.info("");
      log.info(e.getHelpMessage());
      return 2;
    }

    commandInput.getLogLevel().ifPresent(Logging::setLevel);

    if (commandInput.isHelpRequested()) {
      log.info(commandInput.getHelpMessage());
    }

    if (commandInput.isVersionRequested()) {
      log.info(commandInput.getVersionMessage());
    }

    for (UpdateSetInput updateSetInput : commandInput.getUpdateSetCommands()) {
      if (updateSetInput.isHelpRequested()) {
        log.info(updateSetInput.getHelpMessage());
        return 0;
      }

      try {
        UpdateSetOptions options = UpdateSetOptions.fromInputAndConfigFiles(updateSetInput,
            fileSystem, dataFormats, defaultConfigPaths);

        SourceSpec source = options.source()
            .orElseThrow(() -> MonarchException.missingOption("source"));
        Path outputPath = options.outputPath()
            .orElseThrow(() -> MonarchException.missingOption("output path"));

        updateSetService.updateSetInChange(source, outputPath, options.changes(),
            options.putInSet(), options.removeFromSet(), options.hierarchy());
      } catch (Exception e) {
        log.error("Error while updating 'set' in change.", e);
        return 2;
      }
    }

    for (ApplyChangesInput applyChangesInput : commandInput.getApplyCommands()) {
      if (applyChangesInput.isHelpRequested()) {
        log.info(applyChangesInput.getHelpMessage());
        return 0;
      }

      try {
        ApplyChangesOptions options = ApplyChangesOptions.fromInputAndConfigFiles(
            applyChangesInput, fileSystem, dataFormats, defaultConfigPaths);

        Path outputDir = options.outputDir()
            .orElseThrow(() -> MonarchException.missingOption("output directory"));
        Path dataDir = options.dataDir()
            .orElseThrow(() -> MonarchException.missingOption("data directory"));
        Hierarchy hierarchy = options.hierarchy()
            .orElseThrow(() -> MonarchException.missingOption("hierarchy"));

        applyChangesService.applyChanges(outputDir, hierarchy, options.target(), options.changes(),
            options.mergeKeys(), options.dataFormatsConfiguration(), dataDir);
      } catch (Exception e) {
        log.error("Error while applying changes.", e);
        return 2;
      }
    }

    return 0;
  }

  public static void main(String[] args) throws IOException, ArgumentParserException {
    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setPrettyFlow(true);
    dumperOptions.setIndent(YamlConfiguration.DEFAULT.indent());
    dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    Yaml yaml = new Yaml(dumperOptions);
    DataFormats.Default dataFormats = new DataFormats.Default();
    Monarch monarch = new Monarch();

    Cli cli = new Cli(
        new ApplyChangesService(dataFormats, monarch),
        new UpdateSetService(yaml),
        dataFormats,
        System.out, System.err,
        DefaultConfigPaths.standard(),
        FileSystems.getDefault()
    );

    System.exit(cli.run(args));
  }

}

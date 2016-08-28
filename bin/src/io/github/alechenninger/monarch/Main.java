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
import io.github.alechenninger.monarch.set.UpdateSetInput;
import io.github.alechenninger.monarch.set.UpdateSetOptions;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Main {
  private final Path defaultConfigPath;
  private final FileSystem fileSystem;
  private final Monarch monarch;
  private final MonarchParsers parsers;
  private final Yaml yaml;
  private final PrintStream consoleOut;
  // TODO make this configurable; maybe use a 'real' logger
  private final boolean debugInfo = true;

  private final MonarchArgParser parser;

  public Main(Monarch monarch, Yaml yaml, String defaultConfigPath, FileSystem fileSystem,
      MonarchParsers parsers, OutputStream consoleOut) {
    this.monarch = monarch;
    this.yaml = yaml;
    this.parsers = parsers;
    this.consoleOut = consoleOut instanceof PrintStream
        ? (PrintStream) consoleOut
        : new PrintStream(consoleOut);
    this.defaultConfigPath = fileSystem.getPath(defaultConfigPath);
    this.fileSystem = fileSystem;
    this.parser = new ArgParseMonarchArgParser(new DefaultAppInfo(), this.consoleOut);
  }

  public int run(String argsSpaceDelimited) {
    return run(argsSpaceDelimited.split(" "));
  }

  public int run(String... args) {
    final CommandInput commandInput;

    try {
      commandInput = parser.parse(args);
    } catch (MonarchArgParserException e) {
      printError(e.getCause());
      consoleOut.println();
      consoleOut.print(e.getHelpMessage());
      return 2;
    }

    if (commandInput.isHelpRequested()) {
      consoleOut.print(commandInput.getHelpMessage());
    }

    if (commandInput.isVersionRequested()) {
      consoleOut.print(commandInput.getVersionMessage());
    }

    for (UpdateSetInput updateSetInput : commandInput.getUpdateSetCommands()) {
      if (updateSetInput.isHelpRequested()) {
        consoleOut.print(updateSetInput.getHelpMessage());
        return 0;
      }

      try {
        UpdateSetOptions options = UpdateSetOptions.fromInputAndConfigFiles(updateSetInput,
            fileSystem, parsers, defaultConfigPath);

        SourceSpec source = options.source()
            .orElseThrow(missingOptionException("source"));
        Path outputPath = options.outputPath()
            .orElseThrow(missingOptionException("output path"));

        updateSetInChange(source, outputPath, options.changes(), options.putInSet(),
            options.removeFromSet(), options.hierarchy());
      } catch (Exception e) {
        printError(e);
        consoleOut.println();
        consoleOut.print(updateSetInput.getHelpMessage());
        return 2;
      }
    }

    for (ApplyChangesInput applyChangesInput : commandInput.getApplyCommands()) {
      if (applyChangesInput.isHelpRequested()) {
        consoleOut.print(applyChangesInput.getHelpMessage());
        return 0;
      }

      try {
        ApplyChangesOptions options = ApplyChangesOptions.fromInputAndConfigFiles(
            applyChangesInput, fileSystem, parsers, defaultConfigPath);

        Path outputDir = options.outputDir()
            .orElseThrow(missingOptionException("output directory"));
        Path dataDir = options.dataDir()
            .orElseThrow(missingOptionException("data directory"));
        Hierarchy hierarchy = options.hierarchy()
            .orElseThrow(missingOptionException("hierarchy"));
        SourceSpec targetSpec = options.target()
            .orElseThrow(missingOptionException("target"));

        Map<String, SourceData> currentData =
            parsers.parseDataSourcesInHierarchy(dataDir, hierarchy);
        Source target = hierarchy.sourceFor(targetSpec).orElseThrow(
            () -> new IllegalArgumentException("Target source not found in hierarchy: " + targetSpec));

        applyChanges(outputDir, options.changes(), options.mergeKeys(), currentData, target);
      } catch (Exception e) {
        printError(e);
        consoleOut.println();
        consoleOut.print(applyChangesInput.getHelpMessage());
        return 2;
      }
    }

    return 0;
  }

  private void applyChanges(Path outputDir, Iterable<Change> changes, Set<String> mergeKeys,
      Map<String, SourceData> currentSources, Source target) throws IOException {
    if (!changes.iterator().hasNext()) {
      consoleOut.println("No changes provided; formatting target.");
    }

    List<String> affectedSources = target.descendants().stream()
        .map(Source::path)
        .collect(Collectors.toList());
    // TODO: Consider currentSources of type Sources or something like that with getter for this
    Map<String, Map<String, Object>> currentData = currentSources.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().data()));

    Map<String, Map<String, Object>> result = monarch.generateSources(
        target, changes, currentData, mergeKeys);

    for (Map.Entry<String, Map<String, Object>> pathToData : result.entrySet()) {
      String path = pathToData.getKey();

      // We only output a source if it is target or under.
      if (!affectedSources.contains(path)) {
        continue;
      }

      if (currentSources.containsKey(path)) {
        SourceData sourceData = currentSources.get(path);
        Map<String, Object> outData = pathToData.getValue();

        // TODO: Test net new and old remove all keys are both written out
        if (sourceData.isEmpty() && outData.isEmpty()) {
          continue;
        }

        Path outPath = outputDir.resolve(path);

        try {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          sourceData.writeNew(outData, out);
          ensureParentDirectories(outPath);
          Files.write(outPath, out.toByteArray());
        } catch (Exception e) {
          // TODO: Proper logger
          new MonarchException("Failed to write updated data source at " + path + " to " + outPath, e)
              .printStackTrace(consoleOut);
        }
      }
    }
  }

  private void updateSetInChange(SourceSpec source, Path outputPath, Iterable<Change> changes,
      Map<String, Object> toPut, Set<String> toRemove, Optional<Hierarchy> hierarchy)
      throws IOException {
    List<Change> outputChanges = StreamSupport.stream(changes.spliterator(), false)
        // Exclude change we're replacing
        .filter(c -> !c.sourceSpec().equals(source))
        .collect(Collectors.toCollection(ArrayList::new));

    // Change we will replace if present
    Optional<Change> sourceChange = StreamSupport.stream(changes.spliterator(), false)
        .filter(c -> source.equals(c.sourceSpec()))
        .findFirst();

    Map<String, Object> updatedSet = sourceChange.map(c -> new HashMap<>(c.set()))
        .orElse(new HashMap<>());
    updatedSet.putAll(toPut);
    updatedSet.keySet().removeAll(toRemove);

    Set<String> remove = sourceChange.map(Change::remove)
        .orElse(Collections.emptySet());

    // Add replacement change to output if it has any remaining content
    if (!updatedSet.isEmpty() || !remove.isEmpty()) {
      Change updatedSourceChange = source.toChange(updatedSet, remove);
      outputChanges.add(updatedSourceChange);
    }

    // Sort by hierarchy depth if provided, else sort alphabetically
    Comparator<Change> changeComparator = hierarchy.map(h -> {
      List<String> descendants = h.descendants().stream()
          .map(Source::path)
          .collect(Collectors.toList());

      return (Comparator<Change>) (c1, c2) -> {
        // TODO make Source Comparable
        String c1Source = c1.sourceSpec().findSource(h).get().path();
        String c2Source = c2.sourceSpec().findSource(h).get().path();

        int c1Index = descendants.indexOf(c1Source);
        int c2Index = descendants.indexOf(c2Source);

        if (c1Index < 0 || c2Index < 0) {
          return c1Source.compareTo(c2Source);
        }

        return c1Index - c2Index;
      };
      // TODO maybe make change comparable too?
    }).orElse((c1, c2) -> c1.toString().compareTo(c2.toString()));

    List<Map<String, Object>> serializableChanges = outputChanges.stream()
        .sorted(changeComparator)
        .map(Change::toMap)
        .collect(Collectors.toList());

    yaml.dumpAll(serializableChanges.iterator(), Files.newBufferedWriter(outputPath));
  }

  private static void ensureParentDirectories(Path path) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
  }

  private static Supplier<? extends RuntimeException> missingOptionException(String option) {
    return () -> new MonarchException("Missing required option: " + option);
  }

  private void printError(Throwable e) {
    if (debugInfo) {
      e.printStackTrace(consoleOut);
    } else {
      consoleOut.println("Error: " + e.getMessage());
    }
  }

  public static void main(String[] args) throws IOException, ArgumentParserException {
    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setPrettyFlow(true);
    dumperOptions.setIndent(2);
    dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    Yaml yaml = new Yaml(dumperOptions);

    int exitCode = new Main(
        new Monarch(),
        yaml,
        System.getProperty("user.home") + "/.monarch/config.yaml",
        FileSystems.getDefault(),
        new MonarchParsers.Default(yaml),
        System.out)
        .run(args);

    System.exit(exitCode);
  }
}

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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
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
import java.util.SortedMap;
import java.util.TreeMap;
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

  private static final Charset UTF_8 = Charset.forName("UTF-8");

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
  }

  public int run(String argsSpaceDelimited) {
    return run(argsSpaceDelimited.split(" "));
  }

  public int run(String[] args) {
    try {
      CommandInput commandInput = new ArgParseCommandInput(new DefaultAppInfo(), args);

      if (commandInput.isHelpRequested()) {
        consoleOut.print(commandInput.getHelpMessage());
      }

      if (commandInput.isVersionRequested()) {
        consoleOut.print(commandInput.getVersionMessage());
      }

      for (UpdateSetInput updateSetInput : commandInput.getUpdateSetCommands()) {
        if (updateSetInput.isHelpRequested()) {
          consoleOut.print(updateSetInput.getHelpMessage());
          continue;
        }

        try {
          UpdateSetOptions options = UpdateSetOptions.fromInputAndConfigFiles(updateSetInput,
              fileSystem, parsers, defaultConfigPath);

          String source = options.source()
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
          continue;
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
          String target = options.target()
              .orElseThrow(missingOptionException("target"));

          applyChanges(outputDir, dataDir, hierarchy, target, options.changes(),
              options.mergeKeys());
        } catch (Exception e) {
          printError(e);
          consoleOut.println();
          consoleOut.print(applyChangesInput.getHelpMessage());
          return 2;
        }
      }

      return 0;
    } catch (Exception e) {
      printError(e);
      consoleOut.println();
      run("--help");

      return 2;
    }
  }

  private void applyChanges(Path outputDir, Path dataDir, Hierarchy hierarchy, String target,
      Iterable<Change> changes, Set<String> mergeKeys) throws IOException {
    if (!changes.iterator().hasNext()) {
      consoleOut.println("No changes provided; formatting target.");
    }

    List<String> affectedSources = hierarchy.hierarchyOf(target)
        .orElseThrow(() -> new IllegalArgumentException("Target source not found in hierarchy: "
            + target))
        .descendants();

    Map<String, Map<String,Object>> currentData = readDataForHierarchy(dataDir, hierarchy);

    Map<String, Map<String, Object>> result = monarch.generateSources(
        hierarchy, changes, target, currentData, mergeKeys);

    for (Map.Entry<String, Map<String, Object>> sourceToData : result.entrySet()) {
      String source = sourceToData.getKey();

      if (!affectedSources.contains(source)) {
        continue;
      }

      Path sourcePath = outputDir.resolve(source);
      ensureParentDirectories(sourcePath);

      SortedMap<String, Object> sorted = new TreeMap<>(sourceToData.getValue());

      if (sorted.isEmpty()) {
        Files.write(sourcePath, new byte[]{});
      } else {
        yaml.dump(sorted, Files.newBufferedWriter(sourcePath, UTF_8));
      }
    }
  }

  private void updateSetInChange(String source, Path outputPath, Iterable<Change> changes,
      Map<String, Object> toPut, Set<String> toRemove, Optional<Hierarchy> hierarchy)
      throws IOException {
    List<Change> outputChanges = StreamSupport.stream(changes.spliterator(), false)
        // Exclude change we're replacing
        .filter(c -> !c.source().equals(source))
        .collect(Collectors.toCollection(ArrayList::new));

    // Change we will replace if present
    Optional<Change> sourceChange = monarch.findChangeForSource(source, changes);

    Map<String, Object> updatedSet = sourceChange.map(c -> new HashMap<>(c.set()))
        .orElse(new HashMap<>());
    updatedSet.putAll(toPut);
    updatedSet.keySet().removeAll(toRemove);

    List<String> remove = sourceChange.map(Change::remove)
        .orElse(Collections.emptyList());

    // Add replacement change to output if it has any remaining content
    if (!updatedSet.isEmpty() || !remove.isEmpty()) {
      Change updatedSourceChange = new Change(source, updatedSet, remove);
      outputChanges.add(updatedSourceChange);
    }

    // Sort by hierarchy depth if provided, else sort alphabetically
    Comparator<Change> changeComparator = hierarchy.map(h -> {
      List<String> descendants = h.descendants();

      return (Comparator<Change>) (c1, c2) -> {
        int c1Index = descendants.indexOf(c1.source());
        int c2Index = descendants.indexOf(c2.source());
        return c1Index - c2Index;
      };
    }).orElse((c1, c2) -> c1.source().compareTo(c2.source()));

    List<Map<String, Object>> serializableChanges = outputChanges.stream()
        .sorted(changeComparator)
        .map(Change::toMap)
        .collect(Collectors.toList());

    yaml.dumpAll(serializableChanges.iterator(), Files.newBufferedWriter(outputPath));
  }

  private Map<String, Map<String, Object>> readDataForHierarchy(Path dataDir, Hierarchy hierarchy) {
    Map<String, Map<String, Object>> data = new HashMap<>();
    Map<String, List<String>> sourcesByExtension = new HashMap<>();

    for (String source : hierarchy.descendants()) {
      sourcesByExtension.merge(
          MonarchParsers.getExtensionForFileName(source),
          asGrowableList(source),
          (l1, l2) -> { l1.addAll(l2); return l1; });
    }

    for (Map.Entry<String, List<String>> extensionSources : sourcesByExtension.entrySet()) {
      String extension = extensionSources.getKey();
      List<String> sources = extensionSources.getValue();
      Map<String, Map<String, Object>> dataForExtension = parsers.forExtension(extension)
          .readData(sources, dataDir);
      data.putAll(dataForExtension);
    }

    return data;
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

  /** As opposed to {@link java.util.Arrays#asList(Object[])}. */
  private static List<String> asGrowableList(String source) {
    List<String> list = new ArrayList<>();
    list.add(source);
    return list;
  }

  private void printError(Exception e) {
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

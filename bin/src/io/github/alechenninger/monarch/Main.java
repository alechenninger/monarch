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
import io.github.alechenninger.monarch.logging.Logging;
import io.github.alechenninger.monarch.set.UpdateSetInput;
import io.github.alechenninger.monarch.set.UpdateSetOptions;
import io.github.alechenninger.monarch.yaml.YamlConfiguration;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Main {
  private final DefaultConfigPaths defaultConfigPaths;
  private final FileSystem fileSystem;
  private final Monarch monarch;
  private final DataFormats dataFormats;
  private final Yaml yaml;
  private final MonarchArgParser parser;

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(Main.class);

  // TODO: Don't use yaml directly for update changeset?
  public Main(Monarch monarch, Yaml yaml, DefaultConfigPaths defaultConfigPaths, FileSystem fileSystem,
      DataFormats dataFormats, OutputStream stdout, OutputStream stderr) {
    this.monarch = monarch;
    this.yaml = yaml;
    this.dataFormats = dataFormats;
    this.defaultConfigPaths = defaultConfigPaths;
    this.fileSystem = fileSystem;
    this.parser = new ArgParseMonarchArgParser(new DefaultAppInfo());

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
            .orElseThrow(missingOptionException("source"));
        Path outputPath = options.outputPath()
            .orElseThrow(missingOptionException("output path"));

        updateSetInChange(source, outputPath, options.changes(), options.putInSet(),
            options.removeFromSet(), options.hierarchy());
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

        DataFormats configuredFormats = options.dataFormatsConfiguration()
            .map(dataFormats::withConfiguration)
            .orElse(dataFormats);

        Path outputDir = options.outputDir()
            .orElseThrow(missingOptionException("output directory"));
        Path dataDir = options.dataDir()
            .orElseThrow(missingOptionException("data directory"));
        Hierarchy hierarchy = options.hierarchy()
            .orElseThrow(missingOptionException("hierarchy"));
        SourceSpec targetSpec = options.target()
            .orElseThrow(missingOptionException("target"));

        Map<String, SourceData> currentData =
            configuredFormats.parseDataSourcesInHierarchy(dataDir, hierarchy);
        Source target = hierarchy.sourceFor(targetSpec).orElseThrow(
            () -> new IllegalArgumentException("No source found in hierarchy which satisfies: " +
                targetSpec));

        Iterable<Change> changes = options.changes();
        for (Change change : changes) {
          checkChangeIsApplicable(hierarchy, change);
        }

        applyChanges(outputDir, changes, options.mergeKeys(), currentData, target);
      } catch (Exception e) {
        log.error("Error while applying changes.", e);
        return 2;
      }
    }

    return 0;
  }

  private void applyChanges(Path outputDir, Iterable<Change> changes, Set<String> mergeKeys,
      Map<String, SourceData> currentSources, Source target) throws IOException {
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

      Path outPath = outputDir.resolve(path);
      Map<String, Object> outData = pathToData.getValue();
      SourceData sourceData = currentSources.containsKey(path)
          ? currentSources.get(path)
          : dataFormats.forPath(outPath).newSourceData();

      if (sourceData.isEmpty() && outData.isEmpty()) {
        continue;
      }

      log.debug("Writing result source data for {} to {}", path, outPath);

      try {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sourceData.writeUpdate(outData, out);
        ensureParentDirectories(outPath);
        Files.write(outPath, out.toByteArray());
      } catch (Exception e) {
        log.error("Failed to write updated data source for " + path + " to " + outPath, e);
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

    if (hierarchy.map(h -> !h.sourceFor(source).isPresent()).orElse(false)) {
      log.warn("Source not found in provided hierarchy. source={}", source);
    }

    // Sort by hierarchy depth if provided, else sort alphabetically
    Comparator<Change> changeComparator = hierarchy.map(h -> {
      List<String> descendants = h.allSources().stream()
          .map(Source::path)
          .collect(Collectors.toList());

      return (Comparator<Change>) (c1, c2) -> {
        // TODO make Source Comparable
        String c1Source = c1.sourceSpec().findSource(h).map(Source::path).orElse("");
        String c2Source = c2.sourceSpec().findSource(h).map(Source::path).orElse("");

        int c1Index = descendants.indexOf(c1Source);
        int c2Index = descendants.indexOf(c2Source);

        if (c1Index < 0 || c2Index < 0) {
          return c1Source.compareTo(c2Source);
        }

        return c1Index - c2Index;
      };
      // TODO maybe make change comparable too?
    }).orElse(Comparator.comparing(Change::toString));

    List<Map<String, Object>> serializableChanges = outputChanges.stream()
        .sorted(changeComparator)
        .map(Change::toMap)
        .collect(Collectors.toList());

    ensureParentDirectories(outputPath);
    yaml.dumpAll(serializableChanges.iterator(), Files.newBufferedWriter(outputPath));
  }

  private static void checkChangeIsApplicable(Hierarchy hierarchy, Change change) {
    SourceSpec spec = change.sourceSpec();
    if (!spec.findSource(hierarchy).isPresent()) {
      log.warn("Source for change not found in hierarchy. You may want to check this " +
          "change's target for correctness: {}", spec);
    }
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

  public static void main(String[] args) throws IOException, ArgumentParserException {
    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setPrettyFlow(true);
    dumperOptions.setIndent(YamlConfiguration.DEFAULT.indent());
    dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    Yaml yaml = new Yaml(dumperOptions);

    int exitCode = new Main(
        new Monarch(),
        yaml,
        DefaultConfigPaths.standard(),
        FileSystems.getDefault(),
        new DataFormats.Default(new DataFormatsConfiguration() {
          @Override
          public Optional<YamlConfiguration> yamlConfiguration() {
            return Optional.of(YamlConfiguration.DEFAULT);
          }
        }),
        System.out, System.err)
        .run(args);

    System.exit(exitCode);
  }
}

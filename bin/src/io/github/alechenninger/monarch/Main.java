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

import org.apache.commons.cli.ParseException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Main {
  private final Path defaultConfigPath;
  private final FileSystem fileSystem;
  private final Monarch monarch;
  private final MonarchParsers parsers;
  private final Yaml yaml;

  private static final Charset UTF_8 = Charset.forName("UTF-8");

  public Main(Monarch monarch, Yaml yaml, String defaultConfigPath, FileSystem fileSystem,
      MonarchParsers parsers) {
    this.monarch = monarch;
    this.yaml = yaml;
    this.parsers = parsers;
    this.defaultConfigPath = fileSystem.getPath(defaultConfigPath);
    this.fileSystem = fileSystem;
  }

  public void run(String argsSpaceDelimited) throws IOException, ParseException {
    run(argsSpaceDelimited.split(" "));
  }

  public void run(String[] args) throws ParseException, IOException {
    try {
      CliInputs cliInputs = CliInputs.parse(args);

      if (cliInputs.helpRequested()) {
        System.out.print(cliInputs.helpMessage());
        return;
      }

      MonarchOptions options = getOptionsFromInputsAndConfigFiles(cliInputs, fileSystem, parsers);

      Path outputDir = options.outputDir()
          .orElseThrow(missingOptionException("output directory"));
      Path dataDir = options.dataDir()
          .orElseThrow(missingOptionException("data directory"));
      Hierarchy hierarchy = options.hierarchy()
          .orElseThrow(missingOptionException("hierarchy"));
      String target = options.target()
          .orElseThrow(missingOptionException("target"));
      Iterable<Change> changes = options.changes();
      Set<String> mergeKeys = options.mergeKeys();

      if (!changes.iterator().hasNext()) {
        System.out.println("No changes provided; formatting target.");
      }

      List<String> affectedSources = hierarchy.hierarchyOf(target)
          .orElseThrow(() -> new IllegalArgumentException("Target source not found in hierarchy: "
              + options.target()))
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
        }
        else {
          yaml.dump(sorted, Files.newBufferedWriter(sourcePath, UTF_8));
        }
      }
    } catch (MonarchException | ParseException e) {
      e.printStackTrace();
      System.out.print(CliInputs.parse(new String[0]).helpMessage());
    }
  }

  private MonarchOptions getOptionsFromInputsAndConfigFiles(CliInputs cliInputs, FileSystem fileSystem,
      MonarchParsers parsers) {
    MonarchOptions options = MonarchOptions.fromInputs(cliInputs, fileSystem, parsers);

    List<Path> pathsFromCli = cliInputs.getConfigPaths()
        .stream()
        .map(fileSystem::getPath)
        .collect(Collectors.toList());

    List<Path> configPaths = new ArrayList<>();
    configPaths.addAll(pathsFromCli);
    configPaths.add(defaultConfigPath);

    for (Path configPath : configPaths) {
      if (Files.exists(configPath)) {
        options = options.fallingBackTo(MonarchOptions.fromYaml(configPath));
      }
    }

    return options;
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

  public static void main(String[] args) throws ParseException, IOException {
    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setPrettyFlow(true);
    dumperOptions.setIndent(2);
    dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    Yaml yaml = new Yaml(dumperOptions);

    new Main(
        new Monarch(),
        yaml,
        System.getProperty("user.home") + "/.monarch/config.yaml",
        FileSystems.getDefault(),
        new MonarchParsers.Default(yaml))
        .run(args);
  }
}

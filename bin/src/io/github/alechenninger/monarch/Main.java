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

      Path configPath = cliInputs.getConfigPath().map(fileSystem::getPath).orElse(defaultConfigPath);
      MonarchOptions optionsFromCli = MonarchOptions.fromInputs(cliInputs, fileSystem, parsers);

      MonarchOptions options = Files.exists(configPath)
          ? optionsFromCli.fallingBackTo(MonarchOptions.fromYaml(configPath))
          : optionsFromCli;

      Path outputDir = options.outputDir()
          .orElseThrow(missingOptionException("output directory"));
      Path dataDir = options.dataDir()
          .orElseThrow(missingOptionException("data directory"));
      Hierarchy hierarchy = options.hierarchy()
          .orElseThrow(missingOptionException("hierarchy"));
      String pivotSource = options.pivotSource()
          .orElseThrow(missingOptionException("pivot source"));
      Iterable<Change> changes = options.changes();
      Set<String> mergeKeys = options.mergeKeys();

      List<String> affectedSources = hierarchy.hierarchyOf(pivotSource)
          .orElseThrow(() -> new IllegalArgumentException("Pivot source not found in hierarchy: "
              + options.pivotSource()))
          .descendants();

      Map<String, Map<String,Object>> currentData = readDataForHierarchy(dataDir, hierarchy);

      Map<String, Map<String, Object>> result = monarch.generateSources(
          hierarchy, changes, pivotSource, currentData, mergeKeys);

      for (Map.Entry<String, Map<String, Object>> sourceToData : result.entrySet()) {
        String source = sourceToData.getKey();

        if (!affectedSources.contains(source)) {
          continue;
        }

        Path sourcePath = outputDir.resolve(source);
        ensureParentDirectories(sourcePath);

        SortedMap<String, Object> sorted = new TreeMap<>(sourceToData.getValue());

        yaml.dump(sorted, Files.newBufferedWriter(sourcePath, UTF_8));
      }
    } catch (MonarchException | ParseException e) {
      e.printStackTrace();
      System.out.print(CliInputs.parse(new String[0]).helpMessage());
    }
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

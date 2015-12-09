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
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

public class Main {
  private final Path defaultConfigPath;
  private final FileSystem fileSystem;
  private final Monarch monarch;
  private final Yaml yaml;

  private static final Charset UTF_8 = Charset.forName("UTF-8");

  public Main(Monarch monarch, Yaml yaml, String defaultConfigPath, FileSystem fileSystem) {
    this.monarch = monarch;
    this.yaml = yaml;
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

      Path configPath = cliInputs.getConfigPath().map(Paths::get).orElse(defaultConfigPath);

      Inputs inputs = Files.exists(configPath)
          ? cliInputs.fallingBackTo(Inputs.fromYaml(configPath))
          : cliInputs;

      MonarchOptions options = MonarchOptions.fromInputs(inputs, fileSystem);
      Path outputDir = options.outputDir();
      Hierarchy hierarchy = options.hierarchy();
      List<String> affectedSources = hierarchy.hierarchyOf(options.pivotSource())
          .orElseThrow(() -> new IllegalArgumentException("Pivot source not found in hierarchy."))
          .descendants();

      Map<String, Map<String, Object>> result = monarch.generateSources(hierarchy,
          options.changes(), options.pivotSource(), options.data(), options.mergeKeys());

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
      run("--help");
    }
  }

  private static void ensureParentDirectories(Path path) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
  }

  public static void main(String[] args) throws ParseException, IOException {
    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setPrettyFlow(true);
    dumperOptions.setIndent(2);
    dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    new Main(
        new Monarch(),
        new Yaml(dumperOptions),
        System.getProperty("user.home") + "/.monarch/config.yaml",
        FileSystems.getDefault())
        .run(args);
  }
}

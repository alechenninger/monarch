package io.github.alechenninger.monarch;

import org.apache.commons.cli.MissingArgumentException;
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
import java.util.Map;

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

      Map<String, Map<String, Object>> result = monarch.generateSources(options.hierarchy(),
          options.changes(), options.pivotSource(), options.data());

      for (Map.Entry<String, Map<String, Object>> sourceToData : result.entrySet()) {
        Path source = outputDir.resolve(sourceToData.getKey());
        ensureParentDirectories(source);
        yaml.dump(sourceToData.getValue(), Files.newBufferedWriter(source, UTF_8));
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
    dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    new Main(
        new Monarch(),
        new Yaml(dumperOptions),
        System.getProperty("user.home") + "/.monarch/config.yaml",
        FileSystems.getDefault())
        .run(args);
  }
}

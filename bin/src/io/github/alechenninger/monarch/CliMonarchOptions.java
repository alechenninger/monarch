package io.github.alechenninger.monarch;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CliMonarchOptions implements MonarchOptions {
  private final Option hierarchyOption = Option.builder("h")
      .longOpt("hierarchy")
      .hasArg()
      .argName("path to hierarchy yaml")
      .desc("Path to a yaml file describing the source hierarchy, relative to the data directory "
          + "(see data-dir option). For example: \n"
          + "global.yaml:\n"
          + "  teams/myteam.yaml:\n"
          + "    teams/myteam/dev.yaml\n"
          + "    teams/myteam/stage.yaml\n"
          + "    teams/myteam/prod.yaml\n"
          + "  teams/otherteam.yaml")
      .build();

  private final Option changesOption = Option.builder("c")
      .longOpt("changes")
      .hasArg()
      .argName("path to changes yaml")
      .desc("Path to a yaml file describing the desired end-state changes. For example: \n"
          + "---\n"
          + "  source: teams/myteam.yaml\n"
          + "  set:\n"
          + "    myapp::version: 2\n"
          + "    myapp::favorite_website: http://www.redhat.com\n"
          + "---\n"
          + "  source: teams/myteam/stage.yaml\n"
          + "  set:\n"
          + "    myapp::favorite_website: http://stage.redhat.com")
      .build();

  private final Option pivotSourceOption = Option.builder("p")
      .longOpt("pivot")
      .hasArg()
      .argName("name of source to pivot on")
      .desc("A pivot source is the source in the source tree from where you want to change, "
          + "including itself and any sources beneath it in the hierarchy. Redundant keys will be "
          + "removed in sources beneath the pivot (that is, sources which inherit its values).")
      .build();

  private final Option dataDirectoryOption = Option.builder("d")
      .longOpt("data-dir")
      .argName("path to data directory")
      .desc("Path to where existing data sources life. The data for sources describe in the "
          + "hierarchy is looked using the paths in the hierarchy relative to this folder.")
      .build();

  private final Option helpOption = Option.builder("h")
      .longOpt("help")
      .desc("Show this text.")
      .build();

  private final Options options = new Options()
      .addOption(hierarchyOption)
      .addOption(changesOption)
      .addOption(pivotSourceOption)
      .addOption(dataDirectoryOption)
      ;

  private final Map<String, MonarchParser> parserByExtension = new HashMap<>();
  private final CommandLine cli;
  private final HelpFormatter helpFormatter = new HelpFormatter();

  public CliMonarchOptions(String[] args) throws ParseException {
    this(args, new DefaultParser(), new MonarchParsers.Default());
  }

  public CliMonarchOptions(String[] args, CommandLineParser cliParser, MonarchParsers dataParsers)
      throws ParseException {
    cli = cliParser.parse(options, args);

    parserByExtension.put("yaml", dataParsers.yaml());
    parserByExtension.put("yml", dataParsers.yaml());
  }

  @Override
  public Hierarchy hierarchy() {
    Path pathToHierarchy = Optional.ofNullable(hierarchyOption.getValue())
        .map(Paths::get)
        .orElseThrow(() -> new MonarchException("No hierarchy provided."));

    try {
      return getParserForPath(pathToHierarchy)
          .parseHierarchy(Files.newInputStream(pathToHierarchy));
    } catch (IOException e) {
      throw new MonarchException("Error reading hierarchy file.", e);
    }
  }

  @Override
  public Iterable<Change> changes() {
    Path pathToChanges = Optional.ofNullable(changesOption.getValue())
        .map(Paths::get)
        .orElseThrow(() -> new MonarchException("No changes provided."));

    try {
      return getParserForPath(pathToChanges)
          .parseChanges(Files.newInputStream(pathToChanges));
    } catch (IOException e) {
      throw new MonarchException("Error reading hierarchy file.", e);
    }
  }

  @Override
  public String pivotSource() {
    return Optional.ofNullable(pivotSourceOption.getValue())
        .orElseThrow(() -> new MonarchException("No changes provided."));
  }

  @Override
  public Map<String, Map<String, Object>> data() {
    Path dataDir = Optional.ofNullable(dataDirectoryOption.getValue())
        .map(Paths::get)
        .orElseThrow(() -> new MonarchException("No data directory provided."));
    Map<String, Map<String, Object>> data = new LinkedHashMap<>();

    Map<String, List<String>> sourcesByExtension = new HashMap<>();
    for (String source : hierarchy().descendants()) {
      sourcesByExtension.merge(
          getExtensionForFileName(source),
          newListWith(source),
          (l1, l2) -> { l1.addAll(l2); return l1; });
    }

    for (Map.Entry<String, List<String>> extensionSources : sourcesByExtension.entrySet()) {
      String extension = extensionSources.getKey();
      List<String> sources = extensionSources.getValue();
      Map<String, Map<String, Object>> dataForExtension = getParserForExtension(extension)
          .readData(sources, dataDir);
      data.putAll(dataForExtension);
    }

    return data;
  }

  public boolean helpRequested() {
    return cli.hasOption(helpOption.getOpt());
  }

  public String helpMessage() {
    StringWriter result = new StringWriter();
    PrintWriter printWriter = new PrintWriter(result);

    helpFormatter.printHelp(printWriter, HelpFormatter.DEFAULT_WIDTH,
        "monarch --hierarchy hierarchy.yaml --changes changes.yaml --pivot teams/myteam.yaml "
            + "--data-dir ~/hieradata", null, options, HelpFormatter.DEFAULT_LEFT_PAD,
        HelpFormatter.DEFAULT_DESC_PAD, "https://github.com/alechenninger/monarch");

    return result.toString();
  }

  private MonarchParser getParserForPath(Path path) {
    String fileName = path.getFileName().toString();
    String extension = getExtensionForFileName(fileName);
    return getParserForExtension(extension);
  }

  private MonarchParser getParserForExtension(String extension) {
    MonarchParser parser = parserByExtension.get(extension);

    if (parser == null) {
      throw new MonarchException("Unsupported file extension: " + extension + ". Supported "
          + "extensions: " + parserByExtension.keySet());
    }

    return parser;
  }

  private static String getExtensionForFileName(String fileName) {
    int extensionIndex = fileName.lastIndexOf('.');

    if (extensionIndex < 0) {
      throw new MonarchException("Please use a file extension. I don't know how to parse this "
          + "file: " + fileName);
    }

    return fileName.substring(extensionIndex);
  }

  private static List<String> newListWith(String source) {
    List<String> list = new ArrayList<>();
    list.add(source);
    return list;
  }
}

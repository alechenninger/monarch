package io.github.alechenninger.monarch;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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

  private final Yaml yaml;
  private final CommandLine cli;
  private final HelpFormatter helpFormatter = new HelpFormatter();

  public CliMonarchOptions(String[] args, CommandLineParser parser, Yaml yaml)
      throws ParseException {
    this.yaml = yaml;

    cli = parser.parse(options, args);
  }

  @Override
  public Hierarchy hierarchy() {
    Path pathToHierarchy = Optional.ofNullable(hierarchyOption.getValue())
        .map(Paths::get)
        .orElseThrow(() -> new MonarchException("No hierarchy provided."));

    try {
      Object parsedHierarchy = yaml.load(Files.newInputStream(pathToHierarchy));
      return Hierarchy.fromStringListOrMap(parsedHierarchy);
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
      Iterable<Object> parsedChanges = yaml.loadAll(Files.newInputStream(pathToChanges));
      List<Change> changes = new ArrayList<>();

      for (Object parsedChange : parsedChanges) {
        Map<String, Object> parsedAsMap = (Map<String, Object>) parsedChange;
        changes.add(Change.fromMap(parsedAsMap));
      }

      return changes;
    } catch (IOException e) {
      throw new MonarchException("Error reading hierarchy file.", e);
    } catch (ClassCastException e) {
      throw new MonarchException("Expected changes yaml to parse as a map. See help for example.",
          e);
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
    List<String> sources = hierarchy().descendants();
    Map<String, Map<String, Object>> data = new LinkedHashMap<>(sources.size());

    try {
      for (String source : sources) {
        Path sourcePath = dataDir.resolve(source);
        Map<String, Object> dataForSource = (Map<String, Object>) yaml.load(
            Files.newInputStream(sourcePath));
        data.put(source, dataForSource);
      }
    } catch (IOException e) {
      throw new MonarchException("Error reading data source.", e);
    } catch (ClassCastException e) {
      throw new MonarchException("Expected data source to parse as map.", e);
    }

    return data;
  }

  @Override
  public boolean helpRequested() {
    return cli.hasOption(helpOption.getOpt());
  }

  @Override
  public String helpMessage() {
    StringWriter result = new StringWriter();
    PrintWriter printWriter = new PrintWriter(result);

    helpFormatter.printHelp(printWriter, HelpFormatter.DEFAULT_WIDTH,
        "monarch --hierarchy hierarchy.yaml --changes changes.yaml --pivot teams/myteam.yaml "
            + "--data-dir ~/hieradata", null, options, HelpFormatter.DEFAULT_LEFT_PAD,
        HelpFormatter.DEFAULT_DESC_PAD, "https://github.com/alechenninger/monarch");

    return result.toString();
  }
}

package io.github.alechenninger.monarch;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

public class CliInputs implements Inputs {
  // NOTE: Special unicode whitespace used in descriptions to defeat help format whitespace trim.
  private final Option hierarchyOption = Option.builder("h")
      .longOpt("hierarchy")
      .hasArg()
      .argName("path")
      .desc("Path to a yaml file describing the source hierarchy, relative to the data directory "
          + "(see data-dir option). For example: \n"
          + "global.yaml:\n"
          + "  teams/myteam.yaml:\n"
          + "    teams/myteam/dev.yaml\n"
          + "    teams/myteam/stage.yaml\n"
          + "    teams/myteam/prod.yaml\n"
          + "  teams/otherteam.yaml")
      .build();

  private final Option changesOption = Option.builder("c")
      .longOpt("changes")
      .hasArg()
      .argName("path")
      .desc("Path to a yaml file describing the desired end-state changes. For example: \n"
          + "---\n"
          + "  source: teams/myteam.yaml\n"
          + "  set:\n"
          + "    myapp::version: 2\n"
          + "    myapp::favorite_website: http://www.redhat.com\n"
          + "---\n"
          + "  source: teams/myteam/stage.yaml\n"
          + "  set:\n"
          + "    myapp::favorite_website: http://stage.redhat.com")
      .build();

  private final Option pivotSourceOption = Option.builder("p")
      .longOpt("pivot")
      .hasArg()
      .argName("source")
      .desc("A pivot source is the source in the source tree from where you want to change, "
          + "including itself and any sources beneath it in the hierarchy. Redundant keys will be "
          + "removed in sources beneath the pivot (that is, sources which inherit its values). "
          + "Ex: 'teams/myteam.yaml'")
      .build();

  private final Option dataDirectoryOption = Option.builder("d")
      .longOpt("data-dir")
      .argName("path")
      .hasArg()
      .desc("Path to where existing data sources life. The data for sources describe in the "
          + "hierarchy is looked using the paths in the hierarchy relative to this folder.")
      .build();

  private final Option configPathOption = Option.builder()
      .longOpt("config")
      .argName("path")
      .hasArg()
      .desc("Path to file which configures default values for command line options.")
      .build();

  private final Option outputDirOption = Option.builder("o")
      .longOpt("output-dir")
      .argName("path")
      .hasArg()
      .desc("Path to directory where result data sources will be written. Data sources will be "
          + "written using relative paths from hierarchy.")
      .build();

  private final Option helpOption = Option.builder("?")
      .longOpt("help")
      .desc("Show this text.")
      .build();

  private final Options options = new Options()
      .addOption(hierarchyOption)
      .addOption(changesOption)
      .addOption(pivotSourceOption)
      .addOption(dataDirectoryOption)
      .addOption(configPathOption)
      .addOption(outputDirOption)
      .addOption(helpOption)
      ;

  private final CommandLine cli;
  private final HelpFormatter helpFormatter = new HelpFormatter();

  public static CliInputs parse(String[] args) throws ParseException {
    return parse(args, new DefaultParser());
  }

  public static CliInputs parse(String[] args, CommandLineParser parser) throws ParseException {
    return new CliInputs(args, parser);
  }

  private CliInputs(String args[], CommandLineParser cliParser) throws ParseException {
    cli = cliParser.parse(options, args);
  }

  @Override
  public Optional<String> getHierarchyPathOrYaml() {
    return Optional.ofNullable(cli.getOptionValue(hierarchyOption.getOpt()));
  }

  @Override
  public Optional<String> getChangesPathOrYaml() {
    return Optional.ofNullable(cli.getOptionValue(changesOption.getOpt()));
  }

  @Override
  public Optional<String> getPivotSource() {
    return Optional.ofNullable(cli.getOptionValue(pivotSourceOption.getOpt()));
  }

  @Override
  public Optional<String> getDataDir() {
    return Optional.ofNullable(cli.getOptionValue(dataDirectoryOption.getOpt()));
  }

  @Override
  public Optional<String> getConfigPath() {
    return Optional.ofNullable(cli.getOptionValue(configPathOption.getLongOpt()));
  }

  @Override
  public Optional<String> getOutputDir() {
    return Optional.ofNullable(cli.getOptionValue(outputDirOption.getOpt()));
  }

  public boolean helpRequested() {
    return cli.hasOption(helpOption.getOpt());
  }

  public String helpMessage() {
    StringWriter result = new StringWriter();
    PrintWriter printWriter = new PrintWriter(result);

    helpFormatter.printHelp(printWriter, 80,
        "monarch --hierarchy hierarchy.yaml --changes changes.yaml --pivot teams/myteam.yaml "
            + "--data-dir ~/hieradata --output-dir ./", null, options, HelpFormatter.DEFAULT_LEFT_PAD,
        HelpFormatter.DEFAULT_DESC_PAD, "https://github.com/alechenninger/monarch");

    return result.toString();
  }
}

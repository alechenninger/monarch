/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2016  Alec Henninger
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
import io.github.alechenninger.monarch.set.UpdateSetInput;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import net.sourceforge.argparse4j.internal.UnrecognizedArgumentException;
import net.sourceforge.argparse4j.internal.UnrecognizedCommandException;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArgParseMonarchArgParser implements MonarchArgParser {
  private final AppInfo appInfo;
  // TODO proper logger
  private final PrintStream consoleOut;

  private static final String SUBPARSER_DEST = "subparser";
  private static final String DEFAULT_COMMAND_WARNING = "WARNING: Defaulting to 'apply' command. "
      + "In the future this default will be removed";

  public ArgParseMonarchArgParser(AppInfo appInfo, PrintStream consoleOut) {
    this.appInfo = appInfo;
    this.consoleOut = consoleOut;
  }

  @Override
  public CommandInput parse(String[] args) throws MonarchArgParserException {
    ArgumentParser parser = ArgumentParsers.newArgumentParser("monarch", false)
        .version(appInfo.version())
        .description(appInfo.description())
        .epilog(appInfo.url());

    parser.addArgument("-?", "--help")
        .dest("help")
        .action(new AbortParsingAction(Arguments.storeTrue()))
        .help("Show this message and exit.");

    parser.addArgument("--version")
        .dest("show_version")
        .action(new AbortParsingAction(Arguments.storeTrue()))
        .help("Show the running version of monarch and exit.");

    Subparsers subparsers = parser.addSubparsers().dest(SUBPARSER_DEST)
        .title("commands")
        .description("If none chosen, defaults to '" + applySpec.name() + "'")
        .help("Pass --help to a command for more information.");

    InputFactory<ApplyChangesInput> applyChangesFactory = applySpec.addToSubparsers(subparsers);
    InputFactory<UpdateSetInput> updateSetFactory = updateSetSpec.addToSubparsers(subparsers);

    try {
      List<String> unknownArgs = new ArrayList<>();
      Namespace parsed = parseArgsDefaultingToApply(parser, args, unknownArgs);
      String subparser = parsed.getString(SUBPARSER_DEST);

      if (!unknownArgs.isEmpty()) {
        String unknownArg = unknownArgs.get(0);
        UnrecognizedArgumentException cause = new UnrecognizedArgumentException(
            "unrecognized arguments: " + unknownArg, parser, unknownArg);
        String helpMessage = getHelpMessage(parser, applyChangesFactory, updateSetFactory, parsed);

        throw new MonarchArgParserException(helpMessage, cause);
      }

      return new CommandInput() {
        @Override
        public List<ApplyChangesInput> getApplyCommands() {
          return applySpec.name().equals(subparser)
              ? Collections.singletonList(applyChangesFactory.getInput(parsed))
              : Collections.emptyList();
        }

        @Override
        public List<UpdateSetInput> getUpdateSetCommands() {
          return updateSetSpec.name().equals(subparser)
              ? Collections.singletonList(updateSetFactory.getInput(parsed))
              : Collections.emptyList();
        }

        @Override
        public String getHelpMessage() {
          return parser.formatHelp();
        }

        @Override
        public boolean isHelpRequested() {
          return parsed.getBoolean("help");
        }

        @Override
        public boolean isVersionRequested() {
          return parsed.getBoolean("show_version");
        }

        @Override
        public String getVersionMessage() {
          return parser.formatVersion() + '\n';
        }
      };
    } catch (ArgumentParserException e) {
      try {
        // Try adding --help to arguments to see if we can get it to parse at all. Then we can know
        // which command to use for help message.
        List<String> helpArgs = new ArrayList<>(args.length + 1);
        Collections.addAll(helpArgs, args);
        helpArgs.add("--help");

        Namespace parsed = parser.parseKnownArgs(helpArgs.stream().toArray(String[]::new), null);
        String helpMessage = getHelpMessage(parser, applyChangesFactory, updateSetFactory, parsed);

        throw new MonarchArgParserException(helpMessage, e);
      } catch (AbortParsingException expected) {
        Map<String, Object> attrs = expected.attrs;
        expected.subparser.ifPresent(s -> attrs.put(SUBPARSER_DEST, s));
        Namespace parsed = new Namespace(attrs);

        String helpMessage = getHelpMessage(parser, applyChangesFactory, updateSetFactory, parsed);

        throw new MonarchArgParserException(helpMessage, e);
      } catch (ArgumentParserException ignored) {
        // That didn't work, just send default help...
      }

      throw new MonarchArgParserException(parser.formatHelp(), e);
    }
  }

  private String getHelpMessage(ArgumentParser parser, InputFactory<ApplyChangesInput>
      applyChangesFactory, InputFactory<UpdateSetInput> updateSetFactory, Namespace parsed) {
    String subparser = parsed.getString(SUBPARSER_DEST);

    if (applySpec.name().equals(subparser)) {
      return applyChangesFactory.getInput(parsed).getHelpMessage();
    } else if (updateSetSpec.name().equals(subparser)) {
      return updateSetFactory.getInput(parsed).getHelpMessage();
    } else {
      return parser.formatHelp();
    }
  }

  private Namespace parseArgsDefaultingToApply(ArgumentParser parser, String[] args,
      List<String> unknownArgs) throws
      ArgumentParserException {
    try {
      Namespace parsed = parser.parseKnownArgs(args, unknownArgs);

      if (!unknownArgs.isEmpty()) {
        List<String> defaultedArgs = new ArrayList<>(args.length + 1);
        Collections.addAll(defaultedArgs, args);
        defaultedArgs.add(0, applySpec.name());

        List<String> secondTryUnknowns = new ArrayList<>(unknownArgs);
        try {
          Namespace secondTryParsed = parser.parseKnownArgs(
              defaultedArgs.stream().toArray(String[]::new), secondTryUnknowns);

          if (secondTryUnknowns.isEmpty()) {
            consoleOut.println(DEFAULT_COMMAND_WARNING);

            unknownArgs.clear();
            unknownArgs.addAll(secondTryUnknowns);

            return secondTryParsed;
          }
        } catch (ArgumentParserException ignored) {
          // retry failed, ignore it...
        }
      }

      return parsed;
    } catch (AbortParsingException e) {
      e.subparser.ifPresent(s -> e.attrs.put(SUBPARSER_DEST, s));
      return new Namespace(e.attrs);
    } catch (UnrecognizedCommandException e) {
      List<String> defaultedArgs = new ArrayList<>(args.length + 1);
      Collections.addAll(defaultedArgs, args);
      defaultedArgs.add(0, applySpec.name());
      unknownArgs.clear();

      consoleOut.println(DEFAULT_COMMAND_WARNING);

      return parser.parseKnownArgs(defaultedArgs.stream().toArray(String[]::new), unknownArgs);
    }
  }

  interface CommandSpec<T> {
    String name();

    InputFactory<T> addToSubparsers(Subparsers subparsers);
  }

  interface InputFactory<T> {
    T getInput(Namespace parsed);
  }

  static CommandSpec<ApplyChangesInput> applySpec = new CommandSpec<ApplyChangesInput>() {
    @Override
    public String name() {
      return "apply";
    }

    @Override
    public InputFactory<ApplyChangesInput> addToSubparsers(Subparsers subparsers) {
      Subparser subparser = subparsers.addParser(name(), false)
          .description("Applies changes to a target data source and its descendants.")
          .help("Applies changes to a target data source and its descendants.");

      subparser.addArgument("-?", "--help")
          .dest("apply_help")
          .action(new AbortParsingAction(Arguments.storeTrue(), name()))
          .help("Show this message and exit.");

      subparser.addArgument("--changes", "--changeset", "--change", "-c")
          .dest("changes")
          .required(true)
          .help("Path to a yaml file describing the desired end-state changes. For example: \n"
              + "---\n"
              + "  source: teams/myteam.yaml\n"
              + "  set:\n"
              + "    myapp::version: 2\n"
              + "    myapp::favorite_website: http://www.redhat.com\n"
              + "---\n"
              + "  source: teams/myteam/stage.yaml\n"
              + "  set:\n"
              + "    myapp::favorite_website: http://stage.redhat.com");

      subparser.addArgument("--target", "-t")
          .dest("target")
          .required(true)
          .help("A target is the source in the source tree from where you want to change, "
              + "including itself and any sources beneath it in the hierarchy. Redundant keys "
              + "will be removed in sources beneath the target (that is, sources which inherit its "
              + "values). "
              + "Ex: 'teams/myteam.yaml'");

      subparser.addArgument("--configs", "--config")
          .dest("configs")
          .metavar("CONFIG")
          .nargs("+")
          .help("Space delimited paths to files which configures default values for command line "
              + "options. The default config path of ~/.monarch/config.yaml is always checked.");

      subparser.addArgument("--hierarchy", "-h")
          .dest("hierarchy")
          .help("Path to a yaml file describing the source hierarchy, relative to the data "
              + "directory "
              + "(see data-dir option). If not provided, will look for a value in config files "
              + "with key 'hierarchy'. Expected YAML structure looks like: \n"
              + "global.yaml:\n"
              + "  teams/myteam.yaml:\n"
              + "    - teams/myteam/dev.yaml\n"
              + "    - teams/myteam/stage.yaml\n"
              + "    - teams/myteam/prod.yaml\n"
              + "  teams/otherteam.yaml");

      subparser.addArgument("--data-dir", "-d")
          .dest("data_dir")
          .help("Path to where existing data sources life. The data for sources describe in the "
              + "hierarchy is looked using the paths in the hierarchy relative to this folder. "
              + "If not provided, will look for a value in config files with key 'dataDir'.");

      subparser.addArgument("--output-dir", "-o")
          .dest("output_dir")
          .help("Path to directory where result data sources will be written. Data sources will be "
              + "written using relative paths from hierarchy. If not provided, will look for a "
              + "value in config files with key 'outputDir'.");

      subparser.addArgument("--merge-keys", "-m")
          .dest("merge_keys")
          .metavar("MERGE_KEY")
          .nargs("+")
          .help("Space-delimited list of keys which should be inherited with merge semantics. "
              + "That is, normally the value that is inherited for a given key is only the nearest "
              + "ancestor's value. Keys that are in the merge key list however inherit values from "
              + "all of their ancestor's and merge them together, provided they are like types of "
              + "either collections or maps. If not provided, will look for an array value in "
              + "config files with key 'outputDir'.");

      return parsed -> new ApplyChangesInput() {
        @Override
        public Optional<String> getHierarchyPathOrYaml() {
          return Optional.ofNullable(parsed.getString("hierarchy"));
        }

        @Override
        public Optional<String> getChangesPathOrYaml() {
          return Optional.ofNullable(parsed.getString("changes"));
        }

        @Override
        public Optional<String> getTarget() {
          return Optional.ofNullable(parsed.getString("target"));
        }

        @Override
        public Optional<String> getDataDir() {
          return Optional.ofNullable(parsed.getString("data_dir"));
        }

        @Override
        public List<String> getConfigPaths() {
          return Optional.ofNullable(parsed.<String>getList("configs"))
              .orElse(Collections.emptyList());
        }

        @Override
        public Optional<String> getOutputDir() {
          return Optional.ofNullable(parsed.getString("output_dir"));
        }

        @Override
        public List<String> getMergeKeys() {
          return Optional.ofNullable(parsed.<String>getList("merge_keys"))
              .orElse(Collections.emptyList());
        }

        @Override
        public boolean isHelpRequested() {
          return Optional.ofNullable(parsed.getBoolean("apply_help")).orElse(false);
        }

        @Override
        public String getHelpMessage() {
          return subparser.formatHelp();
        }
      };
    }
  };

  static CommandSpec<UpdateSetInput> updateSetSpec = new CommandSpec<UpdateSetInput>() {
    @Override
    public String name() {
      return "set";
    }

    @Override
    public InputFactory<UpdateSetInput> addToSubparsers(Subparsers subparsers) {
      Subparser subparser = subparsers.addParser(name(), false)
          .description("Add or remove key value pairs to set within a change.")
          .help("Add or remove key value pairs to set within a change.");

      subparser.addArgument("-?", "--help")
          .dest("set_help")
          .help("Show this message and exit.")
          .action(new AbortParsingAction(Arguments.storeTrue(), name()));

      subparser.addArgument("--changes", "--changeset", "--change", "-c")
          .dest("changes")
          .required(true)
          .help("Path to a file with changes to modify or create.");

      subparser.addArgument("--source", "-s")
          .dest("source")
          .required(true)
          .help("Identifies the change to operate on by its data source.");

      subparser.addArgument("--put", "-p")
          .dest("put")
          .nargs("+")
          .help("Key value pairs to add or replace in the set block of the source's change.\n"
              + "The list may contain paths to yaml files or inline yaml heterogeneously.");

      subparser.addArgument("--remove", "-r")
          .dest("remove")
          .nargs("+")
          .help("List of keys to remove from the set block of a change.\n"
              + "Applied after 'put' so this may remove keys set by the 'put' argument.");

      subparser.addArgument("--hierarchy", "-h")
          .dest("hierarchy")
          .help("Optional path to hierarchy. Only used for sorting entries in the output changes.");

      subparser.addArgument("--configs", "--config")
          .dest("configs")
          .metavar("CONFIG")
          .nargs("+")
          .help("Paths to config files to use for the hierarchy. First one with a hierarchy wins.");

      return parsed -> new UpdateSetInput() {
        @Override
        public Optional<String> getChangesPath() {
          return Optional.ofNullable(parsed.getString("changes"));
        }

        @Override
        public Optional<String> getSource() {
          return Optional.ofNullable(parsed.getString("source"));
        }

        @Override
        public List<String> getPutPathsOrYaml() {
          return Optional.ofNullable(parsed.<String>getList("put"))
              .orElse(Collections.emptyList());
        }

        @Override
        public List<String> getRemovals() {
          return Optional.ofNullable(parsed.<String>getList("remove"))
              .orElse(Collections.emptyList());
        }

        @Override
        public Optional<String> getHierarchyPathOrYaml() {
          return Optional.ofNullable(parsed.getString("hierarchy"));
        }

        @Override
        public List<String> getConfigPaths() {
          return Optional.ofNullable(parsed.<String>getList("configs"))
              .orElse(Collections.emptyList());
        }

        @Override
        public boolean isHelpRequested() {
          return Optional.ofNullable(parsed.getBoolean("set_help")).orElse(false);
        }

        @Override
        public String getHelpMessage() {
          return subparser.formatHelp();
        }
      };
    }
  };

  static class AbortParsingException extends ArgumentParserException {
    final Optional<String> subparser;
    final Argument arg;
    final Map<String, Object> attrs;
    final String flag;
    final Object value;

    public AbortParsingException(Optional<String> subparser, ArgumentParser parser, Argument arg,
        Map<String, Object> attrs, String flag, Object value) {
      super(parser);
      this.subparser = subparser;
      this.arg = arg;
      this.attrs = attrs;
      this.flag = flag;
      this.value = value;
    }
  }

  /**
   * Performs some argument action then aborts, throwing {@link AbortParsingException} to the
   * parser.
   */
  static class AbortParsingAction implements ArgumentAction {
    private final ArgumentAction delegate;
    private final Optional<String> subparser;

    /**
     * Aborts parsing such that no subparser is recorded being found. If you want to keep the
     * current subparser, see {@link #AbortParsingAction(ArgumentAction, String)}
     *
     * @param delegate What to do before aborting.
     */
    AbortParsingAction(ArgumentAction delegate) {
      this.delegate = delegate;
      this.subparser = Optional.empty();
    }

    /**
     * Aborts parsing, tracking the subparser which was parsing the argument.
     *
     * @param delegate What to do before aborting
     * @param subparser The subparser to track when aborting. Available in the {@link
     * AbortParsingException} thrown.
     */
    AbortParsingAction(ArgumentAction delegate, String subparser) {
      this.delegate = delegate;
      this.subparser = Optional.of(subparser);
    }

    @Override
    public void run(ArgumentParser parser, Argument arg, Map<String, Object> attrs, String flag,
        Object value) throws ArgumentParserException {
      delegate.run(parser, arg, attrs, flag, value);
      throw new AbortParsingException(subparser, parser, arg, attrs, flag, value);
    }

    @Override
    public void onAttach(Argument arg) {
      delegate.onAttach(arg);
    }

    @Override
    public boolean consumeArgument() {
      return delegate.consumeArgument();
    }
  }
}

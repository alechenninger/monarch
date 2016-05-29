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

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import net.sourceforge.argparse4j.inf.Subparsers;
import net.sourceforge.argparse4j.internal.UnrecognizedArgumentException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ArgParseCommandInput implements CommandInput {
  private final ArgumentParser parser;
  private final Namespace parsed;
  private final InputFactory<ApplyChangesetInput> applyChangesetFactory;

  public ArgParseCommandInput(AppInfo appInfo, String[] args) throws ArgumentParserException {
    parser = ArgumentParsers.newArgumentParser("monarch", false)
        .version(appInfo.version())
        .description(appInfo.description())
        .epilog(appInfo.url());

    parser.addArgument("--help", "-?")
        .help("Show this message and exit.");

    Subparsers subparsers = parser.addSubparsers().dest("subparser")
        .help("Available commands. Defaults to " + applySpec.name());

    applyChangesetFactory = applySpec.addToSubparsers(subparsers);

    parsed = parseArgsDefaultingToApply(args);
  }

  private Namespace parseArgsDefaultingToApply(String[] args) throws ArgumentParserException {
    try {
      return parser.parseArgs(args);
    } catch (UnrecognizedArgumentException e) {
      // Assume unrecognized because command ommitted.
      List<String> defaultedArgs = new ArrayList<>(args.length + 1);
      defaultedArgs.add(applySpec.name());
      Collections.addAll(defaultedArgs, args);
      // TODO: warn to user here
      return parser.parseArgs(defaultedArgs.stream().toArray(String[]::new));
    }
  }

  @Override
  public List<ApplyChangesetInput> getApplyCommands() {
    if (applySpec.name().equals(parsed.getString("subparser"))) {
      return Collections.singletonList(applyChangesetFactory.getInput(parsed));
    }

    return Collections.emptyList();
  }

  @Override
  public String getHelpMessage() {
    return parser.formatHelp();
  }

  @Override
  public boolean isHelpRequested() {
    return parsed.getBoolean("help");
  }

  interface CommandSpec<T> {
    String name();
    InputFactory<T> addToSubparsers(Subparsers subparsers);
  }

  interface InputFactory<T> {
    T getInput(Namespace parsed);
  }

  static CommandSpec<ApplyChangesetInput> applySpec = new CommandSpec<ApplyChangesetInput>() {
    @Override
    public String name() {
      return "apply";
    }

    @Override
    public InputFactory<ApplyChangesetInput> addToSubparsers(Subparsers subparsers) {
      Subparser subparser = subparsers.addParser(name(), false)
          .help("Applies a changeset to a target data source and its descendants.");

      subparser.addArgument("-?", "--help")
          .action(Arguments.storeTrue())
          .help("Show help for '" + name() + "' command.");

      subparser.addArgument("-h", "--hierarchy")
          .dest("hierarchy")
          .metavar("path")
          .help("Path to a yaml file describing the source hierarchy, relative to the data directory "
              + "(see data-dir option). For example: \n"
              + "global.yaml:\n"
              + "  teams/myteam.yaml:\n"
              + "    teams/myteam/dev.yaml\n"
              + "    teams/myteam/stage.yaml\n"
              + "    teams/myteam/prod.yaml\n"
              + "  teams/otherteam.yaml");

      subparser.addArgument("-c", "--changes", "--changeset")
          .dest("changes")
          .metavar("path");

      subparser.addArgument("-t", "--target")
          .dest("target");

      subparser.addArgument("-d", "--data-dir")
          .dest("data_dir");

      subparser.addArgument("--configs", "--config")
          .dest("configs")
          .nargs("+");

      subparser.addArgument("-o", "--output-dir")
          .dest("output_dir");

      subparser.addArgument("-m", "--merge-keys")
          .dest("merge_keys");

      return parsed -> new ApplyChangesetInput() {
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
          return Optional.ofNullable(parsed.getBoolean("help")).orElse(false);
        }

        @Override
        public String getHelpMessage() {
          return subparser.formatHelp();
        }
      };
    }
  };

}

/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2017 Alec Henninger
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

import io.github.alechenninger.monarch.apply.ApplyChangesService;
import io.github.alechenninger.monarch.set.UpdateSetService;
import io.github.alechenninger.monarch.yaml.YamlConfiguration;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.FileSystems;

public class Main {

  public static void main(String[] args) throws IOException, ArgumentParserException {
    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setPrettyFlow(true);
    dumperOptions.setIndent(YamlConfiguration.DEFAULT.indent());
    dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    Yaml yaml = new Yaml(dumperOptions);
    DataFormats.Default dataFormats = new DataFormats.Default();
    Monarch monarch = new Monarch();

    Cli cli = new Cli(
        new ApplyChangesService(dataFormats, monarch),
        new UpdateSetService(yaml),
        dataFormats,
        System.out, System.err,
        DefaultConfigPaths.standard(),
        FileSystems.getDefault()
    );

    System.exit(cli.run(args));
  }
}

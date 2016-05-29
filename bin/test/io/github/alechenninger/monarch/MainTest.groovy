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

package io.github.alechenninger.monarch

import com.google.common.jimfs.Jimfs
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

import java.nio.file.Files
import java.nio.file.Path

@RunWith(JUnit4.class)
class MainTest {
  def fs = Jimfs.newFileSystem();
  def dumperOptions = new DumperOptions().with {
    defaultFlowStyle = DumperOptions.FlowStyle.BLOCK;
    prettyFlow = true;
    return it;
  }
  def yaml = new Yaml();
  def consolePath = fs.getPath("console");
  def consoleCapture = new PrintStream(Files.newOutputStream(consolePath));

  def main = new Main(new Monarch(), new Yaml(dumperOptions), "/etc/monarch.yaml", fs,
      new MonarchParsers.Default(), consoleCapture);

  static def dataDir = '/etc/hierarchy';
  static def hierarchyFile = "/etc/hierarchy.yaml"

  static def hierarchy = '''
global.yaml:
  teams/myteam.yaml:
    teams/myteam/stage.yaml
'''

  void writeFile(String file, String data) {
    def path = fs.getPath(file);
    def parent = path.getParent();

    if (parent != null) {
      Files.createDirectories(parent);
    }

    Files.write(path, data.getBytes('UTF-8'));
  }

  void writeDataSource(String source, String data) {
    writeFile("$dataDir/$source", data);
  }

  void writeDataSources(Map<String, String> sourceToData) {
    sourceToData.each { key, value -> writeDataSource(key, value)};
  }

  String getConsole() {
    return new String(Files.readAllBytes(consolePath));
  }

  @Before
  public void writeHierarchyYaml() {
    writeFile(hierarchyFile, hierarchy);
  }

  @After
  public void printConsole() {
    System.out.print(getConsole());
  }

  @Test
  public void shouldDefaultToApplyCommand() {
    writeFile('/etc/changes.yaml', '''
---
  source: teams/myteam.yaml
  set:
    myapp::version: 2
    myapp::favorite_website: http://www.redhat.com
---
  source: teams/myteam/stage.yaml
  set:
    myapp::favorite_website: http://stage.redhat.com
''');

    writeDataSources([
        'global.yaml': 'foo: "bar"',
        'teams/myteam.yaml': 'bar: "baz"',
        'teams/myteam/stage.yaml': 'fizz: "buzz"'
    ]);

    main.run("-h ${hierarchyFile} -c /etc/changes.yaml -t teams/myteam.yaml -d $dataDir -o /output/");

    def myteamYaml = new String(Files.readAllBytes(fs.getPath('/output/teams/myteam.yaml')), 'UTF-8');
    def stageYaml = new String(Files.readAllBytes(fs.getPath('/output/teams/myteam/stage.yaml')), 'UTF-8');

    print myteamYaml;
    print stageYaml;
  }

  @Test
  public void applyShouldWriteToFileSystemUsingCommandLineArguments() {
    writeFile('/etc/changes.yaml', '''
---
  source: teams/myteam.yaml
  set:
    myapp::version: 2
    myapp::favorite_website: http://www.redhat.com
---
  source: teams/myteam/stage.yaml
  set:
    myapp::favorite_website: http://stage.redhat.com
''');

    writeDataSources([
        'global.yaml': 'foo: "bar"',
        'teams/myteam.yaml': 'bar: "baz"',
        'teams/myteam/stage.yaml': 'fizz: "buzz"'
    ]);

    main.run("apply -h ${hierarchyFile} -c /etc/changes.yaml -t teams/myteam.yaml -d $dataDir -o /output/");

    def myteamYaml = new String(Files.readAllBytes(fs.getPath('/output/teams/myteam.yaml')), 'UTF-8');
    def stageYaml = new String(Files.readAllBytes(fs.getPath('/output/teams/myteam/stage.yaml')), 'UTF-8');

    print myteamYaml;
    print stageYaml;
  }

  @Test
  public void shouldReadConfigFromInputsAndConfigFiles() {
    writeFile('/etc/changes.yaml', '''
---
  source: teams/myteam.yaml
  set:
    myapp::favorite_website: http://www.redhat.com
''');

    writeDataSources([
        'global.yaml': 'foo: "bar"',
        'teams/myteam.yaml': 'bar: "baz"',
        'teams/myteam/stage.yaml': 'fizz: "buzz"'
    ]);

    writeFile("/etc/monarch.yaml", '''
dataDir: /etc/hierarchy/
''')

    writeFile("/etc/some_other_config.yaml", '''
outputDir: /output/
''')

    writeFile("/etc/and_one_more.yaml", '''
target: teams/myteam.yaml
''')

    main.run("-h ${hierarchyFile} --config /etc/some_other_config.yaml /etc/and_one_more.yaml -c /etc/changes.yaml");

    def myteamYaml = new String(Files.readAllBytes(fs.getPath('/output/teams/myteam.yaml')), 'UTF-8');

    assert yaml.load(myteamYaml) == [
        'bar': 'baz',
        'myapp::favorite_website': 'http://www.redhat.com'
    ]
  }

  @Test
  public void shouldPrintHelpForSpecificCommand() {
    main.run("apply --help");

    assert getConsole().contains("usage: monarch apply");
  }

  @Test
  public void shouldShowVersion() {
    main.run("--version");

    assert getConsole() ==~ /[0-9].[0-9].[0-9]\n/
  }
}

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
 * but WITHOUT ANY WARRANTY without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.alechenninger.monarch

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

import java.nio.file.Files

@RunWith(JUnit4.class)
class MainTest {
  def fs = Jimfs.newFileSystem()
  def dumperOptions = new DumperOptions().with {
    defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
    prettyFlow = true
    return it
  }
  def yaml = new Yaml(dumperOptions)
  def consoleOut = new ByteArrayOutputStream();
  def dataFormats = new DataFormats.Default()

  def main = new Main(new Monarch(), yaml, new DefaultConfigPaths("/etc/monarch.yaml", ".monarch"),
      fs, dataFormats, consoleOut, consoleOut)

  static def dataDir = '/etc/hierarchy'
  static def hierarchyFile = "/etc/hierarchy.yaml"
  static def hierarchy = '''
global.yaml:
  teams/myteam.yaml:
    teams/myteam/stage.yaml
'''

  void writeFile(String file, String data) {
    def path = fs.getPath(file)
    def parent = path.parent

    if (parent != null) {
      Files.createDirectories(parent)
    }

    Files.write(path, data.getBytes('UTF-8'))
  }

  void writeDataSource(String source, String data) {
    def sourcePath = fs.getPath(dataDir, source)
    sourcePath.parent?.identity Files.&createDirectories
    dataFormats.forPath(sourcePath)
        .newSourceData()
        .writeUpdate(yaml.load(data) as Map<String, Object>, Files.newOutputStream(sourcePath))
  }

  void writeUnmanagedDataSource(String source, String data) {
    def sourcePath = fs.getPath(dataDir, source);
    sourcePath.parent?.identity Files.&createDirectories
    Files.write(sourcePath, data.getBytes('UTF-8'))
  }

  void writeDataSources(Map<String, String> sourceToData) {
    sourceToData.each { key, value -> writeDataSource(key, value)}
  }

  String getConsole() {
    return consoleOut.toString();
  }

  @Before
  void writeHierarchyYaml() {
    writeFile(hierarchyFile, hierarchy)
  }

  @After
  void printConsole() {
    System.out.print(console)
  }

  @Test
  void shouldNotDefaultToApplyCommand() {
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
''')

    writeDataSources([
        'global.yaml': 'foo: "bar"',
        'teams/myteam.yaml': 'bar: "baz"',
        'teams/myteam/stage.yaml': 'fizz: "buzz"'
    ])

    main.run("-h ${hierarchyFile} -c /etc/changes.yaml -t teams/myteam.yaml -d $dataDir -o /output/")

    assert Files.notExists(fs.getPath('/output/teams/myteam.yaml'))
    assert Files.notExists(fs.getPath('/output/teams/myteam/stage.yaml'))
  }

  @Test
  void applyShouldWriteToFileSystemUsingCommandLineArguments() {
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
''')

    writeDataSources([
        'global.yaml': 'foo: "bar"',
        'teams/myteam.yaml': 'bar: "baz"',
        'teams/myteam/stage.yaml': 'fizz: "buzz"'
    ])

    main.run("apply -h ${hierarchyFile} -c /etc/changes.yaml -t teams/myteam.yaml -d $dataDir -o /output/")

    def myteamYaml = new String(Files.readAllBytes(fs.getPath('/output/teams/myteam.yaml')), 'UTF-8')
    def stageYaml = new String(Files.readAllBytes(fs.getPath('/output/teams/myteam/stage.yaml')), 'UTF-8')

    assert Files.notExists(fs.getPath('/output/global.yaml'))
    assert [
        'fizz': 'buzz',
        'myapp::favorite_website': 'http://stage.redhat.com',
    ] == yaml.load(stageYaml)
    assert [
        'bar': 'baz',
        'myapp::version': 2,
        'myapp::favorite_website': 'http://www.redhat.com'
    ] == yaml.load(myteamYaml)
  }

  @Test
  void shouldReadConfigFromInputsAndConfigFiles() {
    writeFile('/etc/changes.yaml', '''
---
  source: teams/myteam.yaml
  set:
    myapp::favorite_website: http://www.redhat.com
''')

    writeDataSources([
        'global.yaml': 'foo: "bar"',
        'teams/myteam.yaml': 'bar: "baz"',
        'teams/myteam/stage.yaml': 'fizz: "buzz"'
    ])

    writeFile("/etc/monarch.yaml", '''
dataDir: /etc/hierarchy/
''')

    writeFile("/etc/some_other_config.yaml", '''
outputDir: /output/
''')

    main.run("apply -h ${hierarchyFile} --target teams/myteam.yaml " +
        "--config /etc/some_other_config.yaml /etc/and_one_more.yaml -c /etc/changes.yaml")

    def myteamYaml = new String(Files.readAllBytes(fs.getPath('/output/teams/myteam.yaml')), 'UTF-8')

    assert yaml.load(myteamYaml) == [
        'bar': 'baz',
        'myapp::favorite_website': 'http://www.redhat.com'
    ]
  }

  @Test
  void shouldPrintHelpForMonarch() {
    assert main.run("--help") == 0
    // Crazy regex is to ensure commands are showing up in syntax like {apply, set}
    // This means that it is not showing the help for a command but for monarch itself.
    assert console =~ /usage: monarch.*\{apply/
  }

  @Test
  void shouldPrintHelpForMonarchIfCommandUnrecognized() {
    assert main.run("foobar") == 2
    // Crazy regex is to ensure commands are showing up in syntax like {apply, set}
    // This means that it is not showing the help for a command but for monarch itself.
    assert console =~ /usage: monarch.*\{apply/
  }

  @Test
  void shouldPrintHelpForApplyCommand() {
    assert main.run("apply --help") == 0
    assert console.contains("usage: monarch apply")
  }

  @Test
  void shouldPrintHelpForApplyCommandIfBadArgumentProvided() {
    assert main.run("apply --target foo --changes bar --wat") == 2
    assert console.contains("usage: monarch apply")
  }

  @Test
  void shouldPrintHelpForApplyCommandIfNoArgumentProvided() {
    assert main.run("apply") == 2
    assert console.contains("usage: monarch apply")
  }

  @Test
  void shouldPrintHelpForSetCommand() {
    assert main.run("set --help") == 0
    assert console.contains("usage: monarch set")
  }

  @Test
  void shouldNotPrintHelpForSetCommandIfBadArgumentProvided() {
    main.run("set --source global.yaml foo --changes petstore.yaml")
    assert !console.contains("usage: monarch set")
  }

  @Test
  void shouldOutputErrorsToStderr() {
    def stderr = new ByteArrayOutputStream()
    def main = new Main(
        new Monarch(), yaml, DefaultConfigPaths.standard(), fs, dataFormats, consoleOut, stderr)
    main.run("set --source global.yaml foo --changes petstore.yaml")
    assert stderr.toString().contains("java.lang.IllegalArgumentException")
    assert !console.contains("java.lang.IllegalArgumentException")
  }

  @Test
  void shouldPrintHelpForSetCommandIfNoArgumentProvided() {
    assert main.run("set") == 2
    assert console.contains("usage: monarch set")
  }

  @Test
  void shouldShowVersion() {
    assert main.run("--version") == 0
    assert console ==~ /[0-9].[0-9].[0-9]\n*/
  }

  @Test
  void shouldIncludeFilenameIfChangeHasYamlParseError() {
    writeFile('/etc/changes.yaml', '''
---
\tsource: teams/myteam.yaml
  set:
    myapp::version: 2
    myapp::favorite_website: http://www.redhat.com
---
  source: teams/myteam/stage.yaml
  set:
    myapp::favorite_website: http://stage.redhat.com
''')

    writeDataSources([
        'global.yaml'            : 'foo: "bar"',
        'teams/myteam.yaml'      : 'bar: "baz"',
        'teams/myteam/stage.yaml': 'fizz: "buzz"'
    ])

    main.run("apply -h ${hierarchyFile} -c /etc/changes.yaml -t teams/myteam.yaml -d $dataDir -o /output/")

    assert console =~ '/etc/changes.yaml'
  }

  @Test
  void shouldIncludeFilenameIfHierarchyHasYamlParseError() {
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
''')

    writeFile(hierarchyFile, "/t$hierarchy")

    writeDataSources([
        'global.yaml'            : 'foo: "bar"',
        'teams/myteam.yaml'      : 'bar: "baz"',
        'teams/myteam/stage.yaml': 'fizz: "buzz"'
    ])

    main.run("apply -h ${hierarchyFile} -c /etc/changes.yaml -t teams/myteam.yaml -d $dataDir -o /output/")

    assert console =~ hierarchyFile
  }

  @Test
  void setShouldCreateNewChangesetIfNoneExists() {
    main.run("set", "--changes", "/etc/new.yaml", "--source", "teams/myteam.yaml", "--put", "foo: bar");

    def changes = yaml.loadAll(Files.newBufferedReader(fs.getPath("/etc/new.yaml")))
        .collect { Change.fromMap(it as Map<String, Object>) }
        .toList()

    def expected = [Change.forPath("teams/myteam.yaml", ["foo": "bar"], [])]

    assert expected == changes
  }

  @Test
  void setShouldDoNothingIfEmptyStringPassedToPut() {
    writeFile('/etc/changes.yaml', '''
---
source: teams/myteam.yaml
set:
  myapp::version: 2
''')

    main.run("set", "--changes", "/etc/changes.yaml", "--source", "teams/myteam.yaml", "--put", "");

    def changes = yaml.loadAll(Files.newBufferedReader(fs.getPath("/etc/changes.yaml")))
        .collect { Change.fromMap(it as Map<String, Object>) }
        .toList()

    def expected = [Change.forPath("teams/myteam.yaml", ["myapp::version": 2], [])]

    assert expected == changes
  }

  @Test
  void setShouldCreateChangeByVariables() {
    main.run("set", "--change", "/etc/changes.yaml", "--source", "environment=qa", "team=ops",
        "--put", "app_version: 2");

    def changes = yaml.loadAll(Files.newBufferedReader(fs.getPath("/etc/changes.yaml")))
        .collect { Change.fromMap(it as Map<String, Object>) }
        .toList()

    def expected = [Change.forVariables(
        ["environment": "qa", "team": "ops"], ["app_version": 2], [])]

    assert expected == changes
  }

  @Test
  void setShouldUpdateExistingChangeByVariables() {
    writeFile('/etc/changes.yaml', '''
---
source:
  environment: qa
  team: ops
set:
  app_url: qa.app.com
---
source:
  environment: stage
  team: ops
set:
  app_url: old.stage.app.com
''')

    main.run("set", "--change", "/etc/changes.yaml", "--source", "environment=stage", "team=ops",
        "--put", "app_url: stage.app.com");

    def changes = yaml.loadAll(Files.newBufferedReader(fs.getPath("/etc/changes.yaml")))
        .collect { Change.fromMap(it as Map<String, Object>) }
        .toList()

    def expected = [
        Change.forVariables(["environment": "qa", "team": "ops"], ["app_url": "qa.app.com"], []),
        Change.forVariables(["environment": "stage", "team": "ops"], ["app_url": "stage.app.com"], [])
    ]

    assert expected == changes
  }

  @Test
  void setShouldMaintainRemoveKeys() {
    writeFile('/etc/changes.yaml', '''
---
source: foo/bar.yaml
set:
  key: value
remove:
  - key_to_remove
''')

    main.run("set", "--change", "/etc/changes.yaml", "--source", "foo/bar.yaml");

    def changes = new String(Files.readAllBytes(fs.getPath("/etc/changes.yaml")), 'UTF-8')

    assert changes =~ /- key_to_remove/
  }

  @Test
  void applyShouldApplyChangeByVariables() {
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
''')

    writeDataSources([
        'global.yaml': 'foo: "bar"',
        'teams/myteam.yaml': 'bar: "baz"',
        'teams/myteam/stage.yaml': 'fizz: "buzz"'
    ])

    writeFile('/etc/hierarchy.yaml', '''
sources:
  - global.yaml
  - teams/%{team}.yaml
  - teams/%{team}/%{environment}.yaml
inventory:
  team:
    - myteam
    - otherteam
  environment:
    - dev
    - qa
    - stage
    - prod
''')

    main.run("apply -h /etc/hierarchy.yaml -c /etc/changes.yaml -t team=myteam -d $dataDir -o /output/")

    def myteamYaml = new String(Files.readAllBytes(fs.getPath('/output/teams/myteam.yaml')), 'UTF-8')
    def stageYaml = new String(Files.readAllBytes(fs.getPath('/output/teams/myteam/stage.yaml')), 'UTF-8')

    assert Files.notExists(fs.getPath('/output/global.yaml'))
    assert [
        'fizz': 'buzz',
        'myapp::favorite_website': 'http://stage.redhat.com',
    ] == yaml.load(stageYaml)
    assert [
        'bar': 'baz',
        'myapp::version': 2,
        'myapp::favorite_website': 'http://www.redhat.com'
    ] == yaml.load(myteamYaml)
  }

  @Test
  void applyShouldWriteSourceIfAllKeysRemoved() {
    writeDataSource('global.yaml', 'bar: 123')
    writeFile('/etc/changes.yaml', '''
---
source: global.yaml
remove:
  - bar
''')

    main.run("apply -h $hierarchyFile -c /etc/changes.yaml -t global.yaml -d $dataDir -o /output/")

    assert Files.exists(fs.getPath('/output/global.yaml'))

    def globalYaml = new String(Files.readAllBytes(fs.getPath('/output/global.yaml')), 'UTF-8')

    assert [:] == (yaml.load(globalYaml) ?: [:])
  }

  @Test
  void applyShouldNotWriteSourceIfWasEmptyAndIsStillEmpty() {
    writeFile('/etc/changes.yaml', '''
---
source: teams/myteam.yaml
set:
  foo: bar
''')

    main.run("apply -h $hierarchyFile -c /etc/changes.yaml -t global.yaml -d $dataDir -o /output/")

    assert Files.notExists(fs.getPath('/output/global.yaml'))
  }

  @Test
  @Ignore("TODO: test this")
  void applyShouldNotWriteAnythingIfWriteFailed() {
    // need to have a test Parsers implementation that just fails to write all the time
  }

  @Test
  void applyShouldAllowConfiguringYamlIsolateToNeverOverCommandLine() {
    writeUnmanagedDataSource('global.yaml', '''
key: value
''')

    writeFile('/etc/changes.yaml', '''
---
source: global.yaml
set:
  key: new value
''')

    main.run('apply', '-h', hierarchyFile, '-c', '/etc/changes.yaml', '-t', 'global.yaml',
        '-d', dataDir, '-o', '/output/', '--yaml-isolate', 'never')

    def globalYaml = new String(Files.readAllBytes(fs.getPath('/output/global.yaml')), 'UTF-8')

    assert ['key': 'new value'] == yaml.load(globalYaml)
  }

  @Test
  void applyShouldUseYamlConfigurationFromConfigFile() {
    writeFile('/etc/config.yaml', '''
dataFormats:
  yaml:
    isolate: never
''')

    writeUnmanagedDataSource('global.yaml', '''
key: value
''')

    writeFile('/etc/changes.yaml', '''
---
source: global.yaml
set:
  key: new value
''')

    main.run('apply', '-h', hierarchyFile, '-c', '/etc/changes.yaml', '-t', 'global.yaml',
        '-d', dataDir, '-o', '/output/', '--config', '/etc/config.yaml')

    def globalYaml = new String(Files.readAllBytes(fs.getPath('/output/global.yaml')), 'UTF-8')

    assert ['key': 'new value'] == yaml.load(globalYaml)
  }

  @Test
  void applyShouldAllowConfiguringYamlIsolateToNeverOverCommandLineOverriddingConfig() {
    writeFile('/etc/config.yaml', '''
dataFormats:
  yaml:
    isolate: always
''')

    writeUnmanagedDataSource('global.yaml', '''
key: value
''')

    writeFile('/etc/changes.yaml', '''
---
source: global.yaml
set:
  key: new value
''')

    main.run('apply', '-h', hierarchyFile, '-c', '/etc/changes.yaml', '-t', 'global.yaml',
        '-d', dataDir, '-o', '/output/', '--yaml-isolate', 'never', '--config', '/etc/config.yaml')

    def globalYaml = new String(Files.readAllBytes(fs.getPath('/output/global.yaml')), 'UTF-8')

    assert ['key': 'new value'] == yaml.load(globalYaml)
  }

  @Test
  void applyShouldAllowConfiguringYamlIsolateToAlwaysOverCommandLineOverriddingConfig() {
    writeFile('/etc/config.yaml', '''
dataFormats:
  yaml:
    isolate: never
''')

    writeUnmanagedDataSource('global.yaml', '''
key: value
''')

    writeFile('/etc/changes.yaml', '''
---
source: global.yaml
set:
  key: new value
''')

    main.run('apply', '-h', hierarchyFile, '-c', '/etc/changes.yaml', '-t', 'global.yaml',
        '-d', dataDir, '-o', '/output/', '--yaml-isolate', 'always', '--config', '/etc/config.yaml')

    assert Files.notExists(fs.getPath('/output/global.yaml'))
  }

  @Test
  void looksForDotMonarchFilesInWorkingDirectoryAndAncestorsForApplyCommand() {
    fs = Jimfs.newFileSystem(Configuration.unix().toBuilder()
        .setWorkingDirectory("/working/directory/")
        .build())
    main = new Main(
        new Monarch(), yaml, DefaultConfigPaths.standard(), fs, dataFormats, consoleOut, consoleOut)

    writeFile('/.monarch', '''
hierarchy:
  top.yaml:
  - bottom.yaml
''')

    writeFile('/working/.monarch', '''
dataDir: /hieradata/
''')

    writeFile('/working/directory/.monarch', '''
outputDir: /output/
''')

    writeFile('/etc/changes.yaml', '''
---
source: top.yaml
set:
  key: new value
''')

    main.run('apply', '-c', '/etc/changes.yaml', '-t', 'top.yaml')

    def topPath = fs.getPath('/output/top.yaml')
    assert Files.exists(topPath)

    def topYaml = new String(Files.readAllBytes(topPath), 'UTF-8')
    assert ['key': 'new value'] == yaml.load(topYaml)
  }

  @Test
  void looksForDotMonarchFilesInWorkingDirectoryAndAncestorsForSetCommand() {
    fs = Jimfs.newFileSystem(Configuration.unix().toBuilder()
        .setWorkingDirectory("/working/directory/")
        .build())
    main = new Main(
        new Monarch(), yaml, DefaultConfigPaths.standard(), fs, dataFormats, consoleOut, consoleOut)

    writeFile('/.monarch', '''
hierarchy:
  top.yaml:
  - bottom.yaml
''')

    main.run('set', '-c', '/etc/changes.yaml', '-s', 'top.yaml', '--put', 'key: new value')
    main.run('set', '-c', '/etc/changes.yaml', '-s', 'bottom.yaml', '--put', 'bottom_key: new value')

    // Test sorting which should use hierarchy config in working directory ancestry
    // A bit convoluted admittedly
    assert ['top.yaml', 'bottom.yaml'] ==
        yaml.loadAll(Files.newBufferedReader(fs.getPath('/etc/changes.yaml')))
            .collect { it['source'] }
  }
}

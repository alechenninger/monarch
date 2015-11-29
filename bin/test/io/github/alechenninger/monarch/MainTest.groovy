package io.github.alechenninger.monarch

import com.google.common.jimfs.Jimfs
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

import java.nio.file.Files

@RunWith(JUnit4.class)
class MainTest {
  def fs = Jimfs.newFileSystem();
  def dumperOptions = new DumperOptions().with {
    defaultFlowStyle = DumperOptions.FlowStyle.BLOCK;
    prettyFlow = true;
    return it;
  }

  def main = new Main(new Monarch(), new Yaml(dumperOptions), "/etc/monarch.yaml", fs);

  static def dataDir = '/etc/hierarchy';
  static def hierarchyFile = "/etc/hierarchy.yaml"

  static def hierarchy = '''
global.yaml:
  teams/myteam.yaml:
    teams/myteam/stage.yaml
''';

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

  @Before
  public void writeHierarchyYaml() {
    writeFile(hierarchyFile, hierarchy);
  }

  @Test
  public void shouldWriteToFileSystemUsingCommandLineArguments() {
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

    main.run("-h ${hierarchyFile} -c /etc/changes.yaml -p teams/myteam.yaml -d $dataDir -o /output/");

    def myteamYaml = new String(Files.readAllBytes(fs.getPath('/output/teams/myteam.yaml')), 'UTF-8');
    def stageYaml = new String(Files.readAllBytes(fs.getPath('/output/teams/myteam/stage.yaml')), 'UTF-8');

    print myteamYaml;
    print stageYaml;
  }
}

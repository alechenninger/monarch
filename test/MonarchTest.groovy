import org.junit.Test
import org.yaml.snakeyaml.Yaml

class MonarchTest {
  Monarch m = new Monarch()
  Yaml yaml = new Yaml()

  Map generateFromYaml(String hierarchy, String changes, String sourceToChange, Map data) {
    return m.generate(
        yaml.load(hierarchy) as Map,
        yaml.loadAll(changes).collect { Change.fromMap(it as Map)},
        sourceToChange,
        data.with {
          it.each { entry ->
            entry.setValue(this.yaml.load(entry.getValue()))
          }
        })
  }

  @Test
  void shouldAddNewKeys() {
    def hierarchy = '''
group/myteam.yaml:
  group/myteam/stage.yaml
''';

    def changes = '''
---
  source: group/myteam.yaml
  set:
    myapp::version: 2
    myapp::favorite_website: http://www.redhat.com
  remove:
    - myapp::should_have_bugs
---
  source: group/myteam/stage.yaml
  set:
    myapp::favorite_website: http://stage.redhat.com
''';

    def rootStart = '''
# Comment
myapp::version: 1
myapp::should_have_bugs: false

otherapp::version: 5
''';

    def stageStart = '''
myapp::version: 1
myapp::a_stage_specific_value: "should stay here"
myapp::should_have_bugs: true

otherapp::version: 5
''';

    def result = generateFromYaml(
        hierarchy, changes, 'group/myteam/stage.yaml', [
      'group/myteam.yaml': rootStart,
      'group/myteam/stage.yaml': stageStart
    ]);

    def newStage = result['group/myteam/stage.yaml'];
    def expected = yaml.load('''
myapp::version: 2
myapp::a_stage_specific_value: "should stay here"

otherapp::version: 5
myapp::favorite_website: http://stage.redhat.com
''');

    assert newStage == expected
  }
}

import org.junit.Test
import org.yaml.snakeyaml.Yaml

class MonarchTest {
  def m = new Monarch()
  def yaml = new Yaml()

  def hierarchy = '''
global.yaml:
  myteam.yaml:
    myteam/stage.yaml
''';

  Map generateFromYaml(String hierarchy, String changes, String sourceToChange, Map data) {
    return m.generateHierarchy(
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
  void shouldSetValuesFromClosestAncestorIfNotAlreadyInherited() {
    def changes = '''
---
  source: myteam.yaml
  set:
    myapp::version: 2
    myapp::favorite_website: http://www.redhat.com
---
  source: myteam/stage.yaml
  set:
    myapp::favorite_website: http://stage.redhat.com
''';

    def rootStart = '''
myapp::version: 1

otherapp::version: 5
''';

    def stageStart = '''
myapp::version: 1
myapp::a_stage_specific_value: "should stay here"

otherapp::version: 5
''';

    def result = generateFromYaml(
        hierarchy, changes, 'myteam/stage.yaml', [
        'global.yaml': '',
        'myteam.yaml': rootStart,
        'myteam/stage.yaml': stageStart
    ]);

    def expected = [
        'global.yaml': [:],
        'myteam.yaml': yaml.load(rootStart),
        'myteam/stage.yaml': yaml.load('''
myapp::version: 2
myapp::a_stage_specific_value: "should stay here"

otherapp::version: 5
myapp::favorite_website: http://stage.redhat.com
''')]

    assert result == expected
  }


  @Test
  public void shouldRemoveValuesIfRemovedInClosestAncestor() {
    def changes = '''
---
  source: myteam.yaml
  remove:
    - myapp::should_have_bugs
''';

    def rootStart = '''
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
        hierarchy, changes, 'myteam/stage.yaml', [
        'global.yaml': '',
        'myteam.yaml': rootStart,
        'myteam/stage.yaml': stageStart
    ]);

    def expected = [
        'global.yaml': [:],
        'myteam.yaml': yaml.load(rootStart),
        'myteam/stage.yaml': yaml.load('''
myapp::version: 1
myapp::a_stage_specific_value: "should stay here"

otherapp::version: 5
''')]

    assert result == expected
  }

  @Test
  public void shouldNotAddValueIfSameValueIsAlreadyInherited() {
    def changes = '''
---
  source: global.yaml
  set:
    myapp::version: 2
''';

    def result = generateFromYaml(
        hierarchy, changes, 'myteam/stage.yaml', [
        'global.yaml': '',
        'myteam.yaml': 'myapp::version: 2',
        'myteam/stage.yaml': ''
    ]);

    def expected = [
        'global.yaml': [:],
        'myteam.yaml': ['myapp::version': 2],
        'myteam/stage.yaml': [:]
    ]

    assert result == expected
  }

  @Test
  public void shouldRemoveInheritedValuesFromDescendantsIfDescendantsDoNotHaveValueExplicitlySet() {
    def changes = '''
---
  source: global.yaml
  set:
    myapp::version: 2
''';

    def result = generateFromYaml(hierarchy, changes, 'global.yaml', [
        'global.yaml': '',
        'myteam.yaml': 'myapp::version: 2',
        'myteam/stage.yaml': 'myapp::version: 2'
    ])

    def expected = [
        'global.yaml': ['myapp::version': 2],
        'myteam.yaml': [:],
        'myteam/stage.yaml': [:]
    ]

    assert result == expected
  }

  @Test
  public void shouldCalculateDescendantsInOrderFromNearestToFurthest() {
    def descendants = m.getAllDescendants(yaml.load('''
foo:
  -
    a:
      - bob
      - jannet: true
  - b
  -
    1:
      - bar
      -
        baz: blue
        biz: red
    2:
      - y
      - z
'''))

    assert ['foo', 'a', 'b', 1, 2, 'bob', 'jannet', 'bar', 'baz', 'biz', 'y', 'z', true, 'blue', 'red'] == descendants
  }
}

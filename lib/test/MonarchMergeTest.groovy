import io.github.alechenninger.monarch.Change
import io.github.alechenninger.monarch.Hierarchy
import io.github.alechenninger.monarch.Monarch
import org.junit.Test
import org.yaml.snakeyaml.Yaml

class MonarchMergeTest {
  def m = new Monarch()
  def yaml = new Yaml()

  def hierarchy = '''
global.yaml:
  myteam.yaml:
    myteam/stage.yaml
''';

  Map generateFromYaml(String hierarchy, String changes, String sourceToChange, Map data, Set mergeKeys) {
    return m.generateSources(
        Hierarchy.fromStringListOrMap(yaml.load(hierarchy)),
        yaml.loadAll(changes).collect { Change.fromMap(it as Map) },
        sourceToChange,
        data.with {
          it.each { entry ->
            entry.setValue(this.yaml.load(entry.getValue()))
          }
        }, mergeKeys)
  }

  @Test
  public void shouldConsiderMergedKeysValuesInherited() {
    def changes = '''
---
  source: global.yaml
  set:
    some::hash:
      global_value: true
---
  source: myteam.yaml
  set:
    some::hash:
      myteam_value: false
'''

    def result = generateFromYaml(hierarchy, changes, 'myteam.yaml', [
        'global.yaml': '',
        'myteam.yaml': '',
        'myteam/stage.yaml': ''
    ], ['some::hash'] as Set)

    def expected = [
        'global.yaml': [:],
        'myteam.yaml': [
            'some::hash': [
                'global_value': true,
                'myteam_value': false
            ]
        ],
        // This test typically fails with this source containing some part of the hash unnecessarily
        'myteam/stage.yaml': [:]
    ]

    assert result == expected
  }

  @Test
  public void shouldMergeEntireAncestryForKeyInMergeList() {
    def changes = '''
---
  source: global.yaml
  set:
    some::hash:
      global_value: true
---
  source: myteam.yaml
  set:
    some::hash:
      myteam_value: false
---
  source: myteam/stage.yaml
  set:
    some::hash:
      stage_value: true
'''

    def result = generateFromYaml(hierarchy, changes, 'myteam/stage.yaml', [
        'global.yaml': '',
        'myteam.yaml': '',
        'myteam/stage.yaml': ''
    ], ['some::hash'] as Set)

    def expected = [
        'global.yaml': [:],
        'myteam.yaml': [:],
        'myteam/stage.yaml': [
            'some::hash': [
                'global_value': true,
                'myteam_value': false,
                'stage_value': true
            ]
        ]
    ]

    assert result == expected
  }

  @Test
  public void shouldRemoveRedundantValuesWhenTheyAreMergedInFromHierarchy() {
    def changes = '''
---
  source: global.yaml
  set:
    some::hash:
      global_value: true
---
  source: myteam.yaml
  set:
    some::hash:
      myteam_value: false
'''

    def result = generateFromYaml(hierarchy, changes, 'global.yaml', [
        'global.yaml': '',
        'myteam.yaml': '''
some::hash:
  myteam_value: false
  global_value: true
''',
        'myteam/stage.yaml': ''
    ], ['some::hash'] as Set)

    def expected = [
        'global.yaml': [
            'some::hash': [
                'global_value': true,
            ]
        ],
        'myteam.yaml': [
            'some::hash': [
                'myteam_value': false
            ]
        ],
        // This test typically fails with this source containing some part of the hash unnecessarily
        'myteam/stage.yaml': [:]
    ]

    assert result == expected
  }
}

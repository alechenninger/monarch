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
        Hierarchy.fromStringListOrMap(yaml.load(hierarchy)).sourceFor(sourceToChange).get(),
        yaml.loadAll(changes).collect { Change.fromMap(it as Map) }
        ,
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

  @Test
  public void shouldReplacePresentValueInMergeKeyWithNewValue() {
    def currentData = [
        'global.yaml': '',
        'myteam.yaml': '''
mergekey:
  somevalue: 1
  want_to_change: 2
''',
        'myteam/stage.yaml': ''
    ]

    def change = '''
---
  source: global.yaml
  set:
    mergekey:
      somevalue: 1
---
  source: myteam.yaml
  set:
    mergekey:
      want_to_change: x
'''

    def result = generateFromYaml(hierarchy, change, 'myteam.yaml', currentData, ['mergekey'] as Set)

    def expected = [
        'global.yaml': [:],
        'myteam.yaml': [
            'mergekey': [
                'somevalue': 1,
                'want_to_change': 'x'
            ]
        ],
        'myteam/stage.yaml': [:]
    ]

    assert result == expected
  }
}

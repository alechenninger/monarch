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

class MonarchTest {
  def m = new Monarch()
  def yaml = new Yaml()

  def hierarchy = '''
global.yaml:
  myteam.yaml:
    myteam/stage.yaml
''';

  Map generateFromYaml(String hierarchy, String changes, String sourceToChange, Map data) {
    return m.generateSources(
        Hierarchy.fromStringListOrMap(yaml.load(hierarchy)),
        yaml.loadAll(changes).collect { Change.fromMap(it as Map) },
        sourceToChange,
        data.with {
          it.each { entry ->
            entry.setValue(this.yaml.load(entry.getValue()))
          }
        }, new HashSet<>())
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
  public void shouldNotAddInheritedValuesIfNotExplicitlySet() {
    def changes = '''
---
  source: myteam.yaml
  set:
    myapp::version: 2
''';

    def result = generateFromYaml(hierarchy, changes, 'myteam.yaml', [
        'global.yaml': '',
        'myteam.yaml': 'myapp::version: 2',
        'myteam/stage.yaml': ''
    ])

    def expected = [
        'global.yaml': [:],
        'myteam.yaml': ['myapp::version': 2],
        'myteam/stage.yaml': [:]
    ]

    assert result == expected
  }

  @Test
  public void shouldNotRemoveInheritedValuesFromDescendantsIfDescendantsHaveValueExplicitlySet() {
    def changes = '''
---
  source: global.yaml
  set:
    myapp::version: 2
---
  source: myteam/stage.yaml
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
        'myteam/stage.yaml': ['myapp::version': 2]
    ]

    assert result == expected
  }

  @Test
  public void shouldDoNothingIfChangeAlreadyApplied() {
    def changes = '''
---
  source: myteam.yaml
  set:
    myapp::version: 2
''';

    def result = generateFromYaml(
        hierarchy, changes, 'myteam/stage.yaml', [
        'global.yaml': '',
        'myteam.yaml': '',
        'myteam/stage.yaml': 'myapp::version: 2'
    ]);

    def expected = [
        'global.yaml': [:],
        'myteam.yaml': [:],
        'myteam/stage.yaml': yaml.load('myapp::version: 2')]

    assert result == expected
  }
}

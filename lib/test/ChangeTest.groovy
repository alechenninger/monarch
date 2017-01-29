import io.github.alechenninger.monarch.Change
import org.junit.Test
import org.yaml.snakeyaml.Yaml

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

class ChangeTest {
  def yaml = new Yaml()

  @Test
  void parsesMapWithSourceByPath() {
    assert [Change.forPath('test1.yaml', ['testset': 1], ['testremove'])] ==
        Change.fromMap(yaml.load('''
source: test1.yaml
set:
  testset: 1
remove:
  - testremove
''') as Map)
  }

  @Test
  void parsesMapWithSourceByVariables() {
    assert [Change.forVariables(['color': 'red', 'number': '2'], ['testset': 1], ['testremove'])] ==
        Change.fromMap(yaml.load('''
source:
  number: '2'
  color: red
set:
  testset: 1
remove:
  - testremove
''') as Map)
  }

  @Test
  void expandsBracesInSourcePath() {
    def changes = Change.fromMap(yaml.load('''
source: test{1,2,3}.yaml
set:
  testset: 1
remove:
  - testremove
''') as Map)

    assert changes == [
        Change.forPath('test1.yaml', ['testset': 1], ['testremove']),
        Change.forPath('test2.yaml', ['testset': 1], ['testremove']),
        Change.forPath('test3.yaml', ['testset': 1], ['testremove']),
    ]
  }

  @Test
  void expandsBracesInSourceVariableNames() {
    def changes = Change.fromMap(yaml.load('''
source:
  '{a,b,c}': test
set:
  testset: 1
remove:
  - testremove
''') as Map)

    assert changes == [
        Change.forVariables(['a': 'test', 'b': 'test', 'c': 'test'], ['testset': 1], ['testremove']),
    ]
  }

  @Test
  void expandsBracesInSourceVariableValues() {
    def changes = Change.fromMap(yaml.load('''
source:
  test: '{a,b,c}'
set:
  testset: 1
remove:
  - testremove
''') as Map)

    assert changes == [
        Change.forVariables(['test': 'a'], ['testset': 1], ['testremove']),
        Change.forVariables(['test': 'b'], ['testset': 1], ['testremove']),
        Change.forVariables(['test': 'c'], ['testset': 1], ['testremove']),
    ]
  }

  @Test
  void expandsBracesInSourceVariableNamesAndValues() {
    def changes = Change.fromMap(yaml.load('''
source:
  test{1..2}: '{a,b}'
  another_{a..b}: '{x,y}'
  static: foo
set:
  testset: 1
remove:
  - testremove
''') as Map)

    assert changes == [
        Change.forVariables(['test1': 'a', 'test2': 'a', 'another_a': 'x', 'another_b': 'x', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'b', 'test2': 'a', 'another_a': 'x', 'another_b': 'x', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'a', 'test2': 'b', 'another_a': 'x', 'another_b': 'x', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'b', 'test2': 'b', 'another_a': 'x', 'another_b': 'x', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'a', 'test2': 'a', 'another_a': 'y', 'another_b': 'x', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'b', 'test2': 'a', 'another_a': 'y', 'another_b': 'x', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'a', 'test2': 'b', 'another_a': 'y', 'another_b': 'x', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'b', 'test2': 'b', 'another_a': 'y', 'another_b': 'x', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'a', 'test2': 'a', 'another_a': 'x', 'another_b': 'y', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'b', 'test2': 'a', 'another_a': 'x', 'another_b': 'y', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'a', 'test2': 'b', 'another_a': 'x', 'another_b': 'y', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'b', 'test2': 'b', 'another_a': 'x', 'another_b': 'y', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'a', 'test2': 'a', 'another_a': 'y', 'another_b': 'y', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'b', 'test2': 'a', 'another_a': 'y', 'another_b': 'y', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'a', 'test2': 'b', 'another_a': 'y', 'another_b': 'y', 'static': 'foo'], ['testset': 1], ['testremove']),
        Change.forVariables(['test1': 'b', 'test2': 'b', 'another_a': 'y', 'another_b': 'y', 'static': 'foo'], ['testset': 1], ['testremove']),
    ]
  }
}

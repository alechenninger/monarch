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

import io.github.alechenninger.monarch.Hierarchy
import org.junit.Test
import org.yaml.snakeyaml.Yaml

class StaticHierarchyTest {
  def yaml = new Yaml()

  @Test
  public void shouldCalculateDescendantsInOrderFromNearestToFurthest() {
    def descendants = Hierarchy.fromStringOrSingleKeyMap(yaml.load('''
foo:
  -
    a:
      - bob
      - jannet: 'true'
  - b
  -
    '1':
      - bar
      -
        baz: blue
        biz: red
    '2':
      - y
      - z
''')).descendants()

    assert ['foo', 'a', 'b', '1', '2', 'bob', 'jannet', 'bar',
            'baz', 'biz', 'y', 'z', 'true', 'blue', 'red'] == descendants
  }

  @Test
  public void shouldParseMapWithMultipleTopLevelNodes() {
    def hierarchy = Hierarchy.fromStringOrSingleKeyMap(yaml.load('''
a:
  - a1
  - a2
b:
  - b1
  - b2
'''))

    assert ['a', 'b', 'a1', 'a2', 'b1', 'b2'] == hierarchy.descendants()
  }
}

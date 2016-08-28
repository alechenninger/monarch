/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2016  Alec Henninger
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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import java.nio.file.FileSystems
import java.nio.file.Files

@RunWith(JUnit4.class)
class DefaultMonarchParsersTest {
  def fs = Jimfs.newFileSystem()
  def parsers = new MonarchParsers.Default()

  void writeFile(String file, String data) {
    def path = fs.getPath(file)
    def parent = path.getParent()

    if (parent != null) {
      Files.createDirectories(parent)
    }

    Files.write(path, data.getBytes('UTF-8'))
  }

  @Test
  void shouldParseParseableMap() {
    assert parsers.parseMap("foo: bar", FileSystems.default) == ['foo': 'bar']
  }

  @Test
  void shouldParsePathAsMap() {
    writeFile('/etc/test.yaml', 'foo: bar')
    assert parsers.parseMap('/etc/test.yaml', fs) == ['foo': 'bar']
  }

  @Test
  void shouldParseParseableHierarchy() {
    assert parsers.parseHierarchy('''
foo:
  - bar
  - baz:
    - buzz
''', fs) == Hierarchy.fromStringListOrMap(['foo': ['bar', ['baz': 'buzz']]])
  }

  @Test
  void shouldParsePathAsHierarchy() {
    writeFile('/etc/hierarchy.yaml', '''
foo:
  - bar
  - baz:
    - buzz
''')

    assert parsers.parseHierarchy('/etc/hierarchy.yaml', fs) ==
        Hierarchy.fromStringListOrMap(['foo': ['bar', ['baz': 'buzz']]])
  }

  @Test
  void shouldParsePathToMissingFileAsEmptyMap() {
    assert parsers.parseMap("/etc/not_a_thing.yaml", fs) == [:]
  }

  @Test
  void shouldParseParseableData() {
    assert parsers.parseData("foo: bar", FileSystems.default).data() == ['foo': 'bar']
  }

  @Test
  void shouldParsePathAsData() {
    writeFile('/etc/test.yaml', 'foo: bar')
    assert parsers.parseData('/etc/test.yaml', fs).data() == ['foo': 'bar']
  }

  @Test
  void shouldParsePathToMissingFileAsEmptyData() {
    assert parsers.parseData("/etc/not_a_thing.yaml", fs).data() == [:]
  }
}

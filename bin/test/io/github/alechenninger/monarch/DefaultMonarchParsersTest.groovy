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

import com.google.common.io.CharStreams
import com.google.common.jimfs.Jimfs
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

import static org.junit.Assert.fail

@RunWith(JUnit4.class)
class DefaultMonarchParsersTest {
  def fs = Jimfs.newFileSystem()
  def dumperOptions = new DumperOptions().with {
    defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
    prettyFlow = true
    return it
  }
  def yaml = new Yaml(dumperOptions)
  def parsers = new MonarchParsers.Default(yaml)

  void writeFile(Path path, String data) {
    def parent = path.getParent()

    if (parent != null) {
      Files.createDirectories(parent)
    }

    Files.write(path, data.getBytes('UTF-8'))
  }

  void writeFile(String file, String data) {
    writeFile(fs.getPath(file), data)
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

  @Test
  void shouldNotRewriteSourceWhenAddingNewKeys() {
    def sourcePath = fs.getPath('/source.yaml')
    def existingData = '''# really important comment that I don't want removed
existing: 123

  # sassy comment about technical debt
#
'''
    writeFile(sourcePath, existingData)

    parsers.forPath(sourcePath).parseData(Files.newInputStream(sourcePath))
        .writeNew(['existing': 123, 'new': 'from monarch w/ <3'], Files.newOutputStream(sourcePath))

    def newData = CharStreams.toString(Files.newBufferedReader(sourcePath))

    assert newData.contains(existingData)
    assert [
        'existing': 123,
        'new': 'from monarch w/ <3'
    ] == parsers.forPath(sourcePath).parseData(Files.newInputStream(sourcePath)).data()
  }

  @Test
  void shouldNotRewriteSourceWhenAddingAdditionalManagedKeys() {
    def sourcePath = fs.getPath('/source.yaml')
    def existingData = '''# really important comment that I don't want removed
existing: 123

  # sassy comment about technical debt
#
'''
    writeFile(sourcePath, existingData)

    parsers.forPath(sourcePath).parseData(Files.newInputStream(sourcePath))
        .writeNew(['existing': 123, 'new': 'from monarch w/ <3'], Files.newOutputStream(sourcePath))
    parsers.forPath(sourcePath)
        .parseData(Files.newInputStream(sourcePath))
        .writeNew(
            ['existing': 123, 'new': 'from monarch w/ <3', 'additional': 456],
            Files.newOutputStream(sourcePath))

    def newData = CharStreams.toString(Files.newBufferedReader(sourcePath))

    assert newData.contains(existingData)
    assert [
        'existing': 123,
        'new': 'from monarch w/ <3',
        'additional': 456
    ] == parsers.forPath(sourcePath).parseData(Files.newInputStream(sourcePath)).data()
  }

  @Test
  void shouldFailToChangeKeysThatAreNotManagedByMonarch() {
    def sourcePath = fs.getPath('/source.yaml')
    def existingData = '''# really important comment that I don't want removed
existing: 123

  # sassy comment about technical debt
#
'''
    writeFile(sourcePath, existingData)

    try {
      parsers.forPath(sourcePath).parseData(Files.newInputStream(sourcePath))
          .writeNew(['existing': 456], Files.newOutputStream(sourcePath))
      fail("Expected exception")
    } catch (Exception expected) {}

    def newData = CharStreams.toString(Files.newBufferedReader(sourcePath))
    assert newData.isEmpty()
  }

  @Test
  void shouldFailToRemoveKeysThatAreNotManagedByMonarch() {
    def sourcePath = fs.getPath('/source.yaml')
    def existingData = '''# really important comment that I don't want removed
existing: 123

  # sassy comment about technical debt
#
'''
    writeFile(sourcePath, existingData)

    try {
      parsers.forPath(sourcePath).parseData(Files.newInputStream(sourcePath))
          .writeNew([:], Files.newOutputStream(sourcePath))
      fail("Expected exception")
    } catch (Exception expected) {}

    def newData = CharStreams.toString(Files.newBufferedReader(sourcePath))
    assert newData.isEmpty()
  }

  @Test
  void shouldHappilyChangeKeysThatAreManagedByMonarch() {
    def sourcePath = fs.getPath('/source.yaml')
    def existingData = '''# really important comment that I don't want removed
existing: 123

  # sassy comment about technical debt
#
'''
    writeFile(sourcePath, existingData)

    parsers.forPath(sourcePath).parseData(Files.newInputStream(sourcePath))
        .writeNew(['existing': 123, 'managed': 'from monarch w/ <3'], Files.newOutputStream(sourcePath))
    parsers.forPath(sourcePath)
        .parseData(Files.newInputStream(sourcePath))
        .writeNew(
        ['existing': 123, 'managed': 'monarch is my favorite'],
        Files.newOutputStream(sourcePath))

    def newData = CharStreams.toString(Files.newBufferedReader(sourcePath))

    assert newData.contains(existingData)
    assert [
        'existing': 123,
        'managed': 'monarch is my favorite',
    ] == parsers.forPath(sourcePath).parseData(Files.newInputStream(sourcePath)).data()
  }

  @Test
  void shouldHappilyRemoveKeysThatAreManagedByMonarch() {
    def sourcePath = fs.getPath('/source.yaml')
    def existingData = '''# really important comment that I don't want removed
existing: 123

  # sassy comment about technical debt
#
'''
    writeFile(sourcePath, existingData)

    parsers.forPath(sourcePath).parseData(Files.newInputStream(sourcePath))
        .writeNew(['existing': 123, 'new': 'from monarch w/ <3'], Files.newOutputStream(sourcePath))
    parsers.forPath(sourcePath)
        .parseData(Files.newInputStream(sourcePath))
        .writeNew(
        ['existing': 123],
        Files.newOutputStream(sourcePath))

    def newData = CharStreams.toString(Files.newBufferedReader(sourcePath))

    assert newData.contains(existingData)
    assert [
        'existing': 123,
    ] == parsers.forPath(sourcePath).parseData(Files.newInputStream(sourcePath)).data()
  }

  @Test
  void shouldNotRewriteSourceWhenChangingManagedKeys() {

  }

  @Test
  void shouldNotRewriteSourceWhenRemovingManagedKeys() {

  }

  @Test
  void shouldMaintainUnmanagedSourceBeforeAndAfterManagedPortion() {

  }
}

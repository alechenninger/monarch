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

import java.nio.file.*

import static org.junit.Assert.fail

@RunWith(JUnit4.class)
class DefaultDataFormatsTest {
  def fs = Jimfs.newFileSystem()
  def dumperOptions = new DumperOptions().with {
    defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
    prettyFlow = true
    return it
  }
  def yaml = new Yaml(dumperOptions)
  def parsers = new DataFormats.Default(yaml)

  void writeFile(Path path, String data, OpenOption... options) {
    def parent = path.getParent()

    if (parent != null) {
      Files.createDirectories(parent)
    }

    Files.write(path, data.getBytes('UTF-8'), options)
  }

  void writeFile(String file, String data, OpenOption... options) {
    writeFile(fs.getPath(file), data, options)
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
    ] == yaml.load(Files.newBufferedReader(sourcePath))
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
    ] == yaml.load(Files.newBufferedReader(sourcePath))
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
    assert newData.empty
  }

  @Test
  void shouldHappilyCreateMonarchManagedSource() {
    def sourcePath = fs.getPath('/source.yaml')

    parsers.forPath(sourcePath).newSourceData()
        .writeNew(['new': 'from monarch w/ <3'], Files.newOutputStream(sourcePath))

    assert ['new': 'from monarch w/ <3'] == yaml.load(Files.newBufferedReader(sourcePath))
  }

  @Test
  void shouldHappilyChangeKeysThatAreManagedByMonarch() {
    def sourcePath = fs.getPath('/source.yaml')

    parsers.forPath(sourcePath).newSourceData()
        .writeNew(['managed': 'from monarch w/ <3'], Files.newOutputStream(sourcePath))
    parsers.forPath(sourcePath)
        .parseData(Files.newInputStream(sourcePath))
        .writeNew(['managed': 'monarch is my favorite'], Files.newOutputStream(sourcePath))

    assert ['managed': 'monarch is my favorite'] == yaml.load(Files.newBufferedReader(sourcePath))
  }

  @Test
  void shouldHappilyRemoveKeysThatAreManagedByMonarch() {
    def sourcePath = fs.getPath('/source.yaml')

    parsers.forPath(sourcePath).newSourceData()
        .writeNew(['managed': 'from monarch w/ <3'], Files.newOutputStream(sourcePath))
    parsers.forPath(sourcePath)
        .parseData(Files.newInputStream(sourcePath))
        .writeNew([:], Files.newOutputStream(sourcePath))

    assert [:] == (yaml.load(Files.newBufferedReader(sourcePath)) ?: [:])
  }

  @Test
  void shouldNotRewriteSourceWhenChangingManagedKeys() {
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
    ] == yaml.load(newData)
  }

  @Test
  void shouldNotRewriteSourceWhenRemovingManagedKeys() {
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
    ] == yaml.load(newData)
  }

  @Test
  void shouldMaintainUnmanagedSourceBeforeAndAfterManagedPortion() {
    def sourcePath = fs.getPath('/source.yaml')
    def preData = '''# really important comment that I don't want removed
existing: 123

  # sassy comment about technical debt
#
'''
    def postData = '''# stuff
post:  true'''

    writeFile(sourcePath, preData)
    parsers.forPath(sourcePath)
        .parseData(Files.newInputStream(sourcePath))
        .writeNew(
            ['existing': 123, 'managed': 'from monarch w/ <3', 'post': true],
            Files.newOutputStream(sourcePath))
    writeFile(sourcePath, postData, StandardOpenOption.WRITE, StandardOpenOption.APPEND)

    parsers.forPath(sourcePath)
        .parseData(Files.newInputStream(sourcePath))
        .writeNew(
            ['existing': 123, 'managed': 'monarch is my favorite', 'post': true],
            Files.newOutputStream(sourcePath))

    def newData = CharStreams.toString(Files.newBufferedReader(sourcePath))

    assert newData.contains(preData)
    assert newData.contains(postData)
    assert [
        'existing': 123,
        'managed': 'monarch is my favorite',
        'post': true,
    ] == yaml.load(newData)
  }

  @Test
  void shouldKeepDuplicatedKeyInUnmanagedDataAndRemoveFromManagedData() {
    def sourcePath = fs.getPath('/source.yaml')

    parsers.forPath(sourcePath).newSourceData()
        .writeNew(['key': 'value'], Files.newOutputStream(sourcePath))

    writeFile(sourcePath, '\n# test\nkey: value', StandardOpenOption.WRITE, StandardOpenOption.APPEND)

    parsers.forPath(sourcePath).parseData(Files.newInputStream(sourcePath))
        .writeNew(['key': 'value'], Files.newOutputStream(sourcePath))

    def written = CharStreams.toString(Files.newBufferedReader(sourcePath))

    assert written.contains('# test\nkey: value')
    assert written.count('key') == 1
    assert ['key': 'value'] == yaml.load(written)
  }

  @Test
  void shouldFailToChangeOrRemoveKeysWhichAreDuplicatedInManagedAndUnmanagedData() {
    def sourcePath = fs.getPath('/source.yaml')

    parsers.forPath(sourcePath).newSourceData()
        .writeNew(['key': 'value'], Files.newOutputStream(sourcePath))

    writeFile(sourcePath, '\n# test\nkey: value', StandardOpenOption.WRITE, StandardOpenOption.APPEND)

    try {
      parsers.forPath(sourcePath).parseData(Files.newInputStream(sourcePath))
          .writeNew(['key': 'new'], Files.newOutputStream(sourcePath))
      fail("Expected exception")
    } catch (Exception expected) {}

    def written = CharStreams.toString(Files.newBufferedReader(sourcePath))
    assert written.empty
  }
}

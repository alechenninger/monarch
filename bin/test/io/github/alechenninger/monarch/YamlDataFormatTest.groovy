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
import io.github.alechenninger.monarch.yaml.YamlConfiguration
import io.github.alechenninger.monarch.yaml.YamlDataFormat
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.yaml.snakeyaml.Yaml

import java.nio.file.Files

@RunWith(Enclosed.class)
class YamlDataFormatTest {
  static class ByDefault {
    def parser = new YamlDataFormat()

    @Test
    void shouldTolerateEmptyYamlDocumentsWhenParseChangeset() {
      def changesetInput = new ByteArrayInputStream('\n---\n'.getBytes("UTF-8"))

      Iterable<Change> parsed = parser.parseChanges(changesetInput)

      assert parsed.iterator().hasNext() == false
    }
  }

  static class WithIsolateNever {
    def fs = Jimfs.newFileSystem()
    def yaml = new Yaml()
    def parser = new YamlDataFormat(new YamlConfiguration() {
      YamlConfiguration.Isolate updateIsolation() { YamlConfiguration.Isolate.NEVER }
    })

    @Test
    void shouldAllowUpdatesToUnmanagedKeys() {
      def sourcePath = fs.getPath('/source.yaml')
      Files.write(sourcePath, 'unmanaged: 123'.bytes)
      parser.parseData(Files.newInputStream(sourcePath))
          .writeUpdate(['unmanaged': 456], Files.newOutputStream(sourcePath))
      def updated = new String(Files.readAllBytes(sourcePath))
      assert 1 == updated.count('unmanaged')
      assert ['unmanaged': 456] == yaml.load(updated)
    }

    @Test
    void shouldSortOutput() {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      parser.newSourceData().writeUpdate([
          'xyz': 1,
          'abc': 2,
          'a': 3,
          'fgh': 4,
          'cde': 5,
          'lmnop': 6
      ], out)

      assert new String(out.toByteArray()).contains('''a: 3
abc: 2
cde: 5
fgh: 4
lmnop: 6
xyz: 1''')
    }
  }

  static class WithIsolateAlways {
    def fs = Jimfs.newFileSystem()
    def parser = new YamlDataFormat(new YamlConfiguration() {
      YamlConfiguration.Isolate updateIsolation() { YamlConfiguration.Isolate.ALWAYS }
    })

    @Test(expected = MonarchException.class)
    void shouldNotAllowUpdatesToUnmanagedKeys() {
      def sourcePath = fs.getPath('/source.yaml')
      Files.write(sourcePath, 'unmanaged: 123'.bytes)
      parser.parseData(Files.newInputStream(sourcePath))
          .writeUpdate(['unmanaged': 456], Files.newOutputStream(sourcePath))
    }

    @Test
    void shouldSortOutput() {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      parser.newSourceData().writeUpdate([
          'xyz': 1,
          'abc': 2,
          'a': 3,
          'fgh': 4,
          'cde': 5,
          'lmnop': 6
      ], out)

      assert new String(out.toByteArray()).contains('''a: 3
abc: 2
cde: 5
fgh: 4
lmnop: 6
xyz: 1''')
    }
  }
}

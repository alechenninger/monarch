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

package io.github.alechenninger.monarch

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

import java.nio.file.FileSystems

import static io.github.alechenninger.monarch.MonarchOptionsFromSerializableConfig.Config

@RunWith(JUnit4.class)
class ConfigSerializationTest {
  def yaml = new Yaml(new Constructor(Config.class));

  @Test
  public void shouldDeserialize() {
    Config config = (Config) yaml.load('''
hierarchy:
  foo:
    - baz
mergeKeys:
  - bar
''');

    def options = new MonarchOptionsFromSerializableConfig(config, FileSystems.default)

    assert options.hierarchy().get().descendants() == ['foo', 'baz']
    assert options.mergeKeys() == ['bar'] as Set
  }
}

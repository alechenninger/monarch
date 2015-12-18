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

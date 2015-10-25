import org.junit.Test
import org.yaml.snakeyaml.Yaml

class HierarchyTest {
  def yaml = new Yaml()

  @Test
  public void shouldCalculateDescendantsInOrderFromNearestToFurthest() {
    def descendants = Hierarchy.fromStringListOrMap(yaml.load('''
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
}

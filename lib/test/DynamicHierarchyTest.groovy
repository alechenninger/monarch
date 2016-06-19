import io.github.alechenninger.monarch.DynamicHierarchy
import io.github.alechenninger.monarch.DynamicHierarchy.SimpleDynamicSource
import io.github.alechenninger.monarch.DynamicHierarchy.SimpleDynamicSource.Part
import org.junit.Test

class DynamicHierarchyTest {
  def hierarchy = new DynamicHierarchy(
      [
          new SimpleDynamicSource([Part.string("common")]),
          new SimpleDynamicSource([Part.variable("os")]),
          new SimpleDynamicSource([Part.string("environment/"), Part.variable("environment")]),
          new SimpleDynamicSource([Part.string("teams/"), Part.variable("team"),
                                  Part.string("/"), Part.variable("environment")]),
          new SimpleDynamicSource([Part.string("nodes/"), Part.variable("hostname")]),
      ],
      [
          "hostname": ["foo.com", "bar.com"],
          "team": ["teamA", "teamB"],
          "environment": ["qa", "prod"],
          "os": ["rhel"],
      ]
  )

  @Test
  void shouldCalculateDescendantsFromPotentialValues() {
    assert hierarchy.descendants() == [
        "common",
        "rhel",
        "environment/qa",
        "environment/prod",
        "teams/teamA/qa",
        "teams/teamB/qa",
        "teams/teamA/prod",
        "teams/teamB/prod",
        "nodes/foo.com",
        "nodes/bar.com",
    ]
  }

  @Test
  void shouldCalculateAncestorsByExactSource() {
    assert hierarchy.ancestorsOf("teams/teamA/qa").get() == [
        "teams/teamA/qa",
        "environment/qa",
        "common",
    ]
  }

  @Test
  void shouldCalculateAncestorsByCompleteVariables() {
    assert hierarchy.ancestorsOf(["team": "teamA", "environment": "qa", "os": "rhel"]).get() == [
        "teams/teamA/qa",
        "environment/qa",
        "rhel",
        "common",
    ]
  }

  @Test
  // TODO: Not sure if this should include dynamic source with only one potential value
  // Question of: is potentials expected to be comprehensive when a potential value is that that
  // variable is absent or that it is never absent? I'm thinking if it's not absent, then you should
  // supply the variable.
  void shouldNotIncludeSourcesInAncestryWithAbsentVariables() {
    assert hierarchy.ancestorsOf(["hostname": "foo.com"]).get() == ["nodes/foo.com", "common"]
  }
}

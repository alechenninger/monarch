import io.github.alechenninger.monarch.DynamicHierarchy
import io.github.alechenninger.monarch.DynamicHierarchy.SimpleDynamicSource
import io.github.alechenninger.monarch.DynamicHierarchy.SimpleDynamicSource.Part
import org.junit.Test

class DynamicHierarchyTest {
  def hierarchy = new DynamicHierarchy(
      [
          new SimpleDynamicSource([Part.string("environment/"), Part.variable("environment")]),
          new SimpleDynamicSource([Part.string("teams/"), Part.variable("team"),
                                  Part.string("/"), Part.variable("environment")]),
          new SimpleDynamicSource([Part.string("nodes/"), Part.variable("hostname")]),
      ],
      [
          "hostname": ["foo.com", "bar.com"],
          "team": ["teamA", "teamB"],
          "environment": ["qa", "prod"]
      ]
  )

  @Test
  void shouldCalculateDescendantsFromPotentialValues() {
    assert hierarchy.descendants() == [
        "environment/qa",
        "environment/prod",
        "teams/teamA/qa",
        "teams/teamB/qa",
        "teams/teamA/prod",
        "teams/teamB/prod",
        "nodes/foo.com",
        "nodes/bar.com"
    ]
  }

  @Test
  void shouldCalculateAncestorsByExactSource() {
    assert hierarchy.ancestorsOf("teams/teamA/qa").get() == [
        "teams/teamA/qa",
        "environment/qa"
    ]
  }

  @Test
  void shouldCalculateAncestorsByCompleteVariables() {
    assert hierarchy.ancestorsOf(["team": "teamA", "environment": "qa"]).get() == [
        "teams/teamA/qa",
        "environment/qa"
    ]
  }

  @Test
  void shouldCalculateAncestorsByIncompleteVariables() {
    assert hierarchy.ancestorsOf(["team": "teamA"]).get() == [
        "teams/teamA/qa",
        "teams/teamA/prod",
        "environment/qa",
        "environment/prod"
    ]
  }
}

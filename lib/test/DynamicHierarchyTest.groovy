import io.github.alechenninger.monarch.DynamicHierarchy
import io.github.alechenninger.monarch.DynamicHierarchy.SimpleDynamicSource
import io.github.alechenninger.monarch.DynamicHierarchy.SimpleDynamicSource.Part
import io.github.alechenninger.monarch.Hierarchy
import org.junit.Test

class DynamicHierarchyTest {
  def hierarchy = new DynamicHierarchy(
      [
          new SimpleDynamicSource([Part.string("common")]),
          new SimpleDynamicSource([Part.variable("os")]),
          new SimpleDynamicSource([Part.string("environment/"), Part.variable("environment")]),
          new SimpleDynamicSource([Part.string("teams/"), Part.variable("team"),
                                   Part.string("/"), Part.variable("environment")]),
          new SimpleDynamicSource([Part.string("teams/"), Part.variable("team"),
                                   Part.string("/"), Part.variable("environment"), Part.string("/"),
                                   Part.variable("app")]),
          new SimpleDynamicSource([Part.string("nodes/"), Part.variable("hostname")]),
      ],
      [
          "hostname": ["foo.com", "bar.com"],
          "team": ["teamA", "teamB"],
          "environment": ["qa", "prod"],
          "app": ["store", "blog"],
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
        "teams/teamA/qa/store",
        "teams/teamB/qa/store",
        "teams/teamA/prod/store",
        "teams/teamB/prod/store",
        "teams/teamA/qa/blog",
        "teams/teamB/qa/blog",
        "teams/teamA/prod/blog",
        "teams/teamB/prod/blog",
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
  // Question of: is potentials expected to be comprehensive WRT when a variable may be potentially
  // absent or not? I'm thinking if it's not absent, then you should supply the variable.
  void shouldNotIncludeSourcesInAncestryWithAbsentVariables() {
    assert hierarchy.ancestorsOf(["hostname": "foo.com"]).get() == ["nodes/foo.com", "common"]
  }

  @Test
  void shouldCreateNewHierarchiesByExactSource() {
    assert hierarchy.hierarchyOf("teams/teamB/prod").get().descendants() == [
        "teams/teamB/prod",
        "teams/teamB/prod/store",
        "teams/teamB/prod/blog",
    ]
  }

  @Test
  void shouldCreateNewHierarchiesByIncompleteVariables() {
    assert hierarchy.hierarchyOf(["team": "teamA"]).get().descendants() == [
        "teams/teamA/qa",
        "teams/teamA/prod",
        "teams/teamA/qa/store",
        "teams/teamA/prod/store",
        "teams/teamA/qa/blog",
        "teams/teamA/prod/blog",
    ]
  }

  @Test
  // This is a bit of a weird case right now and there may be better behavior than this.
  // I think if someone really sets up their hierarchy this way, then 'foo' and 'bar' are probably
  // synonyms and it would be safe to put at the highest level even though technically it would
  // affect other 'bar' assignments than 'baz'.
  void shouldProduceEmptyHierarchyForSourceWithCompetingVariablePossibilities() {
    def hierarchy = Hierarchy.fromDynamicSources([
        new SimpleDynamicSource([Part.variable("foo")]),
        new SimpleDynamicSource([Part.variable("bar")]),
        new SimpleDynamicSource([Part.string("foo/"), Part.variable("foo")]),
        new SimpleDynamicSource([Part.string("bar/"), Part.variable("bar")]),
    ], [
        "foo": ["baz"],
        "bar": ["baz"],
    ], [:])

    assert hierarchy.descendantsOf("baz").get() == []
  }

  @Test
  void shouldTakeIntoAccountKnownArgumentsWhenWorkingWithAStaticSource() {
    def hierarchy = Hierarchy.fromDynamicSources([
        new SimpleDynamicSource([Part.variable("foo")]),
        new SimpleDynamicSource([Part.variable("bar")]),
        new SimpleDynamicSource([Part.string("foo/"), Part.variable("foo")]),
        new SimpleDynamicSource([Part.string("bar/"), Part.variable("bar")]),
    ], [
        "foo": ["baz"],
        "bar": ["baz"],
    ], [
        "foo": ""
    ])

    assert hierarchy.descendantsOf("baz").get() == ["baz", "bar/baz"]
  }
}

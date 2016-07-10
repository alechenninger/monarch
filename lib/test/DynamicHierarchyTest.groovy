import io.github.alechenninger.monarch.DynamicHierarchy
import io.github.alechenninger.monarch.DynamicHierarchy.SimpleDynamicSource
import io.github.alechenninger.monarch.DynamicHierarchy.SimpleDynamicSource.Part
import io.github.alechenninger.monarch.Hierarchy
import io.github.alechenninger.monarch.Source
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
    assert hierarchy.descendants().collect { it.path() } == [
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
    assert hierarchy.getSource("teams/teamA/qa").get().lineage().collect { it.path() } == [
        "teams/teamA/qa",
        "environment/qa",
        "common",
    ]
  }

  @Test
  void shouldCalculateAncestorsByCompleteVariables() {
    assert hierarchy.getSource(["team": "teamA", "environment": "qa", "os": "rhel"]).get()
        .lineage().collect { it.path() } == [
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
    assert hierarchy.getSource(["hostname": "foo.com"]).get()
        .lineage().collect { it.path() } == ["nodes/foo.com", "common"]
  }

  @Test
  void shouldCreateNewHierarchiesByExactSource() {
    assert hierarchy.getSource("teams/teamB/prod").get()
        .descendants().collect { it.path() } == [
        "teams/teamB/prod",
        "teams/teamB/prod/store",
        "teams/teamB/prod/blog",
    ]
  }

  @Test
  void shouldCreateNewHierarchiesByIncompleteVariables() {
    // Note: what's interesting about this case is that there are two peer, top-most targets
    // Both would be affected by changes.
    assert hierarchy.getSource(["team": "teamA"]).get()
        .descendants().collect { it.path() } == [
        "teams/teamA/qa",
        "teams/teamA/prod",
        "teams/teamA/qa/store",
        "teams/teamA/prod/store",
        "teams/teamA/qa/blog",
        "teams/teamA/prod/blog",
    ]
  }

  @Test
  void shouldCreateNewHierarchiesByCompleteVariables() {
    assert hierarchy.getSource(["environment": "qa"]).get().descendants().collect { it.path() } == [
        "environment/qa",
        "teams/teamA/qa",
        "teams/teamB/qa",
        "teams/teamA/qa/store",
        "teams/teamB/qa/store",
        "teams/teamA/qa/blog",
        "teams/teamB/qa/blog",
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
    ])

    assert hierarchy.getSource("baz").get().descendants() == []
  }

  @Test
  void shouldGetHighestPossibleSourceWithLowestPossibleSourcesVariableValues() {
    def hierarchy = Hierarchy.fromDynamicSources([
        new SimpleDynamicSource([Part.variable("foo")]),
        new SimpleDynamicSource([Part.variable("bar")]),
        new SimpleDynamicSource([Part.string("foo/"), Part.variable("foo")]),
        new SimpleDynamicSource([Part.string("constant")]),
        new SimpleDynamicSource([Part.string("bar/"), Part.variable("bar")]),
    ], [
        "foo": ["baz"],
        "bar": ["baz"],
    ])

    assert hierarchy.getSource(["bar": "baz"]).get()
        .descendants().collect { it.path() } == ["baz", "bar/baz"]
  }
}

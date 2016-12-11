import io.github.alechenninger.monarch.Hierarchy
import io.github.alechenninger.monarch.Potential
import org.junit.Test
import org.yaml.snakeyaml.Yaml

class DynamicHierarchyTest {
  def hierarchy = Hierarchy.fromStringListOrMap(new Yaml().load('''
sources:
  - common
  - "%{os}"
  - environment/%{environment}
  - teams/%{team}/%{environment}
  - teams/%{team}/%{environment}/%{app}
  - nodes/%{hostname}
potentials:
  hostname:
    - foo.com:
        team: teamA
        environment: prod
        os: rhel
    - bar.com
  team:
  - teamA:
      app: store
  - teamB
  environment:
  - qa
  - prod
  app:
  - store
  - blog
  os:
  - rhel
'''))

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
        // Since teamA implies app: store, there are no /team/teamA/*/blog descendants
        "teams/teamB/qa/blog",
        "teams/teamB/prod/blog",
        "nodes/foo.com",
        "nodes/bar.com",
    ]
  }

  @Test
  void shouldCalculateAncestorsByExactSource() {
    assert hierarchy.sourceFor("teams/teamA/qa").get().lineage().collect { it.path() } == [
        "teams/teamA/qa",
        "environment/qa",
        "common",
    ]
  }

  @Test
  void shouldNotReturnASourceForUnsatisfiableVariableCombination() {
    assert hierarchy.sourceFor(["team": "teamA", "environment": "qa", "os": "rhel"]) ==
        Optional.empty()
  }

  @Test
  void shouldCalculateAncestorsByCompleteVariables() {
    assert hierarchy.sourceFor(["team": "teamA", "environment": "qa"]).get()
        .lineage().collect { it.path() } == [
        "teams/teamA/qa",
        "environment/qa",
        "common",
    ]
  }

  @Test
  // TODO: Not sure if this should include dynamic source with only one potential value
  // Question of: is potentials expected to be comprehensive WRT when a variable may be potentially
  // absent or not? I'm thinking if it's not absent, then you should supply the variable.
  void shouldNotIncludeSourcesInAncestryWithAbsentVariables() {
    assert hierarchy.sourceFor(["hostname": "bar.com"]).get()
        .lineage().collect { it.path() } == ["nodes/bar.com", "common"]
  }

  @Test
  void shouldCreateNewHierarchiesByExactSource() {
    assert hierarchy.sourceFor("teams/teamB/prod").get()
        .descendants().collect { it.path() } == [
        "teams/teamB/prod",
        "teams/teamB/prod/store",
        "teams/teamB/prod/blog",
    ]
  }

  @Test
  // We could consider instead returning two Sources, one for each environment variable.
  // But this gets complicated actually (weird edge cases), so for now the algorithm is simple.
  void shouldNotReturnSourceIfNoOneSourceHasMatchingVariables() {
    assert hierarchy.sourceFor(["team": "teamA"]) == Optional.empty()
  }

  @Test
  void shouldCreateNewHierarchiesByCompleteVariables() {
    assert hierarchy.sourceFor(["environment": "qa"]).get().descendants().collect { it.path() } == [
        "environment/qa",
        "teams/teamA/qa",
        "teams/teamB/qa",
        "teams/teamA/qa/store",
        "teams/teamB/qa/store",
        "teams/teamB/qa/blog",
    ]
  }

  @Test
  // This is a bit of a weird case right now and there may be better behavior than this.
  // I think if someone really sets up their hierarchy this way, then 'foo' and 'bar' are probably
  // synonyms and it would be safe to put at the highest level even though technically it would
  // affect other 'bar' assignments than 'baz'.
  void shouldNotReturnSourceForPathWithCompetingVariablePossibilities() {
    def hierarchy = Hierarchy.fromDynamicSourceExpressions([
        "%{foo}",
        "%{bar}",
        "foo/%{foo}",
        "bar/%{bar}",
    ], [
        "foo": [Potential.of("baz")],
        "bar": [Potential.of("baz")],
    ])

    assert hierarchy.sourceFor("baz") == Optional.empty()
  }

  @Test
  void shouldGetHighestPossibleSourceWithLowestPossibleSourcesVariableValues() {
    def hierarchy = Hierarchy.fromDynamicSourceExpressions([
        "%{foo}",
        "%{bar}",
        "foo/%{foo}",
        "constant",
        "bar/%{bar}",
    ], [
        "foo": [Potential.of("baz")],
        "bar": [Potential.of("baz")],
    ])

    assert hierarchy.sourceFor(["bar": "baz"]).get()
        .descendants().collect { it.path() } == ["baz", "bar/baz"]
  }

  @Test
  void shouldUseImpliedValuesForLineage() {
    assert hierarchy.sourceFor(['hostname': 'foo.com']).get().lineage().collect { it.path() } == [
        "nodes/foo.com",
        "teams/teamA/prod/store",
        "teams/teamA/prod",
        "environment/prod",
        "rhel",
        "common",
    ]
  }

  @Test
  void shouldUseImpliedValuesForDescendants() {
    assert hierarchy.sourceFor(['environment': 'prod']).get().descendants().collect { it.path() } == [
        "environment/prod",
        "teams/teamA/prod",
        "teams/teamB/prod",
        "teams/teamA/prod/store",
        "teams/teamB/prod/store",
        "teams/teamB/prod/blog",
        "nodes/foo.com",
    ]
  }

  @Test
  void shouldIncludeAllDescendantsWhichImplyAVariableUsedInSource() {

  }

  @Test
  void shouldExcludeDescendantsWhichHaveConflictingImpliedValues() {
    def hierarchy = Hierarchy.fromStringListOrMap(new Yaml().load('''
sources:
  - common
  - "%{os}"
  - app/%{app}
  - environment/%{environment}
  - teams/%{team}/%{environment}
  - teams/%{team}/%{environment}/%{app}
  - nodes/%{hostname}
potentials:
  hostname:
    - foo.com:
        team: teamA
        environment: prod
        os: rhel
    - bar.com
  team:
  - teamA:
      app: store
  - teamB
  environment:
  - qa
  - prod
  app:
  - store
  - blog
  os:
  - rhel
'''))

    assert hierarchy.sourceFor(['app': 'blog']).get().descendants().collect { it.path() } == [
        "app/blog",
        "teams/teamB/qa/blog",
        "teams/teamB/prod/blog",
    ]
  }
}

import io.github.alechenninger.monarch.Assignable
import io.github.alechenninger.monarch.Hierarchy
import io.github.alechenninger.monarch.Inventory
import io.github.alechenninger.monarch.Source
import io.github.alechenninger.monarch.SourceSpec
import org.junit.Ignore
import org.junit.Test
import org.yaml.snakeyaml.Yaml

class DynamicHierarchyTest {
  def yaml = new Yaml()
  def hierarchy = Hierarchy.fromStringListOrMap(yaml.load('''
sources:
  - common
  - "%{os}"
  - environment/%{environment}
  - teams/%{team}
  - teams/%{team}/%{environment}
  - teams/%{team}/%{environment}/%{app}
  - nodes/%{hostname}
inventory:
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
    assert hierarchy.descendants()*.path() == [
        "common",
        "rhel",
        "environment/qa",
        "environment/prod",
        "teams/teamA",
        "teams/teamB",
        "teams/teamA/qa",
        "teams/teamA/prod",
        "teams/teamB/qa",
        "teams/teamB/prod",
        "teams/teamA/qa/store",
        "teams/teamA/prod/store",
        // Since teamA implies app: store, there are no /team/teamA/*/blog descendants
        "teams/teamB/qa/store",
        "teams/teamB/qa/blog",
        "teams/teamB/prod/store",
        "teams/teamB/prod/blog",
        "nodes/foo.com",
        "nodes/bar.com",
    ]
  }

  @Test
  void shouldCalculateAncestorsByExactSource() {
    assert hierarchy.sourceFor("teams/teamA/qa").get().lineage()*.path() == [
        "teams/teamA/qa",
        "teams/teamA",
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
        .lineage()*.path() == [
        "teams/teamA/qa",
        "teams/teamA",
        "environment/qa",
        "common",
    ]
  }

  @Test
  // TODO: Not sure if this should include dynamic source with only one assignable value
  // Question of: is assignables expected to be comprehensive WRT when a variable may be potentially
  // absent or not? I'm thinking if it's not absent, then you should supply the variable.
  void shouldNotIncludeSourcesInAncestryWithAbsentVariables() {
    assert hierarchy.sourceFor(["hostname": "bar.com"]).get()
        .lineage()*.path() == ["nodes/bar.com", "common"]
  }

  @Test
  void shouldCreateNewHierarchiesByExactSource() {
    assert hierarchy.sourceFor("teams/teamB/prod").get()
        .descendants()*.path() == [
        "teams/teamB/prod",
        "teams/teamB/prod/store",
        "teams/teamB/prod/blog",
    ]
  }

  @Test
  // We could consider instead returning two Sources, one for each environment variable.
  // But this gets complicated actually (weird edge cases), so for now the algorithm is simple.
  void shouldNotReturnSourceIfNoOneSourceHasMatchingVariables() {
    def hierarchy = Hierarchy.fromStringListOrMap(yaml.load('''
sources:
  - common
  - "%{os}"
  - environment/%{environment}
  - teams/%{team}/%{environment}
  - teams/%{team}/%{environment}/%{app}
  - nodes/%{hostname}
inventory:
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

    assert hierarchy.sourceFor(["team": "teamA"]) == Optional.empty()
  }

  @Test
  void shouldCreateNewHierarchiesByCompleteVariables() {
    assert hierarchy.sourceFor(["environment": "qa"]).get().descendants()*.path() == [
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
    ], Inventory.from([
        "foo": [Assignable.of("baz")],
        "bar": [Assignable.of("baz")],
    ]))

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
    ], Inventory.from([
        "foo": [Assignable.of("baz")],
        "bar": [Assignable.of("baz")],
    ]))

    assert hierarchy.sourceFor(["bar": "baz"]).get()
        .descendants()*.path() == ["baz", "bar/baz"]
  }

  @Test
  void shouldUseImpliedValuesForLineage() {
    assert hierarchy.sourceFor(['hostname': 'foo.com']).get().lineage()*.path() == [
        "nodes/foo.com",
        "teams/teamA/prod/store",
        "teams/teamA/prod",
        "teams/teamA",
        "environment/prod",
        "rhel",
        "common",
    ]
  }

  @Test
  void shouldUseImpliedValuesForDescendants() {
    assert hierarchy.sourceFor(['environment': 'prod']).get().descendants()*.path() == [
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
  void shouldNotIncludeDescendantsWhichDoNotImplyEnoughToBeIncluded() {
    def hierarchy = Hierarchy.fromStringListOrMap(yaml.load('''
sources:
  - environment/%{environment}
  - teams/%{team}/%{environment}
  - teams/%{team}/%{environment}/%{app}
  - nodes/%{team}/%{hostname}
potentials:
  hostname:
    - foo.com:
        team: teamA
        environment: prod
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
'''))

    assert hierarchy.sourceFor(['environment': 'prod', 'team': 'teamB']).get().descendants()*.path() == [
        "teams/teamB/prod",
        "teams/teamB/prod/store",
        "teams/teamB/prod/blog",
        // nodes/teamB/foo.com not included because foo.com not in teamB (implies teamA)
        // nodes/teamB/bar.com not included because not in prod
    ]
  }

  @Test
  void shouldExcludeDescendantsWhichHaveConflictingImpliedValues() {
    def hierarchy = Hierarchy.fromStringListOrMap(yaml.load('''
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

    assert hierarchy.sourceFor(['app': 'blog']).get().descendants()*.path() == [
        "app/blog",
        "teams/teamB/qa/blog",
        "teams/teamB/prod/blog",
    ]
  }

  @Test
  void shouldTargetAncestorOfSourceWithoutAllAssignmentsOfSource() {
    assert hierarchy.sourceFor(['hostname': 'foo.com']).get()
        .lineage().get(2)
        .isTargetedBy(SourceSpec.byVariables(['team': 'teamA', 'environment': 'prod']))
  }

  /**
   * Any hierarchy where a=foo should only touch sources where a=foo and no others. 'bottom' in this
   * example would also be used by other assignments of a, so we cannot alter 'bottom' without
   * altering other hierarchies we did not target.
   */
  @Test
  void shouldNotIncludeDescendantsWithoutVariables() {
    def hierarchy = Hierarchy.fromStringListOrMap(yaml.load('''
sources:
  - '%{a}'
  - bottom
potentials:
  a:
  - foo
'''))

    assert hierarchy.sourceFor(['a': 'foo']).get().descendants()*.path() == ['foo']
  }

  @Test
  void shouldNotIncludeASourceMoreThanOnceInDescendants() {
    def hierarchy = Hierarchy.fromStringListOrMap(yaml.load('''
sources:
  - top
  - '%{a}'
  - '%{b}'
  - etc/%{a}
  - etc/%{b}
potentials:
  a:
  - foo
  b:
  - foo:
      a: foo
'''))

    def bIsFoo = SourceSpec.byVariables(['b': 'foo'])
    def bFoo = hierarchy.sourceFor(bIsFoo).get()

    assert bFoo.descendants()*.path() == ['foo', 'etc/foo']
  }

  @Test
  void shouldNotFindTargetIsShadowedByLowerDuplicate() {
    def hierarchy = Hierarchy.fromStringListOrMap(yaml.load('''
sources:
  - top
  - '%{a}'
  - '%{b}'
  - etc/%{a}
  - etc/%{b}
potentials:
  a:
  - foo
  b:
  - foo:
      a: foo
'''))

    def aIsFoo = SourceSpec.byVariables(['a': 'foo'])

    assert !hierarchy.sourceFor(aIsFoo).isPresent()
  }

  @Test
  void shouldNotIncludeDuplicatePathsInLineage() {
    def hierarchy = Hierarchy.fromStringListOrMap(yaml.load('''
sources:
  - top
  - '%{a}'
  - middle
  - '%{b}'
  - '%{c}/%{b}'
potentials:
  a:
  - foo
  b:
  - foo:
      a: foo
  c:
  - bar:
      b: foo
'''))

    def cBar = hierarchy.sourceFor(['c': 'bar']).get()

    assert cBar.lineage()*.path() == ['bar/foo', 'foo', 'middle', 'top']
  }

  @Test
  void shouldNotIncludePathInLineageIfItIsDuplicatedInDescendants() {
    // This prevents a misleading lineage where really, because the path is duplicated in
    // descendants, the path will actually override (come before, below) this one, which means it is
    // not really an ancestor.

    def hierarchy = Hierarchy.fromStringListOrMap(yaml.load('''
sources:
  - top
  - '%{a}'
  - middle
  - '%{b}'
  - '%{c}/%{b}'
  - '%{c}'
potentials:
  a:
  - foo
  b:
  - bar:
      a: foo
  c:
  - foo:
      b: bar
'''))

    def bBar = hierarchy.sourceFor(['b': 'bar']).get()

    assert bBar.lineage()*.path() == ['bar', 'middle', 'top']
  }

  @Test
  // TODO: Not sure if this actually tests this
  void ancestorsDescendantsShouldBeRelativeToAncestor() {
    def hierarchy = Hierarchy.fromStringListOrMap(yaml.load('''
sources:
  - top
  - '%{a}'
  - '%{b}'
  - '%{c}/%{b}'
potentials:
  a:
  - foo
  b:
  - foo:
      a: foo
  c:
  - bar:
      b: foo
'''))

    def cBar = hierarchy.sourceFor(['c': 'bar']).get()

    assert cBar.lineage().get(2).descendants()*.path() == ['top', 'foo', 'bar/foo']
  }

  @Test
  void descendantsAncestorsShouldBeRelativeToDescendant() {
    def prod = hierarchy.sourceFor(['environment': 'prod']).get()
    def environmentUnderTeam = prod.descendants()[1]
    assert environmentUnderTeam.lineage()*.path() == [
        'teams/teamA/prod',
        'teams/teamA',
        'environment/prod',
        'common'
    ]
  }

  @Test
  void expandsBracesInInventory() {
    def withBraces = Hierarchy.fromStringListOrMap(yaml.load('''
sources:
  - '%{b1}\'
  - '%{aa}\'
  - '%{b2}\'
  - '%{ab}\'
  - '%{b3}\'
  - '%{ac}\'
inventory:
  a{a,b,c}:
  - foo{,bar}
  - xyz
  b{1..3}:
  - baz:
      a{b..c}: foo
'''))

    def manuallyExpanded = Hierarchy.fromStringListOrMap(yaml.load('''
sources:
  - '%{b1}\'
  - '%{aa}\'
  - '%{b2}\'
  - '%{ab}\'
  - '%{b3}\'
  - '%{ac}\'
inventory:
  aa:
  - foo
  - foobar
  - xyz
  ab:
  - foo
  - foobar
  - xyz
  ac:
  - foo
  - foobar
  - xyz
  b1:
  - baz:
      ab: foo
      ac: foo
  b2:
  - baz:
      ab: foo
      ac: foo
  b3:
  - baz:
      ab: foo
      ac: foo
'''))

    assert manuallyExpanded == withBraces
  }

  @Test
  void allowsEscapingBraceExpansionAndDoesNotExpandValuesInImpliedAssignments() {
    def withBraces = Hierarchy.fromStringListOrMap(yaml.load('''
sources:
  - '%{a}'
  - '%{b}'
inventory:
  a:
  - foo\\{,bar\\}
  b:
  - baz:
      a: foo{,bar}
'''))

    def manuallyExpanded = Hierarchy.fromDynamicSourceExpressions(
        ['%{a}', '%{b}'],
        Inventory.from([
            'a': [Assignable.of('foo{,bar}')],
            'b': [Assignable.of('baz', ['a': 'foo{,bar}'])]
        ])
    )

    assert manuallyExpanded == withBraces
  }

  @Test
  void expandsStringAssignmentWhenNotInAListOrMap() {
    def withBraces = Hierarchy.fromStringListOrMap(yaml.load('''
sources:
  - '%{a}'
inventory:
  a: test{,test}
'''))

    def manuallyExpanded = Hierarchy.fromDynamicSourceExpressions(
        ['%{a}'],
        Inventory.from(['a': [Assignable.of('test'), Assignable.of('testtest')]])
    )

    assert manuallyExpanded == withBraces
  }
}

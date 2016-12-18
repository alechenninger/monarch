/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2016 Alec Henninger
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

import io.github.alechenninger.monarch.Assignable
import io.github.alechenninger.monarch.Inventory
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith

@RunWith(Enclosed.class)
class AssignmentsTest {
  static class ShouldConflictWhenAssignment {
    @Test(expected = IllegalArgumentException.class)
    void impliesTransitivelyAConflictWithItsImplication() {
      def inventory = Inventory.from([
          'dessert': [Assignable.of('cookie', ['drink': 'milk'])],
          'drink': [Assignable.of('milk'), Assignable.of('coffee', ['dessert': 'cookie'])],
      ])

      inventory.assignAll(['drink': 'coffee'])
    }

    @Test(expected = IllegalArgumentException.class)
    void impliesConflictWithItself() {
      def inventory = Inventory.from([
          'drink': [Assignable.of('milk', ['drink': 'coffee']), Assignable.of('coffee')],
      ])

      inventory.assignAll(['drink': 'milk'])
    }
  }

  static class WithImplicitAssignments {
    def inventory = Inventory.from([
        'dessert': [Assignable.of('cookie', ['drink': 'milk'])],
        'drink': [Assignable.of('milk'), Assignable.of('coffee')],
    ])

    def assignments = inventory.assignAll(['dessert': 'cookie'])

    @Test
    void shouldIncludeImplicitAssignment() {
      assert 2 == assignments.size()
      assert assignments.toMap() == ['dessert': 'cookie', 'drink': 'milk']
      assert assignments.forVariable('drink').value() == 'milk'
    }

    @Test
    void shouldShouldNotBeAbleToAssignConflictWithImplicitAssignment() {
      assert assignments.conflictsWith('drink', 'coffee')
    }

    @Test(expected = IllegalArgumentException.class)
    void shouldFailToAssignConflictWithImplicitAssignment() {
      assignments.with('drink', 'coffee')
    }

    @Test
    void shouldNotBeAbleToForkAtImplicitAssignment() {
      assert !assignments.canForkAt('drink')
    }

    @Test(expected = IllegalArgumentException.class)
    void shouldFailToForkAtImplicitAssignment() {
      assignments.forkAt('drink')
    }
  }

  static class WithRedundantExplicitAndImplicitAssignment {
    def inventory = Inventory.from([
        'dessert': [Assignable.of('cookie', ['drink': 'milk'])],
        'drink': [Assignable.of('milk'), Assignable.of('coffee')],
    ])

    def assignments = inventory.assignAll(['dessert': 'cookie', 'drink': 'milk'])

    @Test
    void shouldNotBeAbleToForkAtRedundantAssignment() {
      assert !assignments.canForkAt('drink')
    }

    @Test(expected = IllegalArgumentException.class)
    void shouldFailToForkAtRedundantAssignment() {
      assignments.forkAt('drink')
    }

    @Test
    void shouldNotIncludeRedundantAssignmentTwice() {
      assert 2 == assignments.size()
      assert assignments*.variable().name.sort() == ['dessert', 'drink']
    }
  }
}

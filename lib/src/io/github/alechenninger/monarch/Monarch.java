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

package io.github.alechenninger.monarch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Monarch {
  private static final Logger log = LoggerFactory.getLogger(Monarch.class);

  /**
   * Generates new data for all known sources in the hierarchy based on the hierarchy, the data
   * changes you want applied, the existing state of the data, and a "target" source which you want
   * to change alongside all of its children.
   *
   * @param target A source in a hierarchy that represents a level that we can start to change
   *               values. Sources above this source will be untouched. This source and any below it
   *               will be updated to achieve the desired {@code changes}.
   * @param changes The "end-state" changes to be applied assuming you could update the entire tree.
   *                Even if you are not targeting the root of the tree (which would generate an
   *                entirely new tree), the {@code target} and its children will be updated
   *                such that each source's inherited values are what you want your end state to be,
   *                as described by the changes.
   * @param data A map of sources to key:value pairs representing the existing state of the data.
   * @param mergeKeys A list of keys whose flattened value is a <em>merge</em> of all of the values
   *                  in the hierarchy for that key, provided the values are all either
   *                  {@link Collection collections} or {@link Map maps}. Keys not in the list
   *                  use values from only the nearest in a sources ancestry.
   * @return A map of sources to key:value pairs representing the new state of the data with changes
   *         applied to the given {@code target} and its children.
   */
  public Map<String, Map<String, Object>> generateSources(Source target, Iterable<Change> changes,
      Map<String, Map<String, Object>> data, Set<String> mergeKeys) {
    Map<String, Map<String, Object>> result = copyMapAndValues(data);

    // From top-most to inner-most, generate results, taking into account the results from ancestors
    // as we go along.
    if (log.isDebugEnabled()) {
      log.debug("Generating sources for descendants: {}",
          target.descendants().stream().map(Source::path).collect(Collectors.toList()));
    }

    for (Source descendant : target.descendants()) {
      result.put(descendant.path(), generateSingleSource(descendant, changes, result, mergeKeys));
    }

    return result;
  }

  private Optional<Change> findChangeForSource(Source source, Iterable<Change> changes) {
    return StreamSupport.stream(changes.spliterator(), false)
        .filter(c -> source.isTargetedBy(c.sourceSpec()))
        .collect(Collect.maxOneResultOrThrow(() -> new IllegalArgumentException(
            "Expected at most one change with matching source in list of changes, but got: " +
                changes)));
  }

  /**
   * Generates new data for the given source only, taking into account the desired changes, the
   * existing hierarchy, and the existing data in the hierarchy.
   */
  private Map<String, Object> generateSingleSource(Source target, Iterable<Change> changes,
      Map<String, Map<String, Object>> data, Set<String> mergeKeys) {
    List<Source> lineage = target.lineage();

    DataLookup targetLookup = new DataLookupFromMap(data, target, mergeKeys);

    Map<String, Object> sourceData = data.get(target.path());
    Map<String, Object> resultSourceData = sourceData == null
        ? new HashMap<>()
        : new HashMap<>(sourceData);

    log.debug("Looking for changes applicable to lineage: {}", lineage);

    for (Source ancestor : new ListReversed<>(lineage)) {
      Optional<Change> maybeChange = findChangeForSource(ancestor, changes);

      if (!maybeChange.isPresent()) {
        continue;
      }

      Change change = maybeChange.get();

      log.debug("Applying change for ancestor {} (targeted by {}) to {}.",
          ancestor, change.sourceSpec(), target);

      for (Map.Entry<String, Object> setEntry : change.set().entrySet()) {
        String setKey = setEntry.getKey();
        Object setValue = setEntry.getValue();

        if (target.isNotTargetedBy(change.sourceSpec())) {
          if (targetLookup.isValueInherited(setKey, setValue)) {
            log.debug("Desired key:value is inherited at {}: {}: {}", target, setKey, setValue);

            if (resultSourceData.containsKey(setKey)) {
              if (mergeKeys.contains(setKey)) {
                log.debug("Target {} contains value for merged key '{}', unmerging '{}'",
                    target, setKey, setValue);
                Merger merger = Merger.startingWith(resultSourceData.get(setKey));
                merger.unmerge(setValue);
                resultSourceData.put(setKey, merger.getMerged());
              } else {
                log.debug("Target {} contains value for non-merged key {}, removing.",
                    target, setKey);
                resultSourceData.remove(setKey);
              }
            }
            continue;
          }
        }

        Object newValue;
        Object currentValue = resultSourceData.get(setKey);

        if (mergeKeys.contains(setKey) && currentValue != null) {
          log.debug("Target {} already contains merged key '{}', " +
                  "merging with desired value: {}",
              target, setKey, setValue);
          Merger merger = Merger.startingWith(currentValue);
          merger.merge(setValue);
          newValue = merger.getMerged();
        } else {
          newValue = setValue;
        }

        log.debug("Putting {}: {} in {}", setKey, newValue, target);
        resultSourceData.put(setKey, newValue);
      }

      // TODO: Support removing nested keys (keys in a hash)
      for (String key : change.remove()) {
        // TODO: log
        resultSourceData.remove(key);
      }
    }

    return resultSourceData;
  }

  private static Map<String, Map<String, Object>> copyMapAndValues(
      Map<String, Map<String, Object>> data) {
    Map<String, Map<String, Object>> copy = new HashMap<>();

    for (Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
      String key = entry.getKey();
      Map value = entry.getValue();
      copy.put(key, value == null ? new HashMap<>() : new HashMap<>(value));
    }

    return copy;
  }
}

/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2017 Alec Henninger
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

package io.github.alechenninger.monarch.set;

import io.github.alechenninger.monarch.Change;
import io.github.alechenninger.monarch.Hierarchy;
import io.github.alechenninger.monarch.MonarchException;
import io.github.alechenninger.monarch.Source;
import io.github.alechenninger.monarch.SourceSpec;
import io.github.alechenninger.monarch.util.MoreFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class UpdateSetService {
  private final Yaml yaml;

  private static final Logger log = LoggerFactory.getLogger(UpdateSetService.class);

  public UpdateSetService(Yaml yaml) {
    this.yaml = yaml;
  }

  public void updateSetInChange(UpdateSetOptions options) throws IOException {
    SourceSpec source = options.source()
        .orElseThrow(() -> MonarchException.missingOption("source"));
    Path outputPath = options.outputPath()
        .orElseThrow(() -> MonarchException.missingOption("output path"));

    updateSetInChange(source, outputPath, options.changes(), options.putInSet(),
        options.removeFromSet(), options.hierarchy());
  }

  public void updateSetInChange(SourceSpec source, Path outputPath, Iterable<Change> changes,
      Map<String, Object> toPut, Set<String> toRemove, Optional<Hierarchy> hierarchy)
      throws IOException {
    List<Change> outputChanges = StreamSupport.stream(changes.spliterator(), false)
        // Exclude change we're replacing
        .filter(c -> !c.sourceSpec().equals(source))
        .collect(Collectors.toCollection(ArrayList::new));

    // Change we will replace if present
    Optional<Change> sourceChange = StreamSupport.stream(changes.spliterator(), false)
        .filter(c -> source.equals(c.sourceSpec()))
        .findFirst();

    Map<String, Object> updatedSet = sourceChange.map(c -> new HashMap<>(c.set()))
        .orElse(new HashMap<>());
    updatedSet.putAll(toPut);
    updatedSet.keySet().removeAll(toRemove);

    Set<String> remove = sourceChange.map(Change::remove)
        .orElse(Collections.emptySet());

    // Add replacement change to output if it has any remaining content
    if (!updatedSet.isEmpty() || !remove.isEmpty()) {
      Change updatedSourceChange = source.toChange(updatedSet, remove);
      outputChanges.add(updatedSourceChange);
    }

    if (hierarchy.map(h -> !h.sourceFor(source).isPresent()).orElse(false)) {
      log.warn("Source not found in provided hierarchy. source={}", source);
    }

    // Sort by hierarchy depth if provided, else sort alphabetically
    Comparator<Change> changeComparator = hierarchy.map(h -> {
      List<String> descendants = h.allSources().stream()
          .map(Source::path)
          .collect(Collectors.toList());

      return (Comparator<Change>) (c1, c2) -> {
        // TODO make Source Comparable
        String c1Source = c1.sourceSpec().findSource(h).map(Source::path).orElse("");
        String c2Source = c2.sourceSpec().findSource(h).map(Source::path).orElse("");

        int c1Index = descendants.indexOf(c1Source);
        int c2Index = descendants.indexOf(c2Source);

        if (c1Index < 0 || c2Index < 0) {
          return c1Source.compareTo(c2Source);
        }

        return c1Index - c2Index;
      };
      // TODO maybe make change comparable too?
    }).orElse(Comparator.comparing(Change::toString));

    List<Map<String, Object>> serializableChanges = outputChanges.stream()
        .sorted(changeComparator)
        .map(Change::toMap)
        .collect(Collectors.toList());

    yaml.dumpAll(serializableChanges.iterator(), MoreFiles.createDirectoriesForWriter(outputPath));
  }
}

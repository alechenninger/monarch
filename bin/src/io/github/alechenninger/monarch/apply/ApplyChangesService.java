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

package io.github.alechenninger.monarch.apply;

import io.github.alechenninger.monarch.Change;
import io.github.alechenninger.monarch.DataFormats;
import io.github.alechenninger.monarch.DataFormatsConfiguration;
import io.github.alechenninger.monarch.Hierarchy;
import io.github.alechenninger.monarch.Monarch;
import io.github.alechenninger.monarch.MonarchException;
import io.github.alechenninger.monarch.Source;
import io.github.alechenninger.monarch.SourceData;
import io.github.alechenninger.monarch.SourceSpec;
import io.github.alechenninger.monarch.Targetable;
import io.github.alechenninger.monarch.util.MoreFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ApplyChangesService {
  private final DataFormats dataFormats;
  private final Monarch monarch;

  private static final Logger log = LoggerFactory.getLogger(ApplyChangesService.class);

  public ApplyChangesService(DataFormats dataFormats, Monarch monarch) {
    this.dataFormats = dataFormats;
    this.monarch = monarch;
  }

  public void applyChanges(ApplyChangesOptions options) throws IOException {
    Path outputDir = options.outputDir()
        .orElseThrow(() -> MonarchException.missingOption("output directory"));
    Path dataDir = options.dataDir()
        .orElseThrow(() -> MonarchException.missingOption("data directory"));
    Hierarchy hierarchy = options.hierarchy()
        .orElseThrow(() -> MonarchException.missingOption("hierarchy"));

    applyChanges(outputDir, hierarchy, options.target(), options.changes(), options.mergeKeys(),
        options.dataFormatsConfiguration(), dataDir);
  }

  private void applyChanges(Path outputDir, Hierarchy hierarchy, Optional<SourceSpec> targetSpec,
      Iterable<Change> changes, Set<String> mergeKeys,
      Optional<DataFormatsConfiguration> dataFormatsConfiguration, Path dataDir) {

    DataFormats configuredFormats = dataFormatsConfiguration
        .map(dataFormats::withConfiguration)
        .orElse(dataFormats);
    Map<String, SourceData> currentData =
        configuredFormats.parseDataSourcesInHierarchy(dataDir, hierarchy);

    for (Change change : changes) {
      checkChangeIsApplicable(hierarchy, change);
    }

    Targetable target = targetSpec.map(spec -> Targetable.of(
        hierarchy.sourceFor(spec).orElseThrow(() -> new IllegalArgumentException(
            "No source found in hierarchy which satisfies: " + targetSpec))))
        .orElse(Targetable.of(hierarchy));

    Set<String> affectedSources = target.descendants().stream()
        .map(Source::path)
        .collect(Collectors.toSet());
    // TODO: Consider currentSources of type Sources or something like that with getter for this
    Map<String, Map<String, Object>> currentData1 = currentData.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().data()));

    Map<String, Map<String, Object>> result =
        target.generateSources(monarch, changes, currentData1, mergeKeys);

    for (Map.Entry<String, Map<String, Object>> pathToData : result.entrySet()) {
      String path = pathToData.getKey();

      // We only output a source if it is target or under.
      if (!affectedSources.contains(path)) {
        continue;
      }

      Path outPath = outputDir.resolve(path);
      Map<String, Object> outData = pathToData.getValue();
      SourceData sourceData = currentData.containsKey(path)
          ? currentData.get(path)
          : dataFormats.forPath(outPath).newSourceData();

      if (sourceData.isEmpty() && outData.isEmpty()) {
        continue;
      }

      log.debug("Writing result source data for {} to {}", path, outPath);

      try {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        sourceData.writeUpdate(outData, out);
        MoreFiles.createDirectoriesAndWrite(outPath, out.toByteArray());
      } catch (Exception e) {
        log.error("Failed to write updated data source for " + path + " to " + outPath, e);
      }
    }
  }

  private static void checkChangeIsApplicable(Hierarchy hierarchy, Change change) {
    SourceSpec spec = change.sourceSpec();
    if (!spec.findSource(hierarchy).isPresent()) {
      log.warn("Source for change not found in hierarchy. You may want to check this " +
          "change's target for correctness: {}", spec);
    }
  }
}

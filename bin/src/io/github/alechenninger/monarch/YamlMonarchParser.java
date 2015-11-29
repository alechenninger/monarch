package io.github.alechenninger.monarch;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class YamlMonarchParser implements MonarchParser {
  private final Yaml yaml;

  public YamlMonarchParser(Yaml yaml) {
    this.yaml = Objects.requireNonNull(yaml, "yaml");
  }

  @Override
  public Hierarchy parseHierarchy(InputStream hierarchyInput) {
    Object parsedHierarchy = yaml.load(hierarchyInput);
    return Hierarchy.fromStringListOrMap(parsedHierarchy);
  }

  @Override
  public Iterable<Change> parseChanges(InputStream changesInput) {
    try {
      Iterable<Object> parsedChanges = yaml.loadAll(changesInput);
      List<Change> changes = new ArrayList<>();

      for (Object parsedChange : parsedChanges) {
        Map<String, Object> parsedAsMap = (Map<String, Object>) parsedChange;
        changes.add(Change.fromMap(parsedAsMap));
      }

      return changes;
    } catch (ClassCastException e) {
      throw new MonarchException("Expected changes yaml to parse as a map. See help for example.",
          e);
    }
  }

  @Override
  public Map<String, Map<String, Object>> readData(Collection<String> sources, Path dataDir) {
    Map<String, Map<String, Object>> data = new LinkedHashMap<>(sources.size());

    try {
      for (String source : sources) {
        Path sourcePath = dataDir.resolve(source);
        Map<String, Object> dataForSource = (Map<String, Object>) yaml.load(
            Files.newInputStream(sourcePath));
        data.put(source, dataForSource);
      }
    } catch (IOException e) {
      throw new MonarchException("Error reading data source.", e);
    } catch (ClassCastException e) {
      throw new MonarchException("Expected data source to parse as map.", e);
    }

    return data;
  }
}

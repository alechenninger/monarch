package io.github.alechenninger.monarch;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class YamlMonarchOptions implements MonarchOptions {
  private final String hierarchyPath;
  private final String changesPath;
  private final String pivotSource;
  private final String dataDir;
  private final Yaml yaml;

  public YamlMonarchOptions(String hierarchyPath, String changesPath, String pivotSource, String
      dataDir, Yaml yaml) {
    this.yaml = Objects.requireNonNull(yaml, "yaml");
    this.hierarchyPath = hierarchyPath;
    this.changesPath = changesPath;
    this.pivotSource = pivotSource;
    this.dataDir = dataDir;
  }

  @Override
  public Hierarchy hierarchy() {
    Path pathToHierarchy = Optional.ofNullable(hierarchyPath)
        .map(Paths::get)
        .orElseThrow(() -> new MonarchException("No hierarchy provided."));

    try {
      Object parsedHierarchy = yaml.load(Files.newInputStream(pathToHierarchy));
      return Hierarchy.fromStringListOrMap(parsedHierarchy);
    } catch (IOException e) {
      throw new MonarchException("Error reading hierarchy file.", e);
    }
  }

  @Override
  public Iterable<Change> changes() {
    Path pathToChanges = Optional.ofNullable(changesPath)
        .map(Paths::get)
        .orElseThrow(() -> new MonarchException("No changes provided."));

    try {
      Iterable<Object> parsedChanges = yaml.loadAll(Files.newInputStream(pathToChanges));
      List<Change> changes = new ArrayList<>();

      for (Object parsedChange : parsedChanges) {
        Map<String, Object> parsedAsMap = (Map<String, Object>) parsedChange;
        changes.add(Change.fromMap(parsedAsMap));
      }

      return changes;
    } catch (IOException e) {
      throw new MonarchException("Error reading hierarchy file.", e);
    } catch (ClassCastException e) {
      throw new MonarchException("Expected changes yaml to parse as a map. See help for example.",
          e);
    }
  }

  @Override
  public String pivotSource() {
    return Optional.ofNullable(pivotSource)
        .orElseThrow(() -> new MonarchException("No changes provided."));
  }

  @Override
  public Map<String, Map<String, Object>> data() {
    Path dataDirPath = Optional.ofNullable(dataDir)
        .map(Paths::get)
        .orElseThrow(() -> new MonarchException("No data directory provided."));
    List<String> sources = hierarchy().descendants();
    Map<String, Map<String, Object>> data = new LinkedHashMap<>(sources.size());

    try {
      for (String source : sources) {
        Path sourcePath = dataDirPath.resolve(source);
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

  @Override
  public boolean helpRequested() {
    return false;
  }

  @Override
  public String helpMessage() {
    return "";
  }
}

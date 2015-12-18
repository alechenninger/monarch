package io.github.alechenninger.monarch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MonarchOptionsFromInputs implements MonarchOptions {
  private final Inputs inputs;
  private final MonarchParsers parsers;
  private final FileSystem fileSystem;

  public MonarchOptionsFromInputs(Inputs inputs, MonarchParsers parsers, FileSystem fileSystem) {
    this.inputs = inputs;
    this.parsers = parsers;
    this.fileSystem = fileSystem;
  }

  @Override
  public Optional<Hierarchy> hierarchy() {
    return inputs.getHierarchyPathOrYaml().map(pathOrYaml -> {
      try {
        InputAndParser hierarchyInput = tryGetInputStreamForPathOrString(pathOrYaml);

        return hierarchyInput.parser.parseHierarchy(hierarchyInput.stream);
      } catch (IOException e) {
        throw new MonarchException("Error reading hierarchy file.", e);
      }
    });
  }

  @Override
  public Set<String> mergeKeys() {
    return new HashSet<>(Arrays.asList(inputs.getMergeKeys().orElse("").split(",")));
  }

  @Override
  public Iterable<Change> changes() {
    return inputs.getChangesPathOrYaml().map(pathOrYaml -> {
      try {
        InputAndParser changesInput = tryGetInputStreamForPathOrString(pathOrYaml);

        return changesInput.parser.parseChanges(changesInput.stream);
      } catch (IOException e) {
        throw new MonarchException("Error reading hierarchy file.", e);
      }
    }).orElse(Collections.emptySet());
  }

  @Override
  public Optional<String> pivotSource() {
    return inputs.getPivotSource();
  }

  @Override
  public Optional<Map<String, Map<String, Object>>> data(Hierarchy hierarchy) {
    return inputs.getDataDir().map(dataDir -> {
      Path dataDirPath = fileSystem.getPath(dataDir);

      Map<String, Map<String, Object>> data = new HashMap<>();
      Map<String, List<String>> sourcesByExtension = new HashMap<>();

      for (String source : hierarchy.descendants()) {
        sourcesByExtension.merge(
            MonarchParsers.getExtensionForFileName(source),
            asGrowableList(source),
            (l1, l2) -> { l1.addAll(l2); return l1; });
      }

      for (Map.Entry<String, List<String>> extensionSources : sourcesByExtension.entrySet()) {
        String extension = extensionSources.getKey();
        List<String> sources = extensionSources.getValue();
        Map<String, Map<String, Object>> dataForExtension = parsers.forExtension(extension)
            .readData(sources, dataDirPath);
        data.putAll(dataForExtension);
      }

      return data;
    });
  }

  @Override
  public Optional<Path> outputDir() {
    return inputs.getOutputDir().map(fileSystem::getPath);
  }

  private InputAndParser tryGetInputStreamForPathOrString(String pathOrYaml) throws IOException {
    try {
      Path path = fileSystem.getPath(pathOrYaml);
      return new InputAndParser(Files.newInputStream(path), parsers.forPath(path));
    } catch (InvalidPathException e) {
      return new InputAndParser(new ByteArrayInputStream(pathOrYaml.getBytes("UTF-8")),
          /* Assume yaml */ parsers.yaml());
    }
  }

  /** As opposed to {@link java.util.Arrays#asList(Object[])}. */
  private static List<String> asGrowableList(String source) {
    List<String> list = new ArrayList<>();
    list.add(source);
    return list;
  }

  public static class InputAndParser {
    public final InputStream stream;
    /** Expressed as file extension. */
    public final MonarchParser parser;

    public InputAndParser(InputStream stream, MonarchParser parser) {
      this.stream = stream;
      this.parser = parser;
    }
  }
}

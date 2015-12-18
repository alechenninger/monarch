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
  public Optional<Path> dataDir() {
    return inputs.getDataDir().map(fileSystem::getPath);
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

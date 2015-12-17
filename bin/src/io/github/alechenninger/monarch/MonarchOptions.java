package io.github.alechenninger.monarch;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public interface MonarchOptions {
  Hierarchy hierarchy();
  Set<String> mergeKeys();
  Iterable<Change> changes();
  String pivotSource();
  Map<String, Map<String, Object>> data();
  Path outputDir();

  static MonarchOptions fromInputs(Inputs inputs, FileSystem fileSystem) {
    return fromInputs(inputs, fileSystem, new MonarchParsers.Default());
  }

  static MonarchOptions fromInputs(Inputs inputs, FileSystem fileSystem, MonarchParsers parsers) {
    return new MonarchOptionsFromInputs(inputs, parsers, fileSystem);
  }
}

package io.github.alechenninger.monarch;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public interface MonarchParser {
  Hierarchy parseHierarchy(InputStream hierarchyInput);
  Iterable<Change> parseChanges(InputStream changesInput);
  Map<String, Map<String, Object>> readData(Collection<String> sources, Path dataDir);
}

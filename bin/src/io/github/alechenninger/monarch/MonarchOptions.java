package io.github.alechenninger.monarch;

import java.util.Map;

public interface MonarchOptions {
  Hierarchy hierarchy();
  Iterable<Change> changes();
  String pivotSource();
  Map<String, Map<String, Object>> data();
}

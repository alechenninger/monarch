package io.github.alechenninger.monarch;

import java.util.Map;

public class YamlMonarchOptions implements MonarchOptions {
  @Override
  public Hierarchy hierarchy() {
    return null;
  }

  @Override
  public Iterable<Change> changes() {
    return null;
  }

  @Override
  public String pivotSource() {
    return null;
  }

  @Override
  public Map<String, Map<String, Object>> data() {
    return null;
  }

  @Override
  public boolean helpRequested() {
    return false;
  }

  @Override
  public String helpMessage() {
    return null;
  }
}

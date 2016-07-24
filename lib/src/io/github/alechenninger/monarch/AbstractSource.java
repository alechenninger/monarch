package io.github.alechenninger.monarch;

import java.util.stream.Collectors;

abstract class AbstractSource implements Source {
  @Override
  public int hashCode() {
    return path().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }

    if (obj == null || !(obj instanceof Source)) {
      return false;
    }

    Source other = (Source) obj;

    return path().equals(other.path()) &&
        lineage().stream().map(Source::path).collect(Collectors.toList()).equals(
            other.lineage().stream().map(Source::path).collect(Collectors.toList())) &&
        descendants().stream().map(Source::path).collect(Collectors.toList()).equals(
            other.descendants().stream().map(Source::path).collect(Collectors.toList()));
  }

  @Override
  public String toString() {
    return "Source(" + path() + ")";
  }
}

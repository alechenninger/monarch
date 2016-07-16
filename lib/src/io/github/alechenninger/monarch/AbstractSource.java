package io.github.alechenninger.monarch;

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
        lineage().equals(other.lineage()) &&
        descendants().equals(other.descendants());
  }

  @Override
  public String toString() {
    return "Source(" + path() + ")";
  }
}

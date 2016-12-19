package io.github.alechenninger.monarch;

abstract class AbstractSource implements Source {
  @Override
  public String toString() {
    return "Source(" + path() + ")";
  }
}

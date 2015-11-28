package io.github.alechenninger.monarch;

public class MonarchException extends RuntimeException {
  public MonarchException(String message) {
    super(message);
  }

  public MonarchException(String message, Throwable cause) {
    super(message, cause);
  }

  public MonarchException(Throwable cause) {
    super(cause);
  }
}

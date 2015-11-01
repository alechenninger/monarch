package io.github.alechenninger.monarch;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

abstract class Collect {
  public static <T> Collector<? super T, ?, Optional<T>> maxOneResultOrThrow(Supplier
      <RuntimeException> throwing) {
    return Collectors.collectingAndThen(Collectors.toList(), (List<T> list) -> {
      if (list.size() > 1) {
        throw throwing.get();
      }

      return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    });
  }
}

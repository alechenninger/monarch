/*
 * monarch - A tool for managing hierarchical data.
 * Copyright (C) 2015  Alec Henninger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.alechenninger.monarch;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

abstract class Collect {
  public static <T> Collector<? super T, ?, Optional<T>> maxOneResultOrThrow(
      Supplier<RuntimeException> throwing) {
    return Collectors.collectingAndThen(Collectors.toList(), (List<T> list) -> {
      if (list.size() > 1) {
        throw throwing.get();
      }

      return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    });
  }

  public static <T> Collector<? super T, ?, Optional<T>> maxOneResultOrThrowResult(
      Function<List<T>, RuntimeException> throwing) {
    return Collectors.collectingAndThen(Collectors.toList(), (List<T> list) -> {
      if (list.size() > 1) {
        throw throwing.apply(list);
      }

      return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    });
  }
}

package de.aminh.util;

import java.util.OptionalInt;
import java.util.function.Predicate;

public class ArrayUtil {

  public static <T> OptionalInt indexOf(T[] array, Predicate<T> predicate) {
    for (int i = 0; i < array.length; i++) {
      if (predicate.test(array[i])) {
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }

}

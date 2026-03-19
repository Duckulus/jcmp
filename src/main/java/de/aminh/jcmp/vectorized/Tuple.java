package de.aminh.jcmp.vectorized;

import java.util.Arrays;

public record Tuple(Object[] values) {

  @Override
  public int hashCode() {
    return Arrays.hashCode(values);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Tuple(Object[] otherValues)) {
      return Arrays.equals(this.values, otherValues);
    }
    return false;
  }
}
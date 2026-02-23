package de.aminh.jcmp.data;

import org.apache.commons.lang3.ArrayUtils;

/**
 * An in-memory representation of a Column
 */
public sealed interface Column {

  record IntColumn(int[] values) implements Column {
    public static IntColumn create(int size) {
      return new IntColumn(new int[size]);
    }
  }

  record DoubleColumn(double[] values) implements Column {
    public static DoubleColumn create(int size) {
      return new DoubleColumn(new double[size]);
    }

  }

  record StringColumn(String[] values) implements Column {
    public static StringColumn create(int size) {
      return new StringColumn(new String[size]);
    }

  }

  default void parseAndSetValue(int index, String value) {
    switch (this) {
      case IntColumn(int[] values) -> values[index] = Integer.parseInt(value);
      case DoubleColumn(double[] values) -> values[index] = Double.parseDouble(value);
      case StringColumn(String[] values) -> values[index] = value;
    }
  }

  default Object getValue(int index) {
    return switch (this) {
      case DoubleColumn(double[] values) -> values[index];
      case IntColumn(int[] values) -> values[index];
      case StringColumn(String[] values) -> values[index];
    };
  }

  /**
   * This copies a slice of the specified size from this column into a new one
   * @param cursor The position to start reading from
   * @param n The number of values to read. cursor + n MUST be <= the size of this column.
   */
  default Column copySlice(int cursor, int n) {
    if (cursor == 0 && n == this.length()) {
      return this;
    }
    return switch (this) {
      case IntColumn(int[] values) -> {
        int[] outputValues = new int[n];
        System.arraycopy(values, cursor, outputValues, 0, n);
        yield new IntColumn(outputValues);
      }
      case DoubleColumn(double[] values) -> {
        double[] outputValues = new double[n];
        System.arraycopy(values, cursor, outputValues, 0, n);
        yield new DoubleColumn(outputValues);
      }
      case StringColumn(String[] values) -> {
        String[] outputValues = new String[n];
        System.arraycopy(values, cursor, outputValues, 0, n);
        yield new StringColumn(outputValues);
      }
    };
  }

  /**
   * Takes in a mask of 0s and 1s and copies the elements that have a 1 in the mask into a new column
   * @param mask An int array of 0s and 1s that MUST be the same length as this column
   * @param n MUST match the number of 1s in the mask
   */
  default Column copyMask(int[] mask, int n) {
    return switch (this) {
      case IntColumn(int[] values) -> {
        int[] outputValues = new int[n];
        for(int i = 0, j = 0; i < mask.length; i++) {
          if (mask[i] == 1) {
            outputValues[j++] = values[i];
          }
        }
        yield new IntColumn(outputValues);
      }
      case DoubleColumn(double[] values) -> {
        double[] outputValues = new double[n];
        for(int i = 0, j = 0; i < mask.length; i++) {
          if (mask[i] == 1) {
            outputValues[j++] = values[i];
          }
        }
        yield new DoubleColumn(outputValues);
      }
      case StringColumn(String[] values) -> {
        String[] outputValues = new String[n];
        for(int i = 0, j = 0; i < mask.length; i++) {
          if (mask[i] == 1) {
            outputValues[j++] = values[i];
          }
        }
        yield new StringColumn(outputValues);
      }
    };
  }

  default int length() {
    return switch (this) {
      case IntColumn(int[] values) -> values.length;
      case DoubleColumn(double[] values) -> values.length;
      case StringColumn(String[] values) -> values.length;
    };
  }

  default Column merge(Column other) {
    return switch (this) {
      case IntColumn(int[] values) when other instanceof IntColumn(int[] otherValues) ->
              new IntColumn(ArrayUtils.addAll(values, otherValues));
      case DoubleColumn(double[] values) when other instanceof DoubleColumn(double[] otherValues) ->
              new DoubleColumn(ArrayUtils.addAll(values, otherValues));
      case StringColumn(String[] values) when other instanceof StringColumn(String[] otherValues) ->
              new StringColumn(ArrayUtils.addAll(values, otherValues));
      default -> throw new IllegalArgumentException("Column types didn't match");
    };
  }


}

package de.aminh.data;

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

  default int length() {
    return switch (this) {
      case IntColumn(int[] values) -> values.length;
      case DoubleColumn(double[] values) -> values.length;
      case StringColumn(String[] values) -> values.length;
    };
  }


}

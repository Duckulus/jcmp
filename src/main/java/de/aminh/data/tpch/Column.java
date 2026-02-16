package de.aminh.data.tpch;

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

  default void parseAndAdd(int index, String value) {
    switch (this) {
      case DoubleColumn(double[] values) -> values[index] = Double.parseDouble(value);
      case IntColumn(int[] values) -> values[index] = Integer.parseInt(value);
      case StringColumn(String[] values) -> values[index] = value;
    }
  }

  default int length() {
    return switch (this) {
      case DoubleColumn(double[] values) -> values.length;
      case IntColumn(int[] values) -> values.length;
      case StringColumn(String[] values) -> values.length;
    };
  }


}

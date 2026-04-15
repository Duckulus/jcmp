package de.aminh.jcmp.data;

import de.aminh.jcmp.vectorized.VectorPool;
import org.apache.commons.lang3.ArrayUtils;


/**
 * An in-memory representation of a Column
 */
public sealed interface Column {

  int size();

  record IntColumn(int size, int[] values) implements Column {

  }

  record DoubleColumn(int size, double[] values) implements Column {

  }

  record StringColumn(int size, String[] values) implements Column {

  }

  default void parseAndSetValue(int index, String value) {
    switch (this) {
      case IntColumn(_, int[] values) -> values[index] = Integer.parseInt(value);
      case DoubleColumn(_, double[] values) -> values[index] = Double.parseDouble(value);
      case StringColumn(_, String[] values) -> values[index] = value;
    }
  }

  default Object getValue(int index) {
    return switch (this) {
      case DoubleColumn(_, double[] values) -> values[index];
      case IntColumn(_, int[] values) -> values[index];
      case StringColumn(_, String[] values) -> values[index];
    };
  }

  /**
   * This copies a slice of the specified size from this column into a new one
   * @param cursor The position to start reading from
   * @param n The number of values to read. cursor + n MUST be <= the size of this column.
   */
  default Column copySlice(int cursor, int n, VectorPool pool) {
    return switch (this) {
      case IntColumn(_, int[] values) -> {
        int[] outputValues = pool.getIntVector();
        System.arraycopy(values, cursor, outputValues, 0, n);
        yield new IntColumn(n, outputValues);
      }
      case DoubleColumn(_, double[] values) -> {
        double[] outputValues = pool.getDoubleVector();
        System.arraycopy(values, cursor, outputValues, 0, n);
        yield new DoubleColumn(n, outputValues);
      }
      case StringColumn(_, String[] values) -> {
        String[] outputValues = pool.getStringVector();
        System.arraycopy(values, cursor, outputValues, 0, n);
        yield new StringColumn(n, outputValues);
      }
    };
  }

  /**
   * Takes in a mask of 0s and 1s and copies the elements that have a 1 in the mask into a new column
   * @param mask An int array of 0s and 1s that MUST be the same length as this column
   * @param n MUST match the number of 1s in the mask
   */
  default Column copyMask(int[] mask, int n, VectorPool pool) {
    return switch (this) {
      case IntColumn(_, int[] values) -> {
        int[] outputValues = pool.getIntVector();
        for(int i = 0, j = 0; i < mask.length; i++) {
          if (mask[i] == 1) {
            outputValues[j++] = values[i];
          }
        }
        yield new IntColumn(n, outputValues);
      }
      case DoubleColumn(_, double[] values) -> {
        double[] outputValues = pool.getDoubleVector();
        for(int i = 0, j = 0; i < mask.length; i++) {
          if (mask[i] == 1) {
            outputValues[j++] = values[i];
          }
        }
        yield new DoubleColumn(n, outputValues);
      }
      case StringColumn(_, String[] values) -> {
        String[] outputValues = pool.getStringVector();
        for(int i = 0, j = 0; i < mask.length; i++) {
          if (mask[i] == 1) {
            outputValues[j++] = values[i];
          }
        }
        yield new StringColumn(n, outputValues);
      }
    };
  }

  default void release(VectorPool pool) {
   switch (this) {
     case IntColumn(_, int[] values) -> pool.release(values);
     case DoubleColumn(_, double[] values) -> pool.release(values);
     case StringColumn(_, String[] values) -> pool.release(values);
   }
  }

  default Column merge(Column other) {
    return switch (this) {
      case IntColumn(int size, int[] values) when other instanceof IntColumn(int otherSize, int[] otherValues) ->
              new IntColumn(size + otherSize, ArrayUtils.addAll(values, otherValues));
      case DoubleColumn(int size, double[] values) when other instanceof DoubleColumn(int otherSize, double[] otherValues) ->
              new DoubleColumn(size + otherSize, ArrayUtils.addAll(values, otherValues));
      case StringColumn(int size, String[] values) when other instanceof StringColumn(int otherSize, String[] otherValues) ->
              new StringColumn(size + otherSize, ArrayUtils.addAll(values, otherValues));
      default -> throw new IllegalArgumentException("Column types didn't match");
    };
  }


}

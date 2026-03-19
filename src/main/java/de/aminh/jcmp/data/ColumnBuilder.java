package de.aminh.jcmp.data;

import de.aminh.jcmp.vectorized.VectorPool;

public sealed interface ColumnBuilder {

  void add(Object value);

  Column build();

  final class IntColumnBuilder implements ColumnBuilder {
    private final int[] values;
    private int i = 0;

    public IntColumnBuilder(VectorPool pool) {
      this.values = pool.getIntVector();
    }

    @Override
    public void add(Object value) {
      values[i++] = (int) value;
    }

    @Override
    public Column build() {
      return new Column.IntColumn(i, values);
    }
  }

  final class DoubleColumnBuilder implements ColumnBuilder {
    private final double[] values;
    private int i = 0;

    public DoubleColumnBuilder(VectorPool pool) {
      this.values = pool.getDoubleVector();
    }

    @Override
    public void add(Object value) {
      values[i++] = (double) value;
    }

    @Override
    public Column build() {
      return new Column.DoubleColumn(i, values);
    }
  }

  final class StringColumnBuilder implements ColumnBuilder {
    private final String[] values;
    private int i = 0;

    public StringColumnBuilder(VectorPool pool) {
      this.values = pool.getStringVector();
    }

    @Override
    public void add(Object value) {
      values[i++] = (String) value;
    }

    @Override
    public Column build() {
      return new Column.StringColumn(i, values);
    }
  }

}

package de.aminh.jcmp.data;

import de.aminh.jcmp.data.Column.DoubleColumn;
import de.aminh.jcmp.data.Column.IntColumn;
import de.aminh.jcmp.data.Column.StringColumn;
import de.aminh.jcmp.vectorized.VectorPool;

public enum DataType {
  INT,
  DOUBLE,
  STRING;

  public Column createColumn(int size, VectorPool pool) {
    return switch (this) {
      case INT -> new IntColumn(size, pool.getIntVector());
      case DOUBLE -> new DoubleColumn(size, pool.getDoubleVector());
      case STRING -> new StringColumn(size, pool.getStringVector());
    };
  }

  public Column createUnmanagedColumn(int size) {
    return switch (this) {
      case INT -> new IntColumn(size, new int[size]);
      case DOUBLE -> new DoubleColumn(size, new double[size]);
      case STRING -> new StringColumn(size, new String[size]);
    };
  }

  public ColumnBuilder createColumnBuilder(VectorPool pool) {
    return switch (this) {
      case INT -> new ColumnBuilder.IntColumnBuilder(pool);
      case DOUBLE -> new ColumnBuilder.DoubleColumnBuilder(pool);
      case STRING -> new ColumnBuilder.StringColumnBuilder(pool);
    };
  }
}

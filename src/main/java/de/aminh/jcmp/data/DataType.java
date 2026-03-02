package de.aminh.jcmp.data;

import de.aminh.jcmp.data.Column.DoubleColumn;
import de.aminh.jcmp.data.Column.IntColumn;
import de.aminh.jcmp.data.Column.StringColumn;

public enum DataType {
  INT,
  DOUBLE,
  STRING;

  public Column createColumn(int size) {
    return switch (this) {
      case INT -> IntColumn.create(size);
      case DOUBLE -> DoubleColumn.create(size);
      case STRING -> StringColumn.create(size);
    };
  }

  public ColumnBuilder createColumnBuilder() {
    return switch (this) {
      case INT -> new ColumnBuilder.IntColumnBuilder();
      case DOUBLE -> new ColumnBuilder.DoubleColumnBuilder();
      case STRING -> new ColumnBuilder.StringColumnBuilder();
    };
  }
}

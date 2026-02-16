package de.aminh.data;

import de.aminh.data.Column.DoubleColumn;
import de.aminh.data.Column.IntColumn;
import de.aminh.data.Column.StringColumn;

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
}

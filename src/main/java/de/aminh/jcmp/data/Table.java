package de.aminh.jcmp.data;

public interface Table {

  Column getColumn(String name);

  Attribute getAttribute(String name);

  default int[] getIntColumnValues(String columnName) {
    return ((Column.IntColumn) this.getColumn(columnName)).values();
  }

  default double[] getDoubleColumnValues(String columnName) {
    return ((Column.DoubleColumn) this.getColumn(columnName)).values();
  }

  default String[] getStringColumnValues(String columnName) {
    return ((Column.StringColumn) this.getColumn(columnName)).values();
  }

}

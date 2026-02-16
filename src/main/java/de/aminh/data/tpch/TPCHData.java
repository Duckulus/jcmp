package de.aminh.data.tpch;

import de.aminh.data.tpch.Column.DoubleColumn;
import de.aminh.data.tpch.Column.IntColumn;
import de.aminh.data.tpch.Column.StringColumn;

import java.util.HashMap;
import java.util.Map;

public class TPCHData {

 private final Map<String, Column> columns = new HashMap<>();

 public void addColumn(String name, Column column) {
  columns.put(name, column);
 }

 public IntColumn getIntColumn(String name) {
  return (IntColumn) columns.get(name);
 }

 public DoubleColumn getDoubleColumn(String name) {
  return (DoubleColumn) columns.get(name);
 }

 public StringColumn getStringColumn(String name) {
  return (StringColumn) columns.get(name);
 }

}

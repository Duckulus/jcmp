package de.aminh.jcmp.data;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.List;

public sealed interface ColumnBuilder {

  void add(Object value);

  Column build();

  final class IntColumnBuilder implements ColumnBuilder {
    private final IntArrayList list = new IntArrayList();

    @Override
    public void add(Object value) {
      list.add((int) value);
    }

    @Override
    public Column build() {
      return new Column.IntColumn(list.toIntArray());
    }
  }

  final class DoubleColumnBuilder implements ColumnBuilder {
    private final DoubleArrayList list = new DoubleArrayList();

    @Override
    public void add(Object value) {
      list.add((double) value);
    }

    @Override
    public Column build() {
      return new Column.DoubleColumn(list.toDoubleArray());
    }
  }

  final class StringColumnBuilder implements ColumnBuilder {
    private final List<String> list = new ArrayList<>();

    @Override
    public void add(Object value) {
      list.add((String) value);
    }

    @Override
    public Column build() {
      return new Column.StringColumn(list.toArray(String[]::new));
    }
  }

}

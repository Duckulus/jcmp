package de.aminh.jcmp.plan;

import de.aminh.jcmp.data.*;

import java.lang.reflect.Array;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtils {

  public static Table createTable(Object... columnValues) {
    return new Table() {
      private final Map<String, Column> columns = new HashMap<>();
      private final Map<String, Attribute> attributes = new HashMap<>();

      {
        for (int i = 0; i < columnValues.length; i++) {
          String colName = String.valueOf((char) ('a' + i));
          switch (columnValues[i]) {
            case int[] intValues -> {
              columns.put(colName, new Column.IntColumn(intValues));
              attributes.put(colName, new Attribute(colName, DataType.INT));
            }
            case double[] doubleValues -> {
              columns.put(colName, new Column.DoubleColumn(doubleValues));
              attributes.put(colName, new Attribute(colName, DataType.DOUBLE));
            }
            case String[] stringValues -> {
              columns.put(colName, new Column.StringColumn(stringValues));
              attributes.put(colName, new Attribute(colName, DataType.STRING));
            }
            default -> throw new IllegalArgumentException("Unexpected Type for Column " + columnValues[i]);
          }

        }
      }

      @Override
      public Column getColumn(String name) {
        return columns.get(name);
      }

      @Override
      public Attribute getAttribute(String name) {
        return attributes.get(name);
      }
    };
  }

  public static void assertUnorderedQueryResult(List<RecordBatch> recordBatches, Object[] expectedColumns) {
    Set<List<Object>> expectedRows = new HashSet<>();
    int rowCount = Array.getLength(expectedColumns[0]);
    for (int i = 0; i < rowCount; i++) {
      List<Object> row = new ArrayList<>();
      for (Object expectedColumn : expectedColumns) {
        row.add(Array.get(expectedColumn, i));
      }
      expectedRows.add(row);
    }

    Set<List<Object>> actualRows = new HashSet<>();
    for (RecordBatch batch : recordBatches) {
      for (int i = 0; i < batch.size(); i++) {
        List<Object> row = new ArrayList<>();
        for (int j = 0; j < batch.columns().length; j++) {
          Object value = batch.columns()[j].getValue(i);
          row.add(value);
        }
        actualRows.add(row);
      }
    }

    assertEquals(expectedRows, actualRows);
  }

}

package de.aminh.jcmp.plan;

import de.aminh.jcmp.compilation.CompiledQuery;
import de.aminh.jcmp.compilation.JavaQueryTranspiler;
import de.aminh.jcmp.data.*;
import de.aminh.jcmp.execution.VectorizedExecutor;

import java.lang.reflect.Array;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtils {

  public enum ExecutionEngine {
    VECTORIZED {
      @Override
      public RecordBatch execute(Table table, PlanNode plan) {
        VectorizedExecutor executor = Planner.plan(plan);
        executor.init();
        RecordBatch merged = null;
        RecordBatch batch;
        while ((batch = executor.next()) != null) {
          if (merged == null) {
            merged = batch;
          } else {
            merged = merged.merge(batch);
          }
        }
        return merged;
      }
    }, COMPILED {
      @Override
      public RecordBatch execute(Table table, PlanNode plan) {
        CompiledQuery compiledQuery = JavaQueryTranspiler.compile(table, plan);
        return compiledQuery.execute(table);
      }
    };

    public abstract RecordBatch execute(Table table, PlanNode plan);

  }

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

  public static void assertUnorderedQueryResult(ExecutionEngine engine, Table table, PlanNode plan, Object[] expectedColumns) {
    RecordBatch batch = engine.execute(table, plan);

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
    for (int i = 0; i < batch.size(); i++) {
      List<Object> row = new ArrayList<>();
      for (int j = 0; j < batch.columns().length; j++) {
        Object value = batch.columns()[j].getValue(i);
        row.add(value);
      }
      actualRows.add(row);
    }

    assertEquals(expectedRows, actualRows);
  }

  public static void assertRowCount(ExecutionEngine engine, Table table, PlanNode query, int expectedCount) {
    RecordBatch batch = engine.execute(table, query);
    int rowCount = batch == null ? 0 : batch.size();
    assertEquals(expectedCount,  rowCount);
  }

}

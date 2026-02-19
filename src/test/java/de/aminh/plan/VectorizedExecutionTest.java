package de.aminh.plan;

import de.aminh.data.*;
import de.aminh.data.Column.DoubleColumn;
import de.aminh.data.Column.IntColumn;
import de.aminh.data.Column.StringColumn;
import de.aminh.exceptions.TypeException;
import de.aminh.execution.VectorizedExecutor;
import de.aminh.plan.Expression.Binary;
import de.aminh.plan.Expression.BinaryOperator;
import de.aminh.plan.Expression.LiteralInt;
import de.aminh.plan.Expression.LiteralString;
import de.aminh.plan.PlanNode.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Array;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class VectorizedExecutionTest {

  private Table createTable(Object... columnValues) {
    return new Table() {
      private final Map<String, Column> columns = new HashMap<>();
      private final Map<String, Attribute> attributes = new HashMap<>();

      {
        for (int i = 0; i < columnValues.length; i++) {
          String colName = String.valueOf((char) ('a' + i));
          switch (columnValues[i]) {
            case int[] intValues -> {
              columns.put(colName, new IntColumn(intValues));
              attributes.put(colName, new Attribute(colName, DataType.INT));
            }
            case double[] doubleValues -> {
              columns.put(colName, new DoubleColumn(doubleValues));
              attributes.put(colName, new Attribute(colName, DataType.DOUBLE));
            }
            case String[] stringValues -> {
              columns.put(colName, new StringColumn(stringValues));
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

  void assertQueryResult(PlanNode query, Object[]... expectedResultBatches) {
    VectorizedExecutor executor = Planner.plan(query);
    executor.init();

    for (Object[] expectedColumns : expectedResultBatches) {
      RecordBatch batch = executor.next();
      assertNotNull(batch);

      assertEquals(expectedColumns.length, batch.attributes().length, "Mismatched column count");
      assertEquals(expectedColumns.length, batch.columns().length, "Mismatched attribute count");

      for (int i = 0; i < expectedColumns.length; i++) {
        Object expectedColumnValues = expectedColumns[i];
        Column actualColumn = batch.columns()[i];
        assertEquals(Array.getLength(expectedColumnValues), batch.size());
        switch (expectedColumnValues) {
          case int[] expectedInts -> {
            assertInstanceOf(IntColumn.class, actualColumn);
            assertArrayEquals(expectedInts, ((IntColumn) actualColumn).values());
          }
          case double[] expectedDoubles -> {
            assertInstanceOf(DoubleColumn.class, actualColumn);
            assertArrayEquals(expectedDoubles, ((DoubleColumn) actualColumn).values());
          }
          case String[] expectedStrings -> {
            assertInstanceOf(StringColumn.class, actualColumn);
            assertArrayEquals(expectedStrings, ((StringColumn) actualColumn).values());
          }
          default -> throw new IllegalArgumentException("Unexpected Type for Column " + expectedColumnValues);
        }
      }
    }
    assertNull(executor.next());
  }

  void assertUnorderedQueryResult(PlanNode query, Object[] expectedColumns) {
    VectorizedExecutor executor = Planner.plan(query);
    executor.init();

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
    RecordBatch batch;
    while ((batch = executor.next()) != null) {
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

  void assertRowCount(PlanNode query, int expectedCount) {
    VectorizedExecutor executor = Planner.plan(query);
    executor.init();
    int actualCount = 0;
    RecordBatch batch;
    while ((batch = executor.next()) != null) {
      actualCount += batch.size();
    }
    assertEquals(expectedCount, actualCount);
  }

  @Test
  void integerLiteral() {
    PlanNode query = new ProjectionNode(
            new SingleRowNode(),
            new Expression[]{new LiteralInt(42), new LiteralInt(43)}
    );
    assertQueryResult(query, new int[][]{
            {42},
            {43}
    });
  }

  @Test
  void integerSum() {
    PlanNode query = new ProjectionNode(
            new SingleRowNode(),
            new Expression[]{new Binary(
                    BinaryOperator.PLUS, new LiteralInt(2), new LiteralInt(4)
            )}
    );

    assertQueryResult(query, new int[][]{
            {6}
    });
  }

  @Test
  void sumThrowsTypeExceptionOnStringInput() {
    PlanNode query = new ProjectionNode(
            new SingleRowNode(),
            new Expression[]{new Binary(
                    BinaryOperator.PLUS, new LiteralInt(2), new LiteralString("hi")
            )}
    );
    VectorizedExecutor executor = Planner.plan(query);
    executor.init();
    assertThrows(TypeException.class, executor::next);
  }


  @Test
  void tableScan() {
    Table testTable = createTable((Object) new int[]{1, 2, 3, 4, 5});
    PlanNode query1 = new TableScanNode(testTable, List.of("a"), 5);
    assertQueryResult(query1, new int[][]{
            {1, 2, 3, 4, 5}
    });

    PlanNode query2 = new TableScanNode(testTable, List.of("a"), 3);
    assertQueryResult(query2,
            new int[][]{
                    {1, 2, 3}
            },
            new int[][]{
                    {4, 5}
            });
  }

  @ParameterizedTest(name = "Limit {0} on 5 rows yields {1} rows")
  @CsvSource({
          "5, 5",
          "10, 5",
          "3, 3",
          "0, 0"
  })
  void limit(int limit, int expectedRows) {
    Table testTable = createTable((Object) new int[]{1, 2, 3, 4, 5});
    PlanNode query = new LimitNode(
            new TableScanNode(testTable, List.of("a")),
            limit
    );
    assertRowCount(query, expectedRows);
  }

  @Test
  void selection() {
    Table testTable = createTable(
            new int[]{1, 2, 3, 4, 5},
            new int[]{10, 9, 8, 7, 6}
    );
    PlanNode query = new SelectionNode(
            new TableScanNode(testTable, List.of("a", "b")),
            new Expression.Binary(BinaryOperator.LT, new Expression.ColumnValue("b", DataType.INT), new LiteralInt(8))
    );
    assertQueryResult(query, new int[][]{
            {4, 5},
            {7, 6}
    });
  }

  @Test
  void aggregation() {
    Table testTable = createTable(
            new int[]{1, 1, 1, 2, 2},
            new int[]{2, 4, 6, 8, 10},
            new double[]{1, 1, 2, 2, 2}
    );
    PlanNode countQuery = new AggregationNode(
            new TableScanNode(testTable, List.of("a", "b")),
            List.of(new Aggregate.CountStar()),
            List.of("a")
    );
    assertQueryResult(countQuery, new int[][]{
            {1, 2},
            {3, 2}
    });

    PlanNode sumQuery = new AggregationNode(
            new TableScanNode(testTable, List.of("a", "b")),
            List.of(new Aggregate.Sum(new Expression.ColumnValue("b", DataType.INT))),
            List.of("a")
    );
    assertQueryResult(sumQuery, new int[][]{
            {1, 2},
            {12, 18}
    });

    PlanNode avgQuery = new AggregationNode(
            new TableScanNode(testTable, List.of("a", "b")),
            List.of(new Aggregate.Avg(new Expression.ColumnValue("b", DataType.INT))),
            List.of("a")
    );
    assertQueryResult(avgQuery, new Object[]{
            new int[]{1, 2},
            new double[]{(2 + 4 + 6) / 3d, (8 + 10) / 2d}
    });

    PlanNode multiAggQuery = new AggregationNode(
            new TableScanNode(testTable, List.of("a", "b", "c")),
            List.of(
                    new Aggregate.Sum(new Expression.ColumnValue("b", DataType.INT)),
                    new Aggregate.Avg(new Expression.ColumnValue("c", DataType.DOUBLE))
            ),
            List.of("a")
    );
    assertQueryResult(multiAggQuery, new Object[]{
            new int[]{1, 2},
            new int[]{12, 18},
            new double[]{(1. + 1. + 2.) / 3., 4. / 2.}
    });
    PlanNode multiKeyQuery = new AggregationNode(
            new TableScanNode(testTable, List.of("a", "b", "c")),
            List.of(
                    new Aggregate.Sum(new Expression.ColumnValue("b", DataType.INT))
            ),
            List.of("a", "c")
    );
    assertUnorderedQueryResult(multiKeyQuery, new Object[]{
            new int[]{1, 1, 2},
            new double[]{1, 2, 2},
            new int[]{6, 6, 18}
    });
  }

  @Test
  void stringComparison() {
    Table testTable = createTable(
            (Object) new String[]{"z", "a", "aa", "aab", "b", "aac", "z"}
    );
    PlanNode query = new SelectionNode(
            new TableScanNode(testTable, List.of("a")),
            new Expression.Binary(BinaryOperator.LE, new Expression.ColumnValue("a", DataType.STRING), new Expression.LiteralString("aab"))
    );
    assertQueryResult(query, (Object[]) new String[][]{
            {"a", "aa", "aab"}
    });
  }

}

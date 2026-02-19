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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VectorizedExecutionTest {

  private Table createIntTable(int[]... columnValues) {
    return new Table() {
      private final Map<String, Column> columns = new HashMap<>();
      private final Map<String, Attribute> attributes = new HashMap<>();

      {
        for (int i = 0; i < columnValues.length; i++) {
          String colName = String.valueOf((char) ('a' + i));
          columns.put(colName, new IntColumn(columnValues[i]));
          attributes.put(colName, new Attribute(colName, DataType.INT));
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

      assertEquals(expectedColumns.length, batch.attributes().length);
      assertEquals(expectedColumns.length, batch.columns().length);

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
    Table testTable = createIntTable(new int[]{1, 2, 3, 4, 5});
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
  public void limit(int limit, int expectedRows) {
    Table testTable = createIntTable(new int[]{1, 2, 3, 4, 5});
    PlanNode query = new LimitNode(
            new TableScanNode(testTable, List.of("a")),
            limit
    );
    assertRowCount(query, expectedRows);
  }

  @Test
  public void selection() {
    Table testTable = createIntTable(
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
  public void aggregation() {
    Table testTable = createIntTable(
            new int[]{1, 1, 1, 2, 2},
            new int[]{2, 4, 6, 8, 10}
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
  }

}

package de.aminh.jcmp.plan;

import de.aminh.jcmp.compilation.CompiledQuery;
import de.aminh.jcmp.compilation.JavaQueryTranspiler;
import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.exceptions.TypeException;
import de.aminh.jcmp.execution.VectorizedExecutor;
import de.aminh.jcmp.plan.Expression.Binary;
import de.aminh.jcmp.plan.Expression.BinaryOperator;
import de.aminh.jcmp.plan.Expression.LiteralInt;
import de.aminh.jcmp.plan.Expression.LiteralString;
import de.aminh.jcmp.plan.PlanNode.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static de.aminh.jcmp.plan.TestUtils.createTable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CompiledExecutionTest {

  private static void assertUnorderedQueryResult(Table table, PlanNode query, Object[] expectedColumns) {
    CompiledQuery compiled = JavaQueryTranspiler.compile(table, query);
    RecordBatch batch = compiled.execute(table);
    TestUtils.assertUnorderedQueryResult(List.of(batch), expectedColumns);
  }

  private static void assertRowCount(Table table, PlanNode query, int expectedCount) {
    CompiledQuery compiled = JavaQueryTranspiler.compile(table, query);
    RecordBatch batch = compiled.execute(table);
    assertEquals(expectedCount, batch.size());
    for (Column col : batch.columns()) {
      assertEquals(expectedCount, col.length());
    }
  }

  @BeforeAll
  public static void setup() {
    System.setProperty("jcmp.debug.codegen", "true");
  }

  @Test
  void integerLiteral() {
    PlanNode query = new ProjectionNode(
            new SingleRowNode(),
            new Expression[]{new LiteralInt(42), new LiteralInt(43)}
    );
    assertUnorderedQueryResult(createTable(), query, new int[][]{
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

    assertUnorderedQueryResult(createTable(), query, new int[][]{
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
    assertUnorderedQueryResult(testTable, query1, new int[][]{
            {1, 2, 3, 4, 5}
    });

    PlanNode query2 = new TableScanNode(testTable, List.of("a"), 3);
    assertUnorderedQueryResult(testTable, query2,
            new int[][]{
                    {1, 2, 3, 4, 5}
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
    assertRowCount(testTable, query, expectedRows);
  }

  @Test
  void selection() {
    Table testTable = createTable(
            new int[]{1, 2, 3, 4, 5},
            new int[]{10, 9, 8, 7, 6}
    );
    PlanNode query = new SelectionNode(
            new TableScanNode(testTable, List.of("a", "b")),
            new Binary(BinaryOperator.LT, new Expression.ColumnValue("b", DataType.INT), new LiteralInt(8))
    );
    assertUnorderedQueryResult(testTable, query, new int[][]{
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
    assertUnorderedQueryResult(testTable, countQuery, new int[][]{
            {1, 2},
            {3, 2}
    });

    PlanNode sumQuery = new AggregationNode(
            new TableScanNode(testTable, List.of("a", "b")),
            List.of(new Aggregate.Sum(new Expression.ColumnValue("b", DataType.INT))),
            List.of("a")
    );
    assertUnorderedQueryResult(testTable, sumQuery, new int[][]{
            {1, 2},
            {12, 18}
    });

    PlanNode avgQuery = new AggregationNode(
            new TableScanNode(testTable, List.of("a", "b")),
            List.of(new Aggregate.Avg(new Expression.ColumnValue("b", DataType.INT))),
            List.of("a")
    );
    assertUnorderedQueryResult(testTable, avgQuery, new Object[]{
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
    assertUnorderedQueryResult(testTable, multiAggQuery, new Object[]{
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
    assertUnorderedQueryResult(testTable, multiKeyQuery, new Object[]{
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
            new Binary(BinaryOperator.LE, new Expression.ColumnValue("a", DataType.STRING), new LiteralString("aab"))
    );
    assertUnorderedQueryResult(testTable, query, new String[][]{
            {"a", "aa", "aab"}
    });
  }

}

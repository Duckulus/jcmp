package de.aminh.jcmp.plan;

import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.exceptions.TypeException;
import de.aminh.jcmp.plan.Expression.Binary;
import de.aminh.jcmp.plan.Expression.BinaryOperator;
import de.aminh.jcmp.plan.Expression.LiteralInt;
import de.aminh.jcmp.plan.Expression.LiteralString;
import de.aminh.jcmp.plan.PlanNode.*;
import de.aminh.jcmp.plan.TestUtils.ExecutionEngine;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static de.aminh.jcmp.plan.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ExecutionTest {


  @BeforeAll
  public static void setup() {
    System.setProperty("jcmp.debug.codegen", "true");
  }

  @ParameterizedTest
  @EnumSource(ExecutionEngine.class)
  void integerLiteral(ExecutionEngine engine) {
    PlanNode query = new ProjectionNode(
            new SingleRowNode(),
            new Expression[]{new LiteralInt(42), new LiteralInt(43)}
    );
    assertUnorderedQueryResult(engine, createTable(), query, new int[][]{
            {42},
            {43}
    });
  }

  @ParameterizedTest
  @EnumSource(ExecutionEngine.class)
  void integerSum(ExecutionEngine engine) {
    PlanNode query = new ProjectionNode(
            new SingleRowNode(),
            new Expression[]{new Binary(
                    BinaryOperator.PLUS, new LiteralInt(2), new LiteralInt(4)
            )}
    );

    assertUnorderedQueryResult(engine, createTable(), query, new int[][]{
            {6}
    });
  }

  @ParameterizedTest
  @EnumSource(ExecutionEngine.class)
  void sumThrowsTypeExceptionOnStringInput(ExecutionEngine engine) {
    PlanNode query = new ProjectionNode(
            new SingleRowNode(),
            new Expression[]{new Binary(
                    BinaryOperator.PLUS, new LiteralInt(2), new LiteralString("hi")
            )}
    );

    assertThrows(TypeException.class, () -> engine.execute(createTable(), query));
  }


  @ParameterizedTest
  @EnumSource(ExecutionEngine.class)
  void tableScan(ExecutionEngine engine) {
    Table testTable = createTable((Object) new int[]{1, 2, 3, 4, 5});
    PlanNode query1 = new TableScanNode(testTable, List.of("a"), 5);
    assertUnorderedQueryResult(engine, testTable, query1,
            new int[][]{
                    {1, 2, 3, 4, 5},
            });

    PlanNode query2 = new TableScanNode(testTable, List.of("a"), 3);
    assertUnorderedQueryResult(engine, testTable, query2,
            new int[][]{
                    {1, 2, 3, 4, 5},
            });
  }

  static java.util.stream.Stream<org.junit.jupiter.params.provider.Arguments> limitTestArguments() {
    return java.util.stream.Stream.of(ExecutionEngine.values())
            .flatMap(engine -> java.util.stream.Stream.of(
                    org.junit.jupiter.params.provider.Arguments.of(engine, 5, 5),
                    org.junit.jupiter.params.provider.Arguments.of(engine, 10, 5),
                    org.junit.jupiter.params.provider.Arguments.of(engine, 3, 3),
                    org.junit.jupiter.params.provider.Arguments.of(engine, 0, 0)
            ));
  }

  @ParameterizedTest(name = "[{0}] Limit {1} on 5 rows yields {2} rows")
  @MethodSource("limitTestArguments")
  void limit(ExecutionEngine engine, int limit, int expectedRows) {
    Table testTable = createTable((Object) new int[]{1, 2, 3, 4, 5});
    PlanNode query = new LimitNode(
            new TableScanNode(testTable, List.of("a")),
            limit
    );
    assertRowCount(engine, testTable, query, expectedRows);
  }

  @ParameterizedTest
  @EnumSource(ExecutionEngine.class)
  void selection(ExecutionEngine engine) {
    Table testTable = createTable(
            new int[]{1, 2, 3, 4, 5},
            new int[]{10, 9, 8, 7, 6}
    );
    PlanNode query1 = new SelectionNode(
            new TableScanNode(testTable, List.of("a", "b")),
            new Expression.Binary(BinaryOperator.LT, new Expression.ColumnValue("b", DataType.INT), new LiteralInt(8))
    );
    assertUnorderedQueryResult(engine, testTable, query1,
            new int[][]{
                    {4, 5},
                    {7, 6}
            });

    PlanNode query2 = new SelectionNode(
            new TableScanNode(testTable, List.of("a")),
            new Expression.Binary(BinaryOperator.AND,
                    new Expression.Binary(BinaryOperator.GT, new Expression.ColumnValue("a", DataType.INT), new LiteralInt(2)),
                    new Expression.Binary(BinaryOperator.LT, new Expression.ColumnValue("a", DataType.INT), new LiteralInt(5))

    ));
    assertUnorderedQueryResult(engine, testTable, query2,
            new int[][]{
                    {3,4}
            });

    PlanNode query3 = new SelectionNode(
            new TableScanNode(testTable, List.of("a", "b")),
            new Expression.Binary(BinaryOperator.OR,
                    new Expression.Binary(BinaryOperator.GE, new Expression.ColumnValue("a", DataType.INT), new LiteralInt(4)),
                    new Expression.Binary(BinaryOperator.GE, new Expression.ColumnValue("b", DataType.INT), new LiteralInt(9))

            ));
    assertUnorderedQueryResult(engine, testTable, query3,
            new int[][]{
                    {1,2,4,5},
                    {10,9,7,6}
            });
  }

  @ParameterizedTest
  @EnumSource(ExecutionEngine.class)
  void aggregation(ExecutionEngine engine) {
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
    assertUnorderedQueryResult(engine, testTable, countQuery, new int[][]{
            {1, 2},
            {3, 2}
    });

    PlanNode sumQuery = new AggregationNode(
            new TableScanNode(testTable, List.of("a", "b")),
            List.of(new Aggregate.Sum(new Expression.ColumnValue("b", DataType.INT))),
            List.of("a")
    );
    assertUnorderedQueryResult(engine, testTable, sumQuery, new int[][]{
            {1, 2},
            {12, 18}
    });

    PlanNode avgQuery = new AggregationNode(
            new TableScanNode(testTable, List.of("a", "b")),
            List.of(new Aggregate.Avg(new Expression.ColumnValue("b", DataType.INT))),
            List.of("a")
    );
    assertUnorderedQueryResult(engine, testTable, avgQuery, new Object[]{
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
    assertUnorderedQueryResult(engine, testTable, multiAggQuery, new Object[]{
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
    assertUnorderedQueryResult(engine, testTable, multiKeyQuery,
            new Object[]{
            new int[]{1, 1, 2},
            new double[]{1, 2, 2},
            new int[]{6, 6, 18}
    });
  }

  @ParameterizedTest
  @EnumSource(ExecutionEngine.class)
  void stringComparison(ExecutionEngine engine) {
    Table testTable = createTable(
            (Object) new String[]{"z", "a", "aa", "aab", "b", "aac", "z"}
    );
    PlanNode query = new SelectionNode(
            new TableScanNode(testTable, List.of("a")),
            new Expression.Binary(BinaryOperator.LE, new Expression.ColumnValue("a", DataType.STRING), new Expression.LiteralString("aab"))
    );
    assertUnorderedQueryResult(engine, testTable, query,
            new String[][]{
            {"a", "aa", "aab"}
    });
  }

}

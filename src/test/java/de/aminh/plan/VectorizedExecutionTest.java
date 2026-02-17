package de.aminh.plan;

import de.aminh.data.*;
import de.aminh.data.Column.IntColumn;
import de.aminh.exceptions.TypeException;
import de.aminh.execution.VectorizedExecutor;
import de.aminh.plan.Expression.LiteralInt;
import de.aminh.plan.Expression.LiteralString;
import de.aminh.plan.Expression.Sum;
import de.aminh.plan.PlanNode.ProjectionNode;
import de.aminh.plan.PlanNode.SingleRowNode;
import de.aminh.plan.PlanNode.TableScanNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VectorizedExecutionTest {

  @Test
  void integerLiteral() {
    PlanNode query = new ProjectionNode(
            new SingleRowNode(),
            new Expression[]{new LiteralInt(42), new LiteralInt(43)}
    );

    VectorizedExecutor executor = Planner.plan(query);
    executor.init();

    RecordBatch batch = executor.next();
    assertNotNull(batch);
    assertEquals(1, batch.size());
    assertEquals(2, batch.attributes().length);
    assertEquals(42, batch.columns()[0].getValue(0));
    assertEquals(43, batch.columns()[1].getValue(0));
    assertNull(executor.next());
  }

  @Test
  void integerSum() {
    PlanNode query = new ProjectionNode(
            new SingleRowNode(),
            new Expression[]{new Sum(
                    new LiteralInt(2), new LiteralInt(4)
            )}
    );

    VectorizedExecutor executor = Planner.plan(query);
    executor.init();
    RecordBatch batch = executor.next();
    assertNotNull(batch);
    assertEquals(1, batch.size());
    assertEquals(1, batch.attributes().length);
    assertEquals(6, batch.columns()[0].getValue(0));
    assertNull(executor.next());
  }

  @Test
  void invalidSum() {
    PlanNode query = new ProjectionNode(
            new SingleRowNode(),
            new Expression[]{new Sum(
                    new LiteralInt(2), new LiteralString("hi")
            )}
    );

    VectorizedExecutor executor = Planner.plan(query);
    executor.init();
    assertThrows(TypeException.class, executor::next);
  }

  @Test
  void tableScan() {
    Table testTable = new Table() {
      private final Map<String, Column> columns = Map.of(
              "a", new IntColumn(new int[]{1, 2, 3, 4, 5})
      );
      private final Map<String, Attribute> attributes = Map.of(
              "a",
              new Attribute("a", DataType.INT)
      );

      @Override
      public Column getColumn(String name) {
        return columns.get(name);
      }

      @Override
      public Attribute getAttribute(String name) {
        return attributes.get(name);
      }
    };
    PlanNode query1 = new TableScanNode(testTable, List.of("a"), 5);
    VectorizedExecutor executor1 = Planner.plan(query1);
    executor1.init();
    RecordBatch batch = executor1.next();
    assertNotNull(batch);
    assertEquals(5, batch.size());
    assertEquals(1, batch.attributes().length);
    assertNull(executor1.next());

    PlanNode query2 = new TableScanNode(testTable, List.of("a"), 3);
    VectorizedExecutor executor2 = Planner.plan(query2);
    executor2.init();
    RecordBatch batch2 = executor2.next();
    assertNotNull(batch2);
    assertEquals(3, batch2.size());
    assertEquals(1, batch2.attributes().length);
    batch2 = executor2.next();
    assertNotNull(batch2);
    assertEquals(2, batch2.size());
    assertEquals(1, batch2.attributes().length);
    assertNull(executor2.next());
  }

}

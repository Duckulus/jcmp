package de.aminh.plan;

import de.aminh.data.Attribute;
import de.aminh.data.Column;
import de.aminh.data.Column.IntColumn;
import de.aminh.data.DataType;
import de.aminh.data.Table;
import de.aminh.exceptions.TypeException;
import de.aminh.plan.Expression.LiteralInt;
import de.aminh.plan.Expression.LiteralString;
import de.aminh.plan.Expression.Sum;
import de.aminh.plan.nodes.ProjectionNode;
import de.aminh.plan.nodes.SingleRowNode;
import de.aminh.plan.nodes.TableScanNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VectorizedExecutionTest {

  @Test
  void integerLiteral() {
    PlanNode query = new ProjectionNode(
            new Expression[]{new LiteralInt(42), new LiteralInt(43)},
            new SingleRowNode()
    );

    RecordBatch batch = query.next();
    assertNotNull(batch);
    assertEquals(1, batch.size());
    assertEquals(2, batch.attributes().length);
    assertEquals(42, batch.columns()[0].getValue(0));
    assertEquals(43, batch.columns()[1].getValue(0));
    assertNull(query.next());
  }

  @Test
  void integerSum() {
    PlanNode query = new ProjectionNode(
            new Expression[]{new Sum(
                    new LiteralInt(2), new LiteralInt(4)
            )},
            new SingleRowNode()
    );

    RecordBatch batch = query.next();
    assertNotNull(batch);
    assertEquals(1, batch.size());
    assertEquals(1, batch.attributes().length);
    assertEquals(6, batch.columns()[0].getValue(0));
    assertNull(query.next());
  }

  @Test
  void invalidSum() {
    PlanNode query = new ProjectionNode(
            new Expression[]{new Sum(
                    new LiteralInt(2), new LiteralString("hi")
            )},
            new SingleRowNode()
    );

    assertThrows(TypeException.class, query::next);
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
    PlanNode query = new TableScanNode(testTable, List.of("a"), 5);
    RecordBatch batch = query.next();
    assertNotNull(batch);
    assertEquals(5, batch.size());
    assertEquals(1, batch.attributes().length);
    assertNull(query.next());

    PlanNode query2 = new TableScanNode(testTable, List.of("a"), 3);
    RecordBatch batch2 = query2.next();
    assertNotNull(batch2);
    assertEquals(3, batch2.size());
    assertEquals(1, batch2.attributes().length);
    batch2 = query2.next();
    assertNotNull(batch2);
    assertEquals(2, batch2.size());
    assertEquals(1, batch2.attributes().length);
    assertNull(query2.next());
  }

}

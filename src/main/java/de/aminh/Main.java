package de.aminh;

import de.aminh.data.Attribute;
import de.aminh.data.DataType;
import de.aminh.data.Table;
import de.aminh.data.tpch.TPCHDataLoader;
import de.aminh.plan.Expression;
import de.aminh.plan.Expression.ColumnValue;
import de.aminh.plan.Expression.Sum;
import de.aminh.plan.PlanNode;
import de.aminh.plan.RecordBatch;
import de.aminh.plan.nodes.LimitNode;
import de.aminh.plan.nodes.ProjectionNode;
import de.aminh.plan.nodes.TableScanNode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
  static void main() {
    Table table = new TPCHDataLoader().loadData();

    runQuery(
            new LimitNode(
                    new ProjectionNode(
                            new Expression[]{
                                    new Sum(
                                            new ColumnValue("c_custkey", DataType.INT),
                                            new Expression.LiteralInt(1)
                                    )
                            },
                            new TableScanNode(table, List.of("c_custkey"))
                    ),
                    5

            ));
  }

  static void runQuery(PlanNode planNode) {
    RecordBatch batch;
    boolean printHeader = true;
    while ((batch = planNode.next()) != null) {
      if (printHeader) {
        IO.println(Arrays.stream(batch.attributes()).map(Attribute::name).collect(Collectors.joining(" | ")));
        printHeader = false;
      }
      for (int i = 0; i < batch.size(); i++) {
        int finalI = i;
        IO.println(
                Arrays.stream(batch.columns()).map(col -> col.getValue(finalI).toString()).collect(Collectors.joining(" | "))
        );
      }
    }
  }
}

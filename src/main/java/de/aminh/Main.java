package de.aminh;

import de.aminh.data.Attribute;
import de.aminh.data.DataType;
import de.aminh.data.RecordBatch;
import de.aminh.data.Table;
import de.aminh.data.tpch.TPCHDataLoader;
import de.aminh.execution.VectorizedExecutor;
import de.aminh.plan.Aggregate;
import de.aminh.plan.Expression;
import de.aminh.plan.PlanNode;
import de.aminh.plan.PlanNode.AggregationNode;
import de.aminh.plan.PlanNode.TableScanNode;
import de.aminh.plan.Planner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
  static void main() {
    Table table = new TPCHDataLoader().loadData();

    runQuery(
            new AggregationNode(
                    new TableScanNode(table, List.of("c_nationkey", "c_custkey")),
                    List.of(new Aggregate.Avg(new Expression.ColumnValue("c_custkey", DataType.INT))),
                    List.of("c_nationkey")
            )
    );

  }

  static void runQuery(PlanNode planNode) {
    VectorizedExecutor executor = Planner.plan(planNode);
    executor.init();
    RecordBatch batch;
    boolean printHeader = true;
    while ((batch = executor.next()) != null) {
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

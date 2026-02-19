package de.aminh;

import de.aminh.data.DataType;
import de.aminh.data.Table;
import de.aminh.data.tpch.TPCHDataLoader;
import de.aminh.execution.impl.PrintResultExecutor;
import de.aminh.plan.Aggregate;
import de.aminh.plan.Expression;
import de.aminh.plan.PlanNode.AggregationNode;
import de.aminh.plan.PlanNode.TableScanNode;

import java.util.List;

public class Main {
  static void main() {
    Table table = new TPCHDataLoader().loadData();

    PrintResultExecutor.print(
            new AggregationNode(
                    new TableScanNode(table, List.of("c_nationkey", "c_custkey")),
                    List.of(new Aggregate.Avg(new Expression.ColumnValue("c_custkey", DataType.INT))),
                    List.of("c_nationkey")
            )
    );

  }

}

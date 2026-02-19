package de.aminh.plan;

import de.aminh.Configuration;
import de.aminh.data.Table;

import java.util.List;

public sealed interface PlanNode {

  record LimitNode(PlanNode child, int limit) implements PlanNode {

  }

  record ProjectionNode(PlanNode child, Expression[] expressions) implements PlanNode {

  }

  /**
   * This node is used in queries that don't have a FROM clause, e.g. SELECT 1;
   */
  record SingleRowNode() implements PlanNode {

  }

  record TableScanNode(Table table, List<String> columnNames, int batchSize) implements PlanNode {
    public TableScanNode(Table table, List<String> columnNames) {
      this(table, columnNames, Configuration.BATCH_SIZE);
    }
  }

  record SelectionNode(PlanNode child, Expression predicate) implements PlanNode {

  }

  record AggregationNode(PlanNode child, List<Aggregate> aggregates, List<String> keys) implements PlanNode {

  }

}

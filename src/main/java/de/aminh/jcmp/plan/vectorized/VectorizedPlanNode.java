package de.aminh.jcmp.plan.vectorized;

import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.plan.logical.PlanNode;
import de.aminh.jcmp.plan.vectorized.expr.VectorizedExpression;

import java.util.List;

public sealed interface VectorizedPlanNode {

  PlanNode logical();

  record LimitNode(PlanNode.LimitNode logical, VectorizedPlanNode child, int limit) implements VectorizedPlanNode {

  }

  record ProjectionNode(PlanNode.ProjectionNode logical, VectorizedPlanNode child,
                        List<VectorizedExpression> expressions,
                        List<String> columnAliases) implements VectorizedPlanNode {

  }


  record SingleRowNode(PlanNode.SingleRowNode logical) implements VectorizedPlanNode {

  }

  record TableScanNode(PlanNode.TableScanNode logical, Table table, List<String> columnNames,
                       int batchSize) implements VectorizedPlanNode {

  }

  record SelectionNode(PlanNode.SelectionNode logical, VectorizedPlanNode child,
                       VectorizedExpression predicate) implements VectorizedPlanNode {

  }

  record AggregationNode(PlanNode.AggregationNode logical, VectorizedPlanNode child,
                         List<VectorizedAggregate> aggregates, List<String> aggregateColumnAliases,
                         List<String> keys) implements VectorizedPlanNode {

  }

  record JoinNode(PlanNode.JoinNode logical, VectorizedPlanNode leftChild, VectorizedPlanNode rightChild,
                  List<VectorizedExpression> leftExprs, List<VectorizedExpression> rightExprs,
                  VectorizedExpression residualFilter) implements VectorizedPlanNode {

  }

  default Attribute[] outputSchema() {
    return logical().outputSchema();
  }

}

package de.aminh.jcmp.plan;

import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.execution.VectorizedExecutor;
import de.aminh.jcmp.execution.impl.*;
import de.aminh.jcmp.plan.logical.Aggregate;
import de.aminh.jcmp.plan.logical.Expression;
import de.aminh.jcmp.plan.logical.PlanNode;
import de.aminh.jcmp.plan.vectorized.VectorizedAggregate;
import de.aminh.jcmp.plan.vectorized.VectorizedPlanNode;
import de.aminh.jcmp.plan.vectorized.VectorizedPlanNode.*;
import de.aminh.jcmp.plan.vectorized.expr.VectorizedExpression;


public class Planner {


  public static VectorizedExecutor plan(PlanNode planNode) {
    return createExecutor(translate(planNode));
  }

  /**
   * Translates a logical query plan into a physical vectorized execution plan
   */
  private static VectorizedPlanNode translate(PlanNode planNode) {
    return switch (planNode) {
      case PlanNode.AggregationNode aggNode -> new AggregationNode(
              aggNode,
              translate(aggNode.child()),
              aggNode.aggregates().stream().map(Planner::translateAggregate).toList(),
              aggNode.aggregateColumnAliases(),
              aggNode.keys()
      );

      case PlanNode.JoinNode joinNode -> new JoinNode(
              joinNode,
              translate(joinNode.leftChild()),
              translate(joinNode.rightChild()),
              joinNode.leftExprs().stream().map(Planner::translateExp).toList(),
              joinNode.rightExprs().stream().map(Planner::translateExp).toList(),
              translateExp(joinNode.residualFilter())
      );

      case PlanNode.LimitNode limitNode -> new LimitNode(
              limitNode,
              translate(limitNode.child()),
              limitNode.limit()
      );

      case PlanNode.ProjectionNode projNode -> new ProjectionNode(
              projNode,
              translate(projNode.child()),
              projNode.expressions().stream().map(Planner::translateExp).toList(),
              projNode.columnAliases()
      );

      case PlanNode.SelectionNode selNode -> new SelectionNode(
              selNode,
              translate(selNode.child()),
              translateExp(selNode.predicate())
      );

      case PlanNode.SingleRowNode singleNode -> new SingleRowNode(singleNode);

      case PlanNode.TableScanNode scanNode -> new TableScanNode(
              scanNode,
              scanNode.table(),
              scanNode.columnNames(),
              scanNode.batchSize()
      );
    };
  }

  private static VectorizedAggregate translateAggregate(Aggregate aggregate) {
    return switch (aggregate) {
      case Aggregate.Avg(Expression expr) -> new VectorizedAggregate.Avg(translateExp(expr));
      case Aggregate.CountStar _ -> new VectorizedAggregate.CountStar();
      case Aggregate.Sum(Expression expr) -> new VectorizedAggregate.Sum(translateExp(expr));
    };
  }

  public static VectorizedExpression translateExp(Expression exp) {
    if (exp == null) {
      return null;
    }
    return switch (exp) {
      case Expression.LiteralInt(int value) -> new VectorizedExpression.LiteralInt(value);
      case Expression.LiteralDouble(double value) -> new VectorizedExpression.LiteralDouble(value);
      case Expression.LiteralString(String value) -> new VectorizedExpression.LiteralString(value);
      case Expression.ColumnValue(String name, DataType type) -> new VectorizedExpression.ColumnValue(name, type);
      case Expression.Binary binary -> VectorizedExpression.translateBinary(binary);
    };
  }

  private static VectorizedExecutor createExecutor(VectorizedPlanNode planNode) {
    switch (planNode) {
      case LimitNode limitNode -> {
        VectorizedExecutor child = createExecutor(limitNode.child());
        return new LimitExecutor(limitNode, child);
      }
      case ProjectionNode projectionNode -> {
        VectorizedExecutor child = createExecutor(projectionNode.child());
        return new ProjectionExecutor(projectionNode, child);
      }
      case SingleRowNode singleRowNode -> {
        return new SingleRowExecutor(singleRowNode);
      }
      case TableScanNode tableScanNode -> {
        return new TableScanExecutor(tableScanNode);
      }
      case SelectionNode selectionNode -> {
        VectorizedExecutor child = createExecutor(selectionNode.child());
        return new SelectionExecutor(selectionNode, child);
      }
      case AggregationNode aggregationNode -> {
        VectorizedExecutor child = createExecutor(aggregationNode.child());
        return new HashAggregationExecutor(aggregationNode, child);
      }
      case JoinNode joinNode -> {
        VectorizedExecutor leftChild = createExecutor(joinNode.leftChild());
        VectorizedExecutor rightChild = createExecutor(joinNode.rightChild());
        return new HashJoinExecutor(joinNode, leftChild, rightChild);
      }
    }
  }

}

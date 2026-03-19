package de.aminh.jcmp.vectorized;

import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.plan.Aggregate;
import de.aminh.jcmp.plan.Expression;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.vectorized.executors.*;
import de.aminh.jcmp.vectorized.plan.VectorizedAggregate;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode.*;
import de.aminh.jcmp.vectorized.plan.expr.VectorizedExpression;


public class VectorizedPlanner {


  public static VectorizedExecutor plan(PlanNode planNode) {
    ExecutionContext ctx = new ExecutionContext();
    return createExecutor(ctx, translate(planNode));
  }

  public static VectorizedExecutor plan(ExecutionContext ctx, PlanNode planNode) {
    return createExecutor(ctx, translate(planNode));
  }

  /**
   * Translates a logical query plan into a physical vectorized execution plan
   */
  private static VectorizedPlanNode translate(PlanNode planNode) {
    return switch (planNode) {
      case PlanNode.AggregationNode aggNode -> new AggregationNode(
              aggNode,
              translate(aggNode.child()),
              aggNode.aggregates().stream().map(VectorizedPlanner::translateAggregate).toList(),
              aggNode.aggregateColumnAliases(),
              aggNode.keys()
      );

      case PlanNode.JoinNode joinNode -> new JoinNode(
              joinNode,
              translate(joinNode.leftChild()),
              translate(joinNode.rightChild()),
              joinNode.leftExprs().stream().map(VectorizedPlanner::translateExp).toList(),
              joinNode.rightExprs().stream().map(VectorizedPlanner::translateExp).toList(),
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
              projNode.expressions().stream().map(VectorizedPlanner::translateExp).toList(),
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
              scanNode.columnNames()
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

  private static VectorizedExecutor createExecutor(ExecutionContext ctx, VectorizedPlanNode planNode) {
    switch (planNode) {
      case LimitNode limitNode -> {
        VectorizedExecutor child = createExecutor(ctx, limitNode.child());
        return new LimitExecutor(ctx, limitNode, child);
      }
      case ProjectionNode projectionNode -> {
        VectorizedExecutor child = createExecutor(ctx, projectionNode.child());
        return new ProjectionExecutor(ctx, projectionNode, child);
      }
      case SingleRowNode singleRowNode -> {
        return new SingleRowExecutor(singleRowNode);
      }
      case TableScanNode tableScanNode -> {
        return new TableScanExecutor(ctx, tableScanNode);
      }
      case SelectionNode selectionNode -> {
        VectorizedExecutor child = createExecutor(ctx, selectionNode.child());
        return new SelectionExecutor(ctx, selectionNode, child);
      }
      case AggregationNode aggregationNode -> {
        VectorizedExecutor child = createExecutor(ctx, aggregationNode.child());
        return new HashAggregationExecutor(ctx, aggregationNode, child);
      }
      case JoinNode joinNode -> {
        VectorizedExecutor leftChild = createExecutor(ctx, joinNode.leftChild());
        VectorizedExecutor rightChild = createExecutor(ctx, joinNode.rightChild());
        return new HashJoinExecutor(ctx, joinNode, leftChild, rightChild);
      }
    }
  }

}

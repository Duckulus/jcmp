package de.aminh.jcmp.plan;

import de.aminh.jcmp.execution.VectorizedExecutor;
import de.aminh.jcmp.execution.impl.*;
import de.aminh.jcmp.plan.PlanNode.*;

public class Planner {

  /**
   * Translates a logical query plan into a physical vectorized Executor
   */
  public static VectorizedExecutor plan(PlanNode planNode) {
    switch (planNode) {
      case LimitNode limitNode -> {
        VectorizedExecutor child = plan(limitNode.child());
        return new LimitExecutor(limitNode, child);
      }
      case ProjectionNode projectionNode -> {
        VectorizedExecutor child = plan(projectionNode.child());
        return new ProjectionExecutor(projectionNode, child);
      }
      case SingleRowNode singleRowNode -> {
        return new SingleRowExecutor(singleRowNode);
      }
      case TableScanNode tableScanNode -> {
        return new TableScanExecutor(tableScanNode);
      }
      case SelectionNode selectionNode -> {
        VectorizedExecutor child = plan(selectionNode.child());
        return new SelectionExecutor(selectionNode, child);
      }
      case AggregationNode aggregationNode -> {
        VectorizedExecutor child = plan(aggregationNode.child());
        return new HashAggregationExecutor(aggregationNode, child);
      }
      case JoinNode joinNode -> {
        VectorizedExecutor leftChild = plan(joinNode.leftChild());
        VectorizedExecutor rightChild = plan(joinNode.rightChild());
        return new HashJoinExecutor(joinNode, leftChild, rightChild);
      }
    }
  }

}

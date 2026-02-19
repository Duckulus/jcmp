package de.aminh.plan;

import de.aminh.execution.VectorizedExecutor;
import de.aminh.execution.impl.*;
import de.aminh.plan.PlanNode.*;

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
      case SingleRowNode _ -> {
        return new SingleRowExecutor();
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
    }
  }

}

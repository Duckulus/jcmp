package de.aminh.plan;

import de.aminh.execution.VectorizedExecutor;
import de.aminh.execution.impl.LimitExecutor;
import de.aminh.execution.impl.ProjectionExecutor;
import de.aminh.execution.impl.SingleRowExecutor;
import de.aminh.execution.impl.TableScanExecutor;
import de.aminh.plan.PlanNode.LimitNode;
import de.aminh.plan.PlanNode.ProjectionNode;
import de.aminh.plan.PlanNode.SingleRowNode;
import de.aminh.plan.PlanNode.TableScanNode;

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
    }
  }

}

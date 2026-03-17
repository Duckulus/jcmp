package de.aminh.jcmp.vectorized.executors;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.vectorized.VectorizedExecutor;
import de.aminh.jcmp.vectorized.plan.expr.VectorizedExpression;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode.ProjectionNode;

import java.util.List;

public class ProjectionExecutor implements VectorizedExecutor {

  private final ProjectionNode planNode;
  private final VectorizedExecutor child;

  public ProjectionExecutor(ProjectionNode planNode, VectorizedExecutor child) {
    this.planNode = planNode;
    this.child = child;
  }

  @Override
  public void init() {
    child.init();
  }

  @Override
  public RecordBatch next() {
    RecordBatch batch = child.next();
    if (batch == null) {
      return null;
    }

    List<VectorizedExpression> expressions = planNode.expressions();
    Column[] outputColumns = new Column[expressions.size()];
    for (int i = 0; i < outputColumns.length; i++) {
      outputColumns[i] = expressions.get(i).eval(batch);
    }
    return new RecordBatch(batch.size(), planNode.outputSchema(), outputColumns);
  }

  @Override
  public VectorizedPlanNode planNode() {
    return planNode;
  }

}

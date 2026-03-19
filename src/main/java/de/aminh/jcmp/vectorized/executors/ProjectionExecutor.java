package de.aminh.jcmp.vectorized.executors;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.vectorized.ExecutionContext;
import de.aminh.jcmp.vectorized.VectorizedExecutor;
import de.aminh.jcmp.vectorized.plan.expr.VectorizedExpression;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode.ProjectionNode;

import java.util.List;

public class ProjectionExecutor implements VectorizedExecutor {

  private final ExecutionContext ctx;

  private final ProjectionNode planNode;
  private final VectorizedExecutor child;

  public ProjectionExecutor(ExecutionContext ctx, ProjectionNode planNode, VectorizedExecutor child) {
    this.ctx = ctx;
    this.planNode = planNode;
    this.child = child;
  }

  @Override
  public void init() {
    child.init();
  }

  @Override
  public RecordBatch next() {
    RecordBatch inputBatch = child.next();
    if (inputBatch == null) {
      return null;
    }

    List<VectorizedExpression> expressions = planNode.expressions();
    Column[] outputColumns = new Column[expressions.size()];
    for (int i = 0; i < outputColumns.length; i++) {
      outputColumns[i] = expressions.get(i).eval(inputBatch, ctx.pool());
    }

    inputBatch.release(ctx.pool());

    return new RecordBatch(inputBatch.size(), planNode.outputSchema(), outputColumns);
  }

  @Override
  public VectorizedPlanNode planNode() {
    return planNode;
  }

}

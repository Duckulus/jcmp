package de.aminh.jcmp.execution.impl;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.execution.VectorizedExecutor;
import de.aminh.jcmp.plan.Expression;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.PlanNode.ProjectionNode;

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

    Expression[] expressions = planNode.expressions();
    Column[] outputColumns = new Column[expressions.length];
    for (int i = 0; i < expressions.length; i++) {
      outputColumns[i] = expressions[i].eval(batch);
    }
    return new RecordBatch(batch.size(), planNode.outputSchema(), outputColumns);
  }

  @Override
  public PlanNode planNode() {
    return planNode;
  }

}

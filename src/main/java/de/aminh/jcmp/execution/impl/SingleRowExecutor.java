package de.aminh.jcmp.execution.impl;

import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.execution.VectorizedExecutor;
import de.aminh.jcmp.plan.vectorized.VectorizedPlanNode;
import de.aminh.jcmp.plan.vectorized.VectorizedPlanNode.SingleRowNode;

public class SingleRowExecutor implements VectorizedExecutor {

  private final SingleRowNode planNode;

  private boolean done;

  public SingleRowExecutor(SingleRowNode planNode) {
    this.planNode = planNode;
  }

  @Override
  public void init() {
  }

  @Override
  public RecordBatch next() {
    if(done) {
      return null;
    }

    done = true;
    return new RecordBatch(1, new Attribute[0], new Column[0]);
  }

  @Override
  public VectorizedPlanNode planNode() {
    return planNode;
  }

}

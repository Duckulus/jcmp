package de.aminh.execution.impl;

import de.aminh.data.Column;
import de.aminh.data.RecordBatch;
import de.aminh.execution.VectorizedExecutor;
import de.aminh.plan.PlanNode.LimitNode;

public class LimitExecutor implements VectorizedExecutor {

  private final LimitNode planNode;
  private final VectorizedExecutor child;

  private int emitted = 0;
  private boolean done = false;

  public LimitExecutor(LimitNode planNode, VectorizedExecutor child) {
    this.planNode = planNode;
    this.child = child;
  }

  @Override
  public void init() {
    child.init();
  }

  @Override
  public RecordBatch next() {
    if (done || emitted >= planNode.limit()) {
      return null;
    }
    RecordBatch batch = child.next();
    if (batch == null) {
      done = true;
      return null;
    }

    int rowsLeft = planNode.limit() - emitted;
    if (rowsLeft < batch.size()) {
      Column[] outputColumns = new Column[batch.columns().length];
      for (int i = 0; i < batch.columns().length; i++) {
       outputColumns[i] = batch.columns()[i].copySlice(0, rowsLeft);
      }
      emitted += rowsLeft;
      return new RecordBatch(rowsLeft, batch.attributes(), outputColumns);
    } else {
      emitted += batch.size();
      return new RecordBatch(batch.size(), batch.attributes(), batch.columns());
    }

  }

}

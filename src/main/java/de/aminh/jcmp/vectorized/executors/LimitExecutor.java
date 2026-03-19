package de.aminh.jcmp.vectorized.executors;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.vectorized.ExecutionContext;
import de.aminh.jcmp.vectorized.VectorizedExecutor;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode.LimitNode;

public class LimitExecutor implements VectorizedExecutor {

  private final ExecutionContext ctx;

  private final LimitNode planNode;
  private final VectorizedExecutor child;

  private int emitted = 0;
  private boolean done = false;

  public LimitExecutor(ExecutionContext ctx, LimitNode planNode, VectorizedExecutor child) {
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
    if (done || emitted >= planNode.limit()) {
      return null;
    }
    RecordBatch inputBatch = child.next();
    if (inputBatch == null) {
      done = true;
      return null;
    }

    int rowsLeft = planNode.limit() - emitted;
    if (rowsLeft < inputBatch.size()) {
      Column[] outputColumns = new Column[inputBatch.columns().length];
      for (int i = 0; i < inputBatch.columns().length; i++) {
       outputColumns[i] = inputBatch.columns()[i].copySlice(0, rowsLeft, ctx.pool());
      }
      emitted += rowsLeft;
      inputBatch.release(ctx.pool());
      return new RecordBatch(rowsLeft, inputBatch.attributes(), outputColumns);
    } else {
      emitted += inputBatch.size();
      return new RecordBatch(inputBatch.size(), inputBatch.attributes(), inputBatch.columns());
    }
  }

  @Override
  public VectorizedPlanNode planNode() {
    return planNode;
  }

}

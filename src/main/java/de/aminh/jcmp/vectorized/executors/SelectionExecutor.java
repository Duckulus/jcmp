package de.aminh.jcmp.vectorized.executors;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.Column.IntColumn;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.vectorized.ExecutionContext;
import de.aminh.jcmp.vectorized.VectorizedExecutor;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode.SelectionNode;

/**
 * This executor performs selection (The WHERE clause of an SQL statement)
 * It only queries the child node once and returns all matching tuples, thus it might output might fewer tuples than the systems configured batch size
 */
public class SelectionExecutor implements VectorizedExecutor {

  private final ExecutionContext ctx;

  private final SelectionNode planNode;
  private final VectorizedExecutor child;

  public SelectionExecutor(ExecutionContext ctx, SelectionNode planNode, VectorizedExecutor child) {
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

    IntColumn maskColumn = (IntColumn) planNode.predicate().eval(inputBatch, ctx.pool());
    int[] mask = maskColumn.values();
    int matches = 0;
    for(int i = 0; i < maskColumn.size(); i++) {
      matches += mask[i];
    }

    if (matches == 0) {
      return RecordBatch.empty(inputBatch.attributes(), ctx.pool());
    }

    Column[] outputColumns = new Column[inputBatch.columns().length];
    for(int i = 0; i < inputBatch.columns().length; i++) {
      outputColumns[i] = inputBatch.columns()[i].copyMask(mask, matches, ctx.pool());
    }

    maskColumn.release(ctx.pool());
    inputBatch.release(ctx.pool());

    return new RecordBatch(matches, inputBatch.attributes(), outputColumns);
  }

  @Override
  public VectorizedPlanNode planNode() {
    return planNode;
  }

}

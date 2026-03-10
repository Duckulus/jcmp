package de.aminh.jcmp.execution.impl;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.Column.IntColumn;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.execution.VectorizedExecutor;
import de.aminh.jcmp.plan.vectorized.VectorizedPlanNode;
import de.aminh.jcmp.plan.vectorized.VectorizedPlanNode.SelectionNode;

/**
 * This executor performs selection (The WHERE clause of an SQL statement)
 * It only queries the child node once and returns all matching tuples, thus it might output might fewer tuples than the systems configured batch size
 */
public class SelectionExecutor implements VectorizedExecutor {

  private final SelectionNode planNode;
  private final VectorizedExecutor child;

  public SelectionExecutor(SelectionNode planNode, VectorizedExecutor child) {
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

    int[] mask = ((IntColumn) planNode.predicate().eval(inputBatch)).values();
    int matches = 0;
    for(int i : mask) {
      matches += i;
    }

    if (matches == 0) {
      return RecordBatch.empty(inputBatch.attributes());
    }

    Column[] outputColumns = new Column[inputBatch.columns().length];
    for(int i = 0; i < inputBatch.columns().length; i++) {
      outputColumns[i] = inputBatch.columns()[i].copyMask(mask, matches);
    }

    return new RecordBatch(matches, inputBatch.attributes(), outputColumns);
  }

  @Override
  public VectorizedPlanNode planNode() {
    return planNode;
  }

}

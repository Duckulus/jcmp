package de.aminh.plan.nodes;

import de.aminh.data.Column;
import de.aminh.plan.PlanNode;
import de.aminh.plan.RecordBatch;

public class LimitNode implements PlanNode {

  private final PlanNode child;
  private final int limit;

  private int emitted = 0;
  private boolean done = false;

  public LimitNode(PlanNode child, int limit) {
    this.child = child;
    this.limit = limit;
  }

  @Override
  public void open() {
   child.open();
  }

  @Override
  public RecordBatch next() {
    if (done || emitted >= limit) {
      return null;
    }
    RecordBatch batch = child.next();
    if (batch == null) {
      done = true;
      return null;
    }

    int rowsLeft = limit - emitted;
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

  @Override
  public void close() {
    child.close();
  }
}

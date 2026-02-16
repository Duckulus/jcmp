package de.aminh.plan.nodes;

import de.aminh.plan.PlanNode;
import de.aminh.plan.RecordBatch;

/**
 * This node is used in queries that don't have a FROM clause, e.g. SELECT 1;
 */
public class SingleRowNode implements PlanNode {

  private boolean done;

  @Override
  public void open() {
  }

  @Override
  public RecordBatch next() {
    if(done) {
      return null;
    }

    done = true;
    return RecordBatch.newBatch(1);
  }

  @Override
  public void close() {
  }
}

package de.aminh.execution.impl;

import de.aminh.data.RecordBatch;
import de.aminh.execution.VectorizedExecutor;

public class SingleRowExecutor implements VectorizedExecutor {

  private boolean done;

  @Override
  public void init() {
  }

  @Override
  public RecordBatch next() {
    if(done) {
      return null;
    }

    done = true;
    return RecordBatch.newBatch(1);
  }

}

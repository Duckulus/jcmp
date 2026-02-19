package de.aminh.execution.impl;

import de.aminh.data.Attribute;
import de.aminh.data.Column;
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
    return new RecordBatch(1, new Attribute[0], new Column[0]);
  }

}

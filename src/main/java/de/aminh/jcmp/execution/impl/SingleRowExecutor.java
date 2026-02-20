package de.aminh.jcmp.execution.impl;

import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.execution.VectorizedExecutor;

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

package de.aminh.jcmp.execution;

import de.aminh.jcmp.data.RecordBatch;

public interface VectorizedExecutor {

  void init();

  RecordBatch next();

}

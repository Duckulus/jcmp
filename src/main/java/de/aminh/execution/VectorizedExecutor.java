package de.aminh.execution;

import de.aminh.data.RecordBatch;

public interface VectorizedExecutor {

  void init();

  RecordBatch next();

}

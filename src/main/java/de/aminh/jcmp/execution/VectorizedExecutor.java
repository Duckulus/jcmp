package de.aminh.jcmp.execution;

import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.plan.vectorized.VectorizedPlanNode;

public interface VectorizedExecutor {

  void init();

  RecordBatch next();

  VectorizedPlanNode planNode();

}

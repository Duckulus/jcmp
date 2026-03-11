package de.aminh.jcmp.vectorized;

import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode;

public interface VectorizedExecutor {

  void init();

  RecordBatch next();

  VectorizedPlanNode planNode();

}

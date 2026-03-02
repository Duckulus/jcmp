package de.aminh.jcmp.execution;

import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.plan.PlanNode;

public interface VectorizedExecutor {

  void init();

  RecordBatch next();

  PlanNode planNode();

}

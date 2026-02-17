package de.aminh.execution.impl;

import de.aminh.data.Attribute;
import de.aminh.data.Column;
import de.aminh.data.RecordBatch;
import de.aminh.execution.VectorizedExecutor;
import de.aminh.plan.Expression;
import de.aminh.plan.PlanNode;

public record ProjectionExecutor(PlanNode.ProjectionNode planNode,
                                 VectorizedExecutor child) implements VectorizedExecutor {

  @Override
  public void init() {
    child.init();
  }

  @Override
  public RecordBatch next() {
    RecordBatch batch = child.next();
    if (batch == null) {
      return null;
    }

    Expression[] expressions = planNode().expressions();
    Attribute[] outputAttributes = new Attribute[expressions.length];
    Column[] outputColumns = new Column[expressions.length];
    for (int i = 0; i < expressions.length; i++) {
      outputAttributes[i] = new Attribute("col_" + i, expressions[i].type());
      outputColumns[i] = expressions[i].eval(batch);
    }
    return new RecordBatch(batch.size(), outputAttributes, outputColumns);
  }


}

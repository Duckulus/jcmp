package de.aminh.plan.nodes;

import de.aminh.data.Attribute;
import de.aminh.data.Column;
import de.aminh.plan.Expression;
import de.aminh.plan.PlanNode;
import de.aminh.plan.RecordBatch;

public record ProjectionNode(Expression[] expressions, PlanNode child) implements PlanNode {

  @Override
  public void open() {
    child.open();
  }

  @Override
  public RecordBatch next() {
    RecordBatch batch = child.next();
    if (batch == null) {
      return null;
    }

    Attribute[] outputAttributes = new Attribute[expressions.length];
    Column[] outputColumns = new Column[expressions.length];
    for (int i = 0; i < expressions.length; i++) {
      outputAttributes[i] = new Attribute("col_" + i, expressions[i].type());
      outputColumns[i] = expressions[i].eval(batch);
    }
    return new RecordBatch(batch.size(), outputAttributes, outputColumns);
  }

  @Override
  public void close() {
    child.close();
  }

}

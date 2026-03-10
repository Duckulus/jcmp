package de.aminh.jcmp.execution.impl;

import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.ColumnBuilder;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.execution.VectorizedExecutor;
import de.aminh.jcmp.plan.vectorized.VectorizedPlanNode;
import de.aminh.jcmp.plan.vectorized.VectorizedPlanNode.JoinNode;

import java.util.*;

public class HashJoinExecutor implements VectorizedExecutor {

  static class Tuple {
    Object[] values;

    public Tuple(Object[] values) {
      this.values = values;
    }

    @Override
    public int hashCode() {
      return Arrays.hashCode(values);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Tuple other) {
        return Arrays.equals(this.values, other.values);
      }
      return false;
    }
  }

  private final JoinNode planNode;

  private final VectorizedExecutor leftChild;
  private final VectorizedExecutor rightChild;

  private final Map<Tuple, List<Tuple>> map = new HashMap<>();

  private final Attribute[] outputSchema;

  public HashJoinExecutor(JoinNode planNode, VectorizedExecutor leftChild, VectorizedExecutor rightChild) {
    this.planNode = planNode;
    outputSchema = planNode.outputSchema();
    this.leftChild = leftChild;
    this.rightChild = rightChild;
  }

  @Override
  public void init() {
    leftChild.init();
    rightChild.init();

    RecordBatch batch;
    while ((batch = leftChild.next()) != null) {
      Column[] keyColumns = new Column[planNode.leftExprs().size()];
      for (int i = 0; i < keyColumns.length; i++) {
        keyColumns[i] = planNode.leftExprs().get(i).eval(batch);
      }
      for (int i = 0; i < batch.size(); i++) {
        Object[] keyValues = new Object[keyColumns.length];
        for (int j = 0; j < keyColumns.length; j++) {
          keyValues[j] = keyColumns[j].getValue(i);
        }
        Tuple keyTuple = new Tuple(keyValues);
        List<Tuple> matchingTuples = map.computeIfAbsent(keyTuple, _ -> new ArrayList<>());
        Tuple fullTuple = extractTuple(batch, i);
        matchingTuples.add(fullTuple);
      }
    }
  }

  @Override
  public RecordBatch next() {
    RecordBatch batch = rightChild.next();
    if (batch == null) {
      return null;
    }

    ColumnBuilder[] outputColumnBuilders = new ColumnBuilder[outputSchema.length];
    for (int i = 0; i < outputColumnBuilders.length; i++) {
      outputColumnBuilders[i] = outputSchema[i].type().createColumnBuilder();
    }

    Column[] keyColumns = new Column[planNode.rightExprs().size()];
    for (int i = 0; i < keyColumns.length; i++) {
      keyColumns[i] = planNode.rightExprs().get(i).eval(batch);
    }
    for (int i = 0; i < batch.size(); i++) {
      Object[] keyValues = new Object[keyColumns.length];
      for (int j = 0; j < keyColumns.length; j++) {
        keyValues[j] = keyColumns[j].getValue(i);
      }
      Tuple keyTuple = new Tuple(keyValues);
      List<Tuple> matchingTuples = map.get(keyTuple);
      if (matchingTuples == null) {
        continue;
      }
      Tuple rightTuple = extractTuple(batch, i);
      for (Tuple leftMatch : matchingTuples) {
        for (int j = 0; j < leftMatch.values.length; j++) {
          outputColumnBuilders[j].add(leftMatch.values[j]);
        }
        for (int j = 0; j < rightTuple.values.length; j++) {
          outputColumnBuilders[leftMatch.values.length + j].add(rightTuple.values[j]);
        }
      }
    }

    Column[] outputColumns = new Column[outputColumnBuilders.length];
    for (int i = 0; i < outputColumns.length; i++) {
      outputColumns[i] = outputColumnBuilders[i].build();
    }

    RecordBatch joinedBatch = new RecordBatch(outputColumns[0].length(), outputSchema, outputColumns);
    if (planNode.residualFilter() != null) {
      Column.IntColumn filterResult = (Column.IntColumn) planNode.residualFilter().eval(joinedBatch);
      joinedBatch = joinedBatch.copyMask(filterResult.values());
    }

    return joinedBatch;
  }

  private Tuple extractTuple(RecordBatch batch, int index) {
    Object[] values = new Object[batch.attributes().length];
    for (int i = 0; i < values.length; i++) {
      values[i] = batch.columns()[i].getValue(index);
    }
    return new Tuple(values);
  }

  @Override
  public VectorizedPlanNode planNode() {
    return planNode;
  }

}

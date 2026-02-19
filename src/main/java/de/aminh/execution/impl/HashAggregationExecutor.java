package de.aminh.execution.impl;

import de.aminh.data.Attribute;
import de.aminh.data.Column;
import de.aminh.data.Column.DoubleColumn;
import de.aminh.data.Column.IntColumn;
import de.aminh.data.Column.StringColumn;
import de.aminh.data.DataType;
import de.aminh.data.RecordBatch;
import de.aminh.execution.VectorizedExecutor;
import de.aminh.plan.Aggregate;
import de.aminh.plan.Expression;
import de.aminh.plan.PlanNode.AggregationNode;
import org.apache.commons.lang3.tuple.MutablePair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * This Executor performs Aggregation (GROUP BY in SQL)
 * It breaks the pipeline
 */
public class HashAggregationExecutor implements VectorizedExecutor {

  static class CompoundKey {
    Object[] values;

    public CompoundKey(Object[] values) {
      this.values = values;
    }

    @Override
    public int hashCode() {
      int hashCode = values[0].hashCode();
      for (int i = 1; i < values.length; i++) {
        hashCode ^= values[i].hashCode();
      }
      return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
      // optimistic comparison for performance
      return Arrays.equals(values, ((CompoundKey) obj).values);
    }
  }

  private final AggregationNode planNode;
  private final VectorizedExecutor child;

  private boolean done = false;

  public HashAggregationExecutor(AggregationNode planNode, VectorizedExecutor child) {
    this.planNode = planNode;
    this.child = child;
  }

  @Override
  public void init() {
    child.init();
  }

  @Override
  public RecordBatch next() {
    if (done) {
      return null;
    }
    Map<CompoundKey, Object[]> aggregationMap = new HashMap<>();

    int aggregateCount = planNode.aggregates().size();
    int keyColumnCount = planNode.keys().size();
    int[] keyColumnIndexes = null;

    Attribute[] inputAttributes = null;

    RecordBatch batch;
    while ((batch = child.next()) != null) {
      if (keyColumnIndexes == null) {
        keyColumnIndexes = new int[keyColumnCount];
        for (int i = 0, j = 0; i < batch.attributes().length; i++) {
          String attributeName = batch.attributes()[i].name();
          for (String key : planNode.keys()) {
            if (key.equals(attributeName)) {
              keyColumnIndexes[j] = i;
              j++;
              break;
            }
          }
          if (j == keyColumnCount) {
            break;
          }
        }
      }
      if (inputAttributes == null) {
        inputAttributes = batch.attributes();
      }

      Column[] keyColumns = new Column[keyColumnCount];
      for (int i = 0; i < keyColumnCount; i++) {
        keyColumns[i] = batch.columns()[keyColumnIndexes[i]];
      }

      CompoundKey[] compoundKeys = new CompoundKey[batch.size()];
      // TODO rewrite this column-wise?
      for (int i = 0; i < batch.size(); i++) {
        Object[] keyValues = new Object[keyColumnCount];
        for (int j = 0; j < keyColumnCount; j++) {
          keyValues[j] = keyColumns[j].getValue(i);
        }
        compoundKeys[i] = new CompoundKey(keyValues);
      }


      for (int i = 0; i < compoundKeys.length; i++) {
        CompoundKey compoundKey = compoundKeys[i];
        Object[] state = aggregationMap.computeIfAbsent(compoundKey, _ -> new Object[aggregateCount]);
        for (int j = 0; j < aggregateCount; j++) {
          switch (planNode.aggregates().get(j)) {
            case Aggregate.CountStar _ -> {
              state[j] = (state[j] == null ? 0 : (int) state[j]) + 1;
            }
            case Aggregate.Avg(Expression expr) -> {
              MutablePair<Double, Integer> pair;
              if (state[j] == null) {
                pair = new MutablePair<>(0.0, 0);
                state[j] = pair;
              } else {
                pair = (MutablePair<Double, Integer>) state[j];
              }
              if (expr.type() == DataType.INT) {
                int elementValue = ((IntColumn) expr.evalSlice(batch, i, i + 1)).values()[0];
                pair.setLeft(pair.getLeft() + elementValue);
                pair.setRight(pair.getRight() + 1);
              } else if (expr.type() == DataType.DOUBLE) {
                double elementValue = ((DoubleColumn) expr.evalSlice(batch, i, i + 1)).values()[0];
                pair.setLeft(pair.getLeft() + elementValue);
                pair.setRight(pair.getRight() + 1);
              } else {
                throw new IllegalStateException();
              }
            }
            case Aggregate.Sum(Expression expr) -> {
              if (expr.type() == DataType.INT) {
                state[j] = (state[j] == null ? 0 : (int) state[j]) + ((IntColumn) expr.evalSlice(batch, i, i + 1)).values()[0];
              } else if (expr.type() == DataType.DOUBLE) {
                state[j] = (state[j] == null ? 0.0 : (double) state[j]) + ((DoubleColumn) expr.evalSlice(batch, i, i + 1)).values()[0];
              } else {
                throw new IllegalStateException();
              }
            }
          }
        }
      }
    }

    int outputRows = aggregationMap.size();
    Attribute[] outputAttributes = new Attribute[keyColumnCount + aggregateCount];
    Column[] outputColumns = new Column[keyColumnCount + aggregateCount];
    for (int i = 0; i < keyColumnCount; i++) {
      assert inputAttributes != null; // if keyColumnCount > 0, then there was at least one batch
      Attribute inputAttribute = inputAttributes[keyColumnIndexes[i]];
      outputAttributes[i] = inputAttribute;
      outputColumns[i] = inputAttribute.type().createColumn(outputRows);
    }
    for (int i = 0; i < aggregateCount; i++) {
      DataType aggregateType = planNode.aggregates().get(i).type();
      outputAttributes[keyColumnCount + i] = new Attribute("agg_" + i, aggregateType);
      outputColumns[keyColumnCount + i] = aggregateType.createColumn(outputRows);
    }

    for (int i = 0; i < planNode.aggregates().size(); i++) {
      Aggregate aggregate = planNode.aggregates().get(i);
      if (aggregate instanceof Aggregate.Avg(_)) {
        for (Object[] state : aggregationMap.values()) {
          MutablePair<Double, Integer> pair = (MutablePair<Double, Integer>) state[i];
          state[i] = pair.getLeft() / pair.getRight();
        }
      }
    }

    int i = 0;
    for (Map.Entry<CompoundKey, Object[]> entry : aggregationMap.entrySet()) {
      for (int j = 0; j < keyColumnCount; j++) {
        switch (outputColumns[j]) {
          case DoubleColumn(double[] values) -> values[i] = (double) entry.getKey().values[j];
          case IntColumn(int[] values) -> values[i] = (int) entry.getKey().values[j];
          case StringColumn(String[] values) -> values[i] = (String) entry.getKey().values[j];
        }
      }
      for (int j = 0; j < aggregateCount; j++) {
        switch (outputColumns[keyColumnCount + j]) {
          case DoubleColumn(double[] values) -> values[i] = (double) entry.getValue()[j];
          case IntColumn(int[] values) -> values[i] = (int) entry.getValue()[j];
          case StringColumn(String[] values) -> values[i] = (String) entry.getValue()[j];
        }
      }
      i++;
    }

    done = true;
    return new RecordBatch(outputRows, outputAttributes, outputColumns);
  }

}

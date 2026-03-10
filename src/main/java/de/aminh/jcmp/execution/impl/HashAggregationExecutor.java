package de.aminh.jcmp.execution.impl;

import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.Column.DoubleColumn;
import de.aminh.jcmp.data.Column.IntColumn;
import de.aminh.jcmp.data.Column.StringColumn;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.execution.VectorizedExecutor;
import de.aminh.jcmp.plan.vectorized.VectorizedAggregate;
import de.aminh.jcmp.plan.vectorized.expr.VectorizedExpression;
import de.aminh.jcmp.plan.vectorized.VectorizedPlanNode;
import de.aminh.jcmp.plan.vectorized.VectorizedPlanNode.AggregationNode;

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
      return Arrays.hashCode(values);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof CompoundKey other) {
        return Arrays.equals(this.values, other.values);
      }
      return false;
    }
  }

  public static class AggregationState {
    public int[] counts;
    public int[] sumsInt;
    public double[] sumsDouble;

    public AggregationState(int aggregateCount) {
      counts = new int[aggregateCount];
      sumsInt = new int[aggregateCount];
      sumsDouble = new double[aggregateCount];
    }
  }

  private final AggregationNode planNode;
  private final VectorizedExecutor child;

  private final Attribute[] inputAttributes;

  private boolean done = false;

  public HashAggregationExecutor(AggregationNode planNode, VectorizedExecutor child) {
    this.planNode = planNode;
    this.child = child;

    inputAttributes = child.planNode().outputSchema();
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
    Map<CompoundKey, AggregationState> aggregationMap = new HashMap<>();

    int aggregateCount = planNode.aggregates().size();
    int keyColumnCount = planNode.keys().size();
    int[] keyColumnIndexes = null;

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

      Column[] keyColumns = new Column[keyColumnCount];
      for (int i = 0; i < keyColumnCount; i++) {
        keyColumns[i] = batch.columns()[keyColumnIndexes[i]];
      }

      CompoundKey[] compoundKeys = new CompoundKey[batch.size()];
      for (int i = 0; i < batch.size(); i++) {
        Object[] keyValues = new Object[keyColumnCount];
        for (int j = 0; j < keyColumnCount; j++) {
          keyValues[j] = keyColumns[j].getValue(i);
        }
        compoundKeys[i] = new CompoundKey(keyValues);
      }

      Column[] evaluatedVectorizedAggregates = new Column[aggregateCount];
      for(int i = 0; i < aggregateCount; i++) {
        VectorizedExpression expr = planNode.aggregates().get(i).expressionOrNull();
        if (expr != null) {
          evaluatedVectorizedAggregates[i] = expr.eval(batch);
        }
      }

      DataType[] aggregateTypes = new DataType[aggregateCount];
      for(int i = 0; i < aggregateCount; i++) {
        aggregateTypes[i] = planNode.aggregates().get(i).expressionType();
      }

      VectorizedAggregate[] aggregates = planNode.aggregates().toArray(new VectorizedAggregate[0]);
      for (int i = 0; i < compoundKeys.length; i++) {
        CompoundKey compoundKey = compoundKeys[i];
        AggregationState state = aggregationMap.computeIfAbsent(compoundKey, _ -> new AggregationState(aggregateCount));
        for (int j = 0; j < aggregateCount; j++) {
          switch (aggregates[j]) {
            case VectorizedAggregate.CountStar _ -> state.counts[j] += 1;
            case VectorizedAggregate.Avg(_) -> {
              if (aggregateTypes[j] == DataType.INT) {
                state.sumsInt[j] += ((IntColumn) evaluatedVectorizedAggregates[j]).values()[i];
                state.counts[j] += 1;
              } else if (aggregateTypes[j] == DataType.DOUBLE) {
                state.sumsDouble[j] += ((DoubleColumn) evaluatedVectorizedAggregates[j]).values()[i];
                state.counts[j] += 1;
              } else {
                throw new IllegalStateException();
              }
            }
            case VectorizedAggregate.Sum(_) -> {
              if (aggregateTypes[j] == DataType.INT) {
                state.sumsInt[j] += ((IntColumn) evaluatedVectorizedAggregates[j]).values()[i];
              } else if (aggregateTypes[j] == DataType.DOUBLE) {
                state.sumsDouble[j] += ((DoubleColumn) evaluatedVectorizedAggregates[j]).values()[i];
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
      assert keyColumnIndexes != null;
      Attribute inputAttribute = inputAttributes[keyColumnIndexes[i]];
      outputAttributes[i] = inputAttribute;
      outputColumns[i] = inputAttribute.type().createColumn(outputRows);
    }
    for (int i = 0; i < aggregateCount; i++) {
      DataType aggregateType = planNode.aggregates().get(i).outputType();
      outputAttributes[keyColumnCount + i] = new Attribute(planNode.aggregateColumnAliases().get(i), aggregateType);
      outputColumns[keyColumnCount + i] = aggregateType.createColumn(outputRows);
    }

    int i = 0;
    for (Map.Entry<CompoundKey, AggregationState> entry : aggregationMap.entrySet()) {
      for (int j = 0; j < keyColumnCount; j++) {
        switch (outputColumns[j]) {
          case DoubleColumn(double[] values) -> values[i] = (double) entry.getKey().values[j];
          case IntColumn(int[] values) -> values[i] = (int) entry.getKey().values[j];
          case StringColumn(String[] values) -> values[i] = (String) entry.getKey().values[j];
        }
      }
      for (int j = 0; j < aggregateCount; j++) {
        VectorizedAggregate aggregate = planNode.aggregates().get(j);
        DataType exprType = aggregate.expressionType();
        Object value = aggregate.extractValue(entry.getValue(), exprType,j);
        switch (outputColumns[keyColumnCount + j]) {
          case DoubleColumn(double[] values) -> values[i] = (double) value;
          case IntColumn(int[] values) -> values[i] = (int) value;
          default -> throw new IllegalStateException("Invalid Column for aggregation " + outputColumns[keyColumnCount + j]);
        }
      }
      i++;
    }

    done = true;
    return new RecordBatch(outputRows, outputAttributes, outputColumns);
  }

  @Override
  public VectorizedPlanNode planNode() {
    return planNode;
  }

}

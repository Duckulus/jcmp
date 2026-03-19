package de.aminh.jcmp.vectorized.executors;

import de.aminh.jcmp.Configuration;
import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.Column.DoubleColumn;
import de.aminh.jcmp.data.Column.IntColumn;
import de.aminh.jcmp.data.Column.StringColumn;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.vectorized.ExecutionContext;
import de.aminh.jcmp.vectorized.Tuple;
import de.aminh.jcmp.vectorized.VectorizedExecutor;
import de.aminh.jcmp.vectorized.plan.VectorizedAggregate;
import de.aminh.jcmp.vectorized.plan.expr.VectorizedExpression;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode.AggregationNode;

import java.util.HashMap;
import java.util.Map;

/**
 * This Executor performs Aggregation (GROUP BY in SQL)
 * It breaks the pipeline
 */
public class HashAggregationExecutor implements VectorizedExecutor {

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

  private final ExecutionContext ctx;

  private final AggregationNode planNode;
  private final VectorizedExecutor child;

  private final Attribute[] inputAttributes;

  private boolean done = false;

  public HashAggregationExecutor(ExecutionContext ctx, AggregationNode planNode, VectorizedExecutor child) {
    this.ctx = ctx;
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
    Map<Tuple, AggregationState> aggregationMap = new HashMap<>();

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

      Tuple[] Tuples = new Tuple[batch.size()];
      for (int i = 0; i < batch.size(); i++) {
        Object[] keyValues = new Object[keyColumnCount];
        for (int j = 0; j < keyColumnCount; j++) {
          keyValues[j] = keyColumns[j].getValue(i);
        }
        Tuples[i] = new Tuple(keyValues);
      }

      Column[] evaluatedVectorizedAggregates = new Column[aggregateCount];
      for(int i = 0; i < aggregateCount; i++) {
        VectorizedExpression expr = planNode.aggregates().get(i).expressionOrNull();
        if (expr != null) {
          evaluatedVectorizedAggregates[i] = expr.eval(batch, ctx.pool());
        }
      }

      DataType[] aggregateTypes = new DataType[aggregateCount];
      for(int i = 0; i < aggregateCount; i++) {
        aggregateTypes[i] = planNode.aggregates().get(i).expressionType();
      }

      VectorizedAggregate[] aggregates = planNode.aggregates().toArray(new VectorizedAggregate[0]);
      for (int i = 0; i < Tuples.length; i++) {
        Tuple Tuple = Tuples[i];
        AggregationState state = aggregationMap.computeIfAbsent(Tuple, _ -> new AggregationState(aggregateCount));
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
      for(Column col : evaluatedVectorizedAggregates) {
        if (col != null) {
          col.release(ctx.pool());
        }
      }
      batch.release(ctx.pool());
    }

    int outputRows = aggregationMap.size();
    if (outputRows > Configuration.BATCH_SIZE) {
      throw new IllegalStateException("Aggregation group amount greater than " + Configuration.BATCH_SIZE + " not supported.");
    }
    Attribute[] outputAttributes = new Attribute[keyColumnCount + aggregateCount];
    Column[] outputColumns = new Column[keyColumnCount + aggregateCount];
    for (int i = 0; i < keyColumnCount; i++) {
      assert keyColumnIndexes != null;
      Attribute inputAttribute = inputAttributes[keyColumnIndexes[i]];
      outputAttributes[i] = inputAttribute;
      outputColumns[i] = inputAttribute.type().createColumn(outputRows, ctx.pool());
    }
    for (int i = 0; i < aggregateCount; i++) {
      DataType aggregateType = planNode.aggregates().get(i).outputType();
      outputAttributes[keyColumnCount + i] = new Attribute(planNode.aggregateColumnAliases().get(i), aggregateType);
      outputColumns[keyColumnCount + i] = aggregateType.createColumn(outputRows, ctx.pool());
    }

    int i = 0;
    for (Map.Entry<Tuple, AggregationState> entry : aggregationMap.entrySet()) {
      for (int j = 0; j < keyColumnCount; j++) {
        switch (outputColumns[j]) {
          case DoubleColumn(_, double[] values) -> values[i] = (double) entry.getKey().values()[j];
          case IntColumn(_, int[] values) -> values[i] = (int) entry.getKey().values()[j];
          case StringColumn(_, String[] values) -> values[i] = (String) entry.getKey().values()[j];
        }
      }
      for (int j = 0; j < aggregateCount; j++) {
        VectorizedAggregate aggregate = planNode.aggregates().get(j);
        DataType exprType = aggregate.expressionType();
        Object value = aggregate.extractValue(entry.getValue(), exprType,j);
        switch (outputColumns[keyColumnCount + j]) {
          case DoubleColumn(_, double[] values) -> values[i] = (double) value;
          case IntColumn(_, int[] values) -> values[i] = (int) value;
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

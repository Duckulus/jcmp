package de.aminh.jcmp.vectorized.executors;

import de.aminh.jcmp.Configuration;
import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.ColumnBuilder;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.vectorized.ExecutionContext;
import de.aminh.jcmp.vectorized.Tuple;
import de.aminh.jcmp.vectorized.VectorizedExecutor;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode.JoinNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HashJoinExecutor implements VectorizedExecutor {

  private final ExecutionContext ctx;

  private final JoinNode planNode;

  private final VectorizedExecutor leftChild;
  private final VectorizedExecutor rightChild;

  private final Map<Tuple, List<Tuple>> map = new HashMap<>();

  private final Attribute[] outputSchema;

  public HashJoinExecutor(ExecutionContext ctx, JoinNode planNode, VectorizedExecutor leftChild, VectorizedExecutor rightChild) {
    this.ctx = ctx;
    this.planNode = planNode;
    outputSchema = planNode.outputSchema();
    this.leftChild = leftChild;
    this.rightChild = rightChild;
  }

  @Override
  public void init() {
    leftChild.init();
    rightChild.init();

    RecordBatch buildBatch;
    while ((buildBatch = leftChild.next()) != null) {
      Column[] keyColumns = new Column[planNode.leftExprs().size()];
      for (int i = 0; i < keyColumns.length; i++) {
        keyColumns[i] = planNode.leftExprs().get(i).eval(buildBatch, ctx.pool());
      }
      for (int i = 0; i < buildBatch.size(); i++) {
        Object[] keyValues = new Object[keyColumns.length];
        for (int j = 0; j < keyColumns.length; j++) {
          keyValues[j] = keyColumns[j].getValue(i);
        }
        Tuple keyTuple = new Tuple(keyValues);
        List<Tuple> matchingTuples = map.computeIfAbsent(keyTuple, _ -> new ArrayList<>());
        Tuple fullTuple = extractTuple(buildBatch, i);
        matchingTuples.add(fullTuple);
      }
      buildBatch.release(ctx.pool());
      for (Column keyCol : keyColumns) {
        keyCol.release(ctx.pool());
      }
    }
  }

  private RecordBatch currentProbeBatch;
  private Column[] rightKeyColumns;
  private int currentProbeBatchIndex;
  private int currentMatchIndex;

  @Override
  public RecordBatch next() {
    if (currentProbeBatch == null) {
      currentProbeBatch = rightChild.next();
      if (currentProbeBatch == null) {
        return null;
      } else {
        rightKeyColumns = new Column[planNode.rightExprs().size()];
        for (int i = 0; i < rightKeyColumns.length; i++) {
          rightKeyColumns[i] = planNode.rightExprs().get(i).eval(currentProbeBatch, ctx.pool());
        }
      }
    }

    int outputRowCount = 0;
    ColumnBuilder[] outputColumnBuilders = new ColumnBuilder[outputSchema.length];
    for (int i = 0; i < outputColumnBuilders.length; i++) {
      outputColumnBuilders[i] = outputSchema[i].type().createColumnBuilder(ctx.pool());
    }

    top:
    while (currentProbeBatch != null) {
      while (currentProbeBatchIndex < currentProbeBatch.size()) {
        Object[] keyValues = new Object[rightKeyColumns.length];
        for (int j = 0; j < rightKeyColumns.length; j++) {
          keyValues[j] = rightKeyColumns[j].getValue(currentProbeBatchIndex);
        }
        Tuple keyTuple = new Tuple(keyValues);
        List<Tuple> matchingTuples = map.get(keyTuple);
        if (matchingTuples == null) {
          currentProbeBatchIndex++;
          continue;
        }
        while (currentMatchIndex < matchingTuples.size()) {
          assert outputRowCount <= Configuration.BATCH_SIZE;
          if (outputRowCount == Configuration.BATCH_SIZE) {
            break top;
          }
          Tuple rightTuple = extractTuple(currentProbeBatch, currentProbeBatchIndex);
          Tuple leftMatch = matchingTuples.get(currentMatchIndex);
          for (int j = 0; j < leftMatch.values().length; j++) {
            outputColumnBuilders[j].add(leftMatch.values()[j]);
          }
          for (int j = 0; j < rightTuple.values().length; j++) {
            outputColumnBuilders[leftMatch.values().length + j].add(rightTuple.values()[j]);
          }
          currentMatchIndex++;
          outputRowCount++;
        }
        currentProbeBatchIndex++;
        currentMatchIndex = 0;
      }

      if (outputRowCount == 0) {
        return null;
      }

      for (Column keyCol : rightKeyColumns) {
        keyCol.release(ctx.pool());
      }
      currentProbeBatch.release(ctx.pool());
      currentProbeBatch = rightChild.next();
      if (currentProbeBatch != null) {
        rightKeyColumns = new Column[planNode.rightExprs().size()];
        for (int i = 0; i < rightKeyColumns.length; i++) {
          rightKeyColumns[i] = planNode.rightExprs().get(i).eval(currentProbeBatch, ctx.pool());
        }
      }
      currentProbeBatchIndex = 0;
    }

    Column[] outputColumns = new Column[outputColumnBuilders.length];
    for (int i = 0; i < outputColumns.length; i++) {
      outputColumns[i] = outputColumnBuilders[i].build();
    }

    RecordBatch outputBatch = new RecordBatch(outputColumns[0].size(), outputSchema, outputColumns);
    if (planNode.residualFilter() != null) {
      Column.IntColumn filterResult = (Column.IntColumn) planNode.residualFilter().eval(outputBatch, ctx.pool());
      RecordBatch filteredBatch = outputBatch.copyMask(filterResult.values(), ctx.pool());
      outputBatch.release(ctx.pool());
      outputBatch = filteredBatch;
      filterResult.release(ctx.pool());
    }

    return outputBatch;
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

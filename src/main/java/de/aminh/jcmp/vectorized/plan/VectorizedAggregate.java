package de.aminh.jcmp.vectorized.plan;

import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.exceptions.TypeException;
import de.aminh.jcmp.vectorized.executors.HashAggregationExecutor;
import de.aminh.jcmp.vectorized.plan.expr.VectorizedExpression;

public sealed interface VectorizedAggregate {

  record CountStar() implements VectorizedAggregate {

  }

  record Sum(VectorizedExpression expression) implements VectorizedAggregate {

  }

  record Avg(VectorizedExpression expression) implements VectorizedAggregate {

  }

  /**
   * The type of the column produced by this aggregate
   */
  default DataType outputType() {
    return switch (this) {
      case CountStar _ -> DataType.INT;
      case Sum(VectorizedExpression expr) -> {
        DataType inputType = expr.type();
        if (inputType == DataType.INT || inputType == DataType.DOUBLE) {
          yield inputType;
        } else {
          throw new TypeException("Invalid Type %s for SUM aggregation", inputType);
        }
      }
      case Avg(VectorizedExpression expr) -> {
        DataType inputType = expr.type();
        if (inputType == DataType.INT || inputType == DataType.DOUBLE) {
          yield DataType.DOUBLE;
        } else {
          throw new TypeException("Invalid Type %s for AVG aggregation", inputType);
        }
      }
    };
  }

  default VectorizedExpression expressionOrNull() {
    return switch (this) {
      case CountStar _ -> null;
      case Sum(VectorizedExpression expr) -> expr;
      case Avg(VectorizedExpression expr) -> expr;
    };
  }

  default DataType expressionType() {
    return switch (this) {
      case CountStar _ -> DataType.INT;
      case Sum(VectorizedExpression expr) -> expr.type();
      case Avg(VectorizedExpression expr) -> expr.type();
    };
  }

  default Object extractValue(HashAggregationExecutor.AggregationState state, DataType type, int aggregateIndex) {
    return switch (this) {
      case CountStar _ -> state.counts[aggregateIndex];
      case Sum _ -> {
        if (type == DataType.INT) {
          yield state.sumsInt[aggregateIndex];
        } else if (type == DataType.DOUBLE) {
          yield state.sumsDouble[aggregateIndex];
        } else {
          throw new IllegalArgumentException();
        }
      }
      case Avg _ -> {
        if (type == DataType.INT) {
          yield (double) state.sumsInt[aggregateIndex] / state.counts[aggregateIndex];
        } else if (type == DataType.DOUBLE) {
          yield state.sumsDouble[aggregateIndex] / state.counts[aggregateIndex];
        } else {
          throw new IllegalArgumentException();
        }
      }
    };
  }

}

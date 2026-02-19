package de.aminh.plan;

import de.aminh.data.DataType;
import de.aminh.exceptions.TypeException;
import de.aminh.execution.impl.HashAggregationExecutor;

public sealed interface Aggregate {

  record CountStar() implements Aggregate {

  }

  record Sum(Expression expression) implements Aggregate {

  }

  record Avg(Expression expression) implements Aggregate {

  }

  default DataType type() {
    return switch (this) {
      case CountStar _ -> DataType.INT;
      case Sum(Expression expr) -> {
        DataType inputType = expr.type();
        if (inputType == DataType.INT || inputType == DataType.DOUBLE) {
          yield inputType;
        } else {
          throw new TypeException("Invalid Type %s for SUM aggregation", inputType);
        }
      }
      case Avg(Expression expr) -> {
        DataType inputType = expr.type();
        if (inputType == DataType.INT || inputType == DataType.DOUBLE) {
          yield DataType.DOUBLE;
        } else {
          throw new TypeException("Invalid Type %s for AVG aggregation", inputType);
        }
      }
    };
  }

  default Expression expressionOrNull() {
    return switch (this) {
      case CountStar _ -> null;
      case Sum(Expression expr) -> expr;
      case Avg(Expression expr) -> expr;
    };
  }

  default DataType expressionType() {
    return switch (this) {
      case CountStar _ -> DataType.INT;
      case Sum(Expression expr) -> expr.type();
      case Avg(Expression expr) -> expr.type();
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

package de.aminh.jcmp.plan;

import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.exceptions.TypeException;

public sealed interface Aggregate {

  record CountStar() implements Aggregate {

  }

  record Sum(Expression expression) implements Aggregate {

  }

  record Avg(Expression expression) implements Aggregate {

  }

  /**
   * The type of the column produced by this aggregate
   */
  default DataType outputType() {
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

}

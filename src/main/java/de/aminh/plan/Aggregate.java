package de.aminh.plan;

import de.aminh.data.DataType;
import de.aminh.exceptions.TypeException;

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
          throw new TypeException("Invalid Type %s for SUM aggregation", inputType);
        }
      }
    };
  }

}

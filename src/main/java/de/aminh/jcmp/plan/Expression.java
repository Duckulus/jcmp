package de.aminh.jcmp.plan;

import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.exceptions.TypeException;

public sealed interface Expression {

  record LiteralInt(int value) implements Expression {

  }

  record LiteralDouble(double value) implements Expression {
  }

  record LiteralString(String value) implements Expression {

  }

  record ColumnValue(String name, DataType type) implements Expression {

  }

  record Binary(BinaryOperator oper, Expression left, Expression right) implements Expression {

  }

  default DataType type() {
    return switch (this) {
      case LiteralInt _ -> DataType.INT;
      case LiteralDouble _ -> DataType.DOUBLE;
      case LiteralString _ -> DataType.STRING;
      case Binary(BinaryOperator oper, Expression left, Expression right) -> {
        DataType leftType = left.type();
        DataType rightType = right.type();
        if (leftType != rightType) {
          throw new TypeException("Invalid Operator %s for %s and %s", oper.name(), leftType, rightType);
        }
        yield switch (oper) {
          case EQUALS, LT, GT, LE, GE, AND, OR -> DataType.INT;
          case PLUS, MINUS, TIMES, DIVIDE -> {
            if (leftType == DataType.INT || leftType == DataType.DOUBLE) {
              yield leftType;
            } else {
              throw new TypeException("Invalid Operator %s for %s and %s", oper.name(), leftType, rightType);
            }
          }
        };
      }
      case ColumnValue(String _, DataType type) -> type;
    };
  }


}

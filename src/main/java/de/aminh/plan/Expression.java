package de.aminh.plan;

import de.aminh.data.Column;
import de.aminh.data.Column.DoubleColumn;
import de.aminh.data.Column.IntColumn;
import de.aminh.data.Column.StringColumn;
import de.aminh.data.DataType;
import de.aminh.data.RecordBatch;
import de.aminh.exceptions.ColumnNotFoundException;
import de.aminh.exceptions.TypeException;
import de.aminh.util.ArrayUtil;

import java.util.Arrays;
import java.util.OptionalInt;

public sealed interface Expression {

  record LiteralInt(int value) implements Expression {
  }

  record LiteralDouble(double value) implements Expression {
  }

  record LiteralString(String value) implements Expression {
  }

  record ColumnValue(String name, DataType type) implements Expression {

  }

  enum BinaryOperator {
    PLUS, MINUS, TIMES, DIVIDE,
    EQUALS, LT, GT, LE, GE
  }

  record Binary(BinaryOperator operator, Expression left, Expression right) implements Expression {

  }

  default Column eval(RecordBatch input) {
    return evalSlice(input, 0, input.size());
  }

  default Column evalSlice(RecordBatch input, int start, int end) {
    int sliceLength = end - start;
    switch (this) {
      case LiteralInt(int value) -> {
        int[] output = new int[sliceLength];
        Arrays.fill(output, value);
        return new IntColumn(output);
      }
      case LiteralDouble(double value) -> {
        double[] output = new double[sliceLength];
        Arrays.fill(output, value);
        return new DoubleColumn(output);
      }
      case LiteralString(String value) -> {
        String[] output = new String[sliceLength];
        Arrays.fill(output, value);
        return new StringColumn(output);
      }
      case Binary(BinaryOperator oper, Expression left, Expression right) -> {
        DataType leftType = left.type();
        DataType rightType = right.type();
        if (leftType == DataType.INT && rightType == DataType.INT) {
          int[] leftValues = ((IntColumn) left.evalSlice(input, start, end)).values();
          int[] rightValues = ((IntColumn) right.evalSlice(input, start, end)).values();
          int[] output = new int[sliceLength];
          switch (oper) {
            case PLUS -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i] + rightValues[i];
              }
            }
            case MINUS -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i] - rightValues[i];
              }
            }
            case TIMES -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i] * rightValues[i];
              }
            }
            case DIVIDE -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i] / rightValues[i];
              }
            }
            case EQUALS -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i] == rightValues[i] ? 1 : 0;
              }
            }
            case LT -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i] < rightValues[i] ? 1 : 0;
              }
            }
            case GT -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i] > rightValues[i] ? 1 : 0;
              }
            }
            case LE -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i] <= rightValues[i] ? 1 : 0;
              }
            }
            case GE -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i] >= rightValues[i] ? 1 : 0;
              }
            }
            default -> throw new TypeException("Invalid Operator %s for %s and %s", oper.name(), leftType, rightType);
          }

          return new IntColumn(output);
        } else if (leftType == DataType.DOUBLE && rightType == DataType.DOUBLE) {
          double[] leftValues = ((DoubleColumn) left.evalSlice(input, start, end)).values();
          double[] rightValues = ((DoubleColumn) right.evalSlice(input, start, end)).values();
          double[] doubleOutput = new double[sliceLength];
          int[] intOutput = new int[sliceLength];
          switch (oper) {
            case PLUS -> {
              for (int i = 0; i < doubleOutput.length; i++) {
                doubleOutput[i] = leftValues[i] + rightValues[i];
              }
            }
            case MINUS -> {
              for (int i = 0; i < doubleOutput.length; i++) {
                doubleOutput[i] = leftValues[i] - rightValues[i];
              }
            }
            case TIMES -> {
              for (int i = 0; i < doubleOutput.length; i++) {
                doubleOutput[i] = leftValues[i] * rightValues[i];
              }
            }
            case DIVIDE -> {
              for (int i = 0; i < doubleOutput.length; i++) {
                doubleOutput[i] = leftValues[i] / rightValues[i];
              }
            }
            case EQUALS -> {
              for (int i = 0; i < doubleOutput.length; i++) {
                intOutput[i] = leftValues[i] == rightValues[i] ? 1 : 0;
              }
            }
            case LT -> {
              for (int i = 0; i < doubleOutput.length; i++) {
                intOutput[i] = leftValues[i] < rightValues[i] ? 1 : 0;
              }
            }
            case GT -> {
              for (int i = 0; i < doubleOutput.length; i++) {
                intOutput[i] = leftValues[i] > rightValues[i] ? 1 : 0;
              }
            }
            case LE -> {
              for (int i = 0; i < doubleOutput.length; i++) {
                intOutput[i] = leftValues[i] <= rightValues[i] ? 1 : 0;
              }
            }
            case GE -> {
              for (int i = 0; i < doubleOutput.length; i++) {
                intOutput[i] = leftValues[i] >= rightValues[i] ? 1 : 0;
              }
            }
            default -> throw new TypeException("Invalid Operator %s for %s and %s", oper.name(), leftType, rightType);
          }
          return switch (oper) {
            case PLUS, MINUS, TIMES, DIVIDE -> new DoubleColumn(doubleOutput);
            case EQUALS, LT, GT, LE, GE -> new IntColumn(intOutput);
          };
        } else if (leftType == DataType.STRING && rightType == DataType.STRING) {
          String[] leftValues = ((StringColumn) left.evalSlice(input, start, end)).values();
          String[] rightValues = ((StringColumn) right.evalSlice(input, start, end)).values();
          int[] output = new int[sliceLength];
          switch (oper) {
            case EQUALS -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i].equals(rightValues[i]) ? 1 : 0;
              }
            }
            case LT -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i].compareTo(rightValues[i]) < 0 ? 1 : 0;
              }
            }
            case GT -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i].compareTo(rightValues[i]) > 0 ? 1 : 0;
              }
            }
            case LE -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i].compareTo(rightValues[i]) <= 0 ? 1 : 0;
              }
            }
            case GE -> {
              for (int i = 0; i < output.length; i++) {
                output[i] = leftValues[i].compareTo(rightValues[i]) >= 0 ? 1 : 0;
              }
            }
            default -> throw new TypeException("Invalid Operator %s for %s and %s", oper.name(), leftType, rightType);
          }
          return new IntColumn(output);
        } else {
          throw new TypeException("Invalid Operator %s for %s and %s", oper.name(), leftType, rightType);
        }
      }
      case ColumnValue(String name, DataType _) -> {
        OptionalInt columnIndex = ArrayUtil.indexOf(input.attributes(),
                attr -> attr.name().equals(name)
        );
        if (columnIndex.isEmpty()) {
          throw new ColumnNotFoundException(name);
        }
        return input.columns()[columnIndex.getAsInt()].copySlice(start, sliceLength);
      }
    }
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
          case EQUALS, LT, GT, LE, GE -> DataType.INT;
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

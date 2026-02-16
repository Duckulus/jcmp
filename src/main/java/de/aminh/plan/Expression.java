package de.aminh.plan;

import de.aminh.data.Column;
import de.aminh.data.Column.IntColumn;
import de.aminh.data.Column.StringColumn;
import de.aminh.data.DataType;
import de.aminh.exceptions.ColumnNotFoundException;
import de.aminh.exceptions.TypeException;
import de.aminh.util.ArrayUtil;

import java.util.Arrays;
import java.util.OptionalInt;

public sealed interface Expression {

  record LiteralInt(int value) implements Expression {
  }

  record LiteralString(String value) implements Expression {
  }

  record ColumnValue(String name, DataType type) implements Expression {

  }

  record Sum(Expression left, Expression right) implements Expression {

  }

  default Column eval(RecordBatch input) {
    switch (this) {
      case LiteralInt(int value) -> {
        int[] output = new int[input.size()];
        Arrays.fill(output, value);
        return new IntColumn(output);
      }
      case LiteralString(String value) -> {
        String[] output = new String[input.size()];
        Arrays.fill(output, value);
        return new StringColumn(output);
      }
      case Sum(Expression left, Expression right) -> {
        DataType leftType = left.type();
        DataType rightType = right.type();
        if (leftType == DataType.INT && rightType == DataType.INT) {
          int[] leftValues = ((IntColumn) left.eval(input)).values();
          int[] rightValues = ((IntColumn) right.eval(input)).values();
          int[] output = new int[input.size()];
          for (int i = 0; i < output.length; i++) {
            output[i] = leftValues[i] + rightValues[i];
          }
          return new IntColumn(output);
        } else {
          throw new TypeException("Tried adding %s and %s", leftType, rightType);
        }
      }
      case ColumnValue(String name, DataType _) -> {
        OptionalInt columnIndex = ArrayUtil.indexOf(input.attributes(),
                attr -> attr.name().equals(name)
        );
        if (columnIndex.isEmpty()) {
          throw new ColumnNotFoundException(name);
        }
        return input.columns()[columnIndex.getAsInt()];
      }
    }
  }

  default DataType type() {
    return switch (this) {
      case LiteralInt _ -> DataType.INT;
      case LiteralString _ -> DataType.STRING;
      case Sum(Expression left, Expression right) -> {
        DataType leftType = left.type();
        DataType rightType = right.type();
        if (leftType == DataType.INT && rightType == DataType.INT) {
          yield DataType.INT;
        } else {
          throw new TypeException("Tried adding %s and %s", left, rightType);
        }
      }
      case ColumnValue(String _, DataType type) -> type;
    };
  }

}

package de.aminh.jcmp.vectorized.plan.expr;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.Column.DoubleColumn;
import de.aminh.jcmp.data.Column.IntColumn;
import de.aminh.jcmp.data.Column.StringColumn;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.exceptions.ColumnNotFoundException;
import de.aminh.jcmp.exceptions.TypeException;
import de.aminh.jcmp.plan.BinaryOperator;
import de.aminh.jcmp.vectorized.VectorPool;
import de.aminh.jcmp.vectorized.VectorizedPlanner;
import de.aminh.jcmp.plan.Expression;
import de.aminh.jcmp.util.ArrayUtil;

import java.util.Arrays;
import java.util.OptionalInt;

public interface VectorizedExpression {

  Column eval(RecordBatch input, VectorPool pool);

  DataType type();

  record LiteralInt(int value) implements VectorizedExpression {
    @Override
    public Column eval(RecordBatch input, VectorPool pool) {
      int[] output = pool.getIntVector();
      Arrays.fill(output, 0, input.size(), value);
      return new IntColumn(input.size(), output);
    }

    @Override
    public DataType type() {
      return DataType.INT;
    }
  }

  record LiteralDouble(double value) implements VectorizedExpression {
    @Override
    public Column eval(RecordBatch input, VectorPool pool) {
      double[] output = pool.getDoubleVector();
      Arrays.fill(output, 0, input.size(), value);
      return new DoubleColumn(input.size(), output);
    }

    @Override
    public DataType type() {
      return DataType.DOUBLE;
    }
  }

  record LiteralString(String value) implements VectorizedExpression {
    @Override
    public Column eval(RecordBatch input, VectorPool pool) {
      String[] output = pool.getStringVector();
      Arrays.fill(output, 0, input.size(), value);
      return new StringColumn(input.size(), output);
    }

    @Override
    public DataType type() {
      return DataType.STRING;
    }
  }

  record ColumnValue(String name, DataType type) implements VectorizedExpression {
    @Override
    public Column eval(RecordBatch input, VectorPool pool) {
      OptionalInt columnIndex = ArrayUtil.indexOf(input.attributes(),
              attr -> attr.name().equals(name)
      );
      if (columnIndex.isEmpty()) {
        throw new ColumnNotFoundException(name);
      }
      return input.columns()[columnIndex.getAsInt()].copySlice(0, input.size(), pool);
    }

    @Override
    public DataType type() {
      return type;
    }
  }

  static VectorizedExpression translateBinary(Expression.Binary binary) {
    VectorizedExpression left = VectorizedPlanner.translateExp(binary.left());
    VectorizedExpression right = VectorizedPlanner.translateExp(binary.right());
    DataType leftType = left.type();
    DataType rightType = right.type();

    BinaryOperator oper = binary.oper();

    if (leftType != rightType) {
      throw new TypeException("Type mismatch: Cannot apply %s on %s and %s", oper.name(), leftType, rightType);
    }

    return switch (leftType) {
      case INT -> switch (binary.oper()) {
        case PLUS -> new IntBinaryExpression.IntAddExpression(left, right);
        case MINUS -> new IntBinaryExpression.IntSubExpression(left, right);
        case TIMES -> new IntBinaryExpression.IntMulExpression(left, right);
        case DIVIDE -> new IntBinaryExpression.IntDivExpression(left, right);
        case EQUALS -> new IntBinaryExpression.IntEqualsExpression(left, right);
        case LT -> new IntBinaryExpression.IntLtExpression(left, right);
        case GT -> new IntBinaryExpression.IntGtExpression(left, right);
        case LE -> new IntBinaryExpression.IntLeExpression(left, right);
        case GE -> new IntBinaryExpression.IntGeExpression(left, right);
        case AND -> new IntBinaryExpression.IntAndExpression(left, right);
        case OR -> new IntBinaryExpression.IntOrExpression(left, right);
      };

      case DOUBLE -> switch (oper) {
        case PLUS -> new DoubleArithmeticExpression.DoubleAddExpression(left, right);
        case MINUS -> new DoubleArithmeticExpression.DoubleSubExpression(left, right);
        case TIMES -> new DoubleArithmeticExpression.DoubleMulExpression(left, right);
        case DIVIDE -> new DoubleArithmeticExpression.DoubleDivExpression(left, right);
        case EQUALS -> new DoubleComparisonExpression.DoubleEqualsExpression(left, right);
        case LT -> new DoubleComparisonExpression.DoubleLtExpression(left, right);
        case GT -> new DoubleComparisonExpression.DoubleGtExpression(left, right);
        case LE -> new DoubleComparisonExpression.DoubleLeExpression(left, right);
        case GE -> new DoubleComparisonExpression.DoubleGeExpression(left, right);
        default -> throw new TypeException("Invalid Operator %s for DOUBLE", oper.name());
      };

      case STRING -> switch (oper) {
        case EQUALS -> new StringComparisonExpression.StringEqualsExpression(left, right);
        case LT -> new StringComparisonExpression.StringLtExpression(left, right);
        case GT -> new StringComparisonExpression.StringGtExpression(left, right);
        case LE -> new StringComparisonExpression.StringLeExpression(left, right);
        case GE -> new StringComparisonExpression.StringGeExpression(left, right);
        default -> throw new TypeException("Invalid Operator %s for STRING", oper.name());
      };
    };
  }

}

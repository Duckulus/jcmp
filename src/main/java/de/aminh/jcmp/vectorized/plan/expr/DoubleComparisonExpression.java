package de.aminh.jcmp.vectorized.plan.expr;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.RecordBatch;

abstract class DoubleComparisonExpression implements VectorizedExpression {
  private final VectorizedExpression left;
  private final VectorizedExpression right;

  protected DoubleComparisonExpression(VectorizedExpression left, VectorizedExpression right) {
    this.left = left;
    this.right = right;
  }

  abstract void compute(double[] l, double[] r, int[] out);

  @Override
  public Column eval(RecordBatch input) {
    double[] l = ((Column.DoubleColumn) left.eval(input)).values();
    double[] r = ((Column.DoubleColumn) right.eval(input)).values();
    int[] out = new int[input.size()];

    compute(l, r, out);

    return new Column.IntColumn(out);
  }

  @Override
  public DataType type() { return DataType.INT; }

  static final class DoubleEqualsExpression extends DoubleComparisonExpression {
    DoubleEqualsExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] == r[i] ? 1 : 0;
    }
  }

  static final class DoubleLtExpression extends DoubleComparisonExpression {
    DoubleLtExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] < r[i] ? 1 : 0;
    }
  }

  static final class DoubleGtExpression extends DoubleComparisonExpression {
    DoubleGtExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] > r[i] ? 1 : 0;
    }
  }

  static final class DoubleLeExpression extends DoubleComparisonExpression {
    DoubleLeExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] <= r[i] ? 1 : 0;
    }
  }

  static final class DoubleGeExpression extends DoubleComparisonExpression {
    DoubleGeExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] >= r[i] ? 1 : 0;
    }
  }
}

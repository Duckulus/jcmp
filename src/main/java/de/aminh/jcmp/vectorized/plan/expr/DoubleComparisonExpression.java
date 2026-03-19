package de.aminh.jcmp.vectorized.plan.expr;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.vectorized.VectorPool;

public abstract class DoubleComparisonExpression implements VectorizedExpression {
  private final VectorizedExpression left;
  private final VectorizedExpression right;

  protected DoubleComparisonExpression(VectorizedExpression left, VectorizedExpression right) {
    this.left = left;
    this.right = right;
  }

  abstract void compute(double[] l, double[] r, int[] out, int size);

  @Override
  public Column eval(RecordBatch input, VectorPool pool) {
    Column.DoubleColumn leftCol = (Column.DoubleColumn) left.eval(input, pool);
    Column.DoubleColumn rightCol = (Column.DoubleColumn) right.eval(input, pool);

    double[] l = leftCol.values();
    double[] r = rightCol.values();
    int[] out = pool.getIntVector();

    int size = input.size();

    compute(l, r, out, size);

    leftCol.release(pool);
    rightCol.release(pool);

    return new Column.IntColumn(size, out);
  }

  @Override
  public DataType type() { return DataType.INT; }

  static final class DoubleEqualsExpression extends DoubleComparisonExpression {
    DoubleEqualsExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] == r[i] ? 1 : 0;
    }
  }

  static final class DoubleLtExpression extends DoubleComparisonExpression {
    DoubleLtExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] < r[i] ? 1 : 0;
    }
  }

  static final class DoubleGtExpression extends DoubleComparisonExpression {
    DoubleGtExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] > r[i] ? 1 : 0;
    }
  }

  static final class DoubleLeExpression extends DoubleComparisonExpression {
    DoubleLeExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] <= r[i] ? 1 : 0;
    }
  }

  static final class DoubleGeExpression extends DoubleComparisonExpression {
    DoubleGeExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] >= r[i] ? 1 : 0;
    }
  }
}
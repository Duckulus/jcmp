package de.aminh.jcmp.vectorized.plan.expr;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.vectorized.VectorPool;

public abstract class DoubleArithmeticExpression implements VectorizedExpression {
  private final VectorizedExpression left;
  private final VectorizedExpression right;

  protected DoubleArithmeticExpression(VectorizedExpression left, VectorizedExpression right) {
    this.left = left;
    this.right = right;
  }

  abstract void compute(double[] l, double[] r, double[] out, int size);

  @Override
  public Column eval(RecordBatch input, VectorPool pool) {
    Column.DoubleColumn leftCol = (Column.DoubleColumn) left.eval(input, pool);
    Column.DoubleColumn rightCol = (Column.DoubleColumn) right.eval(input, pool);

    double[] l = leftCol.values();
    double[] r = rightCol.values();
    double[] out = pool.getDoubleVector();

    int size = input.size();

    compute(l, r, out, size);

    leftCol.release(pool);
    rightCol.release(pool);

    return new Column.DoubleColumn(size, out);
  }

  @Override
  public DataType type() {
    return DataType.DOUBLE;
  }


  static final class DoubleAddExpression extends DoubleArithmeticExpression {
    DoubleAddExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(double[] l, double[] r, double[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] + r[i];
    }
  }

  static final class DoubleSubExpression extends DoubleArithmeticExpression {
    DoubleSubExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(double[] l, double[] r, double[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] - r[i];
    }
  }

  static final class DoubleMulExpression extends DoubleArithmeticExpression {
    DoubleMulExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(double[] l, double[] r, double[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] * r[i];
    }
  }

  static final class DoubleDivExpression extends DoubleArithmeticExpression {
    DoubleDivExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(double[] l, double[] r, double[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] / r[i];
    }
  }
}
package de.aminh.jcmp.vectorized.plan.expr;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.RecordBatch;

public abstract class DoubleArithmeticExpression implements VectorizedExpression {
  private final VectorizedExpression left;
  private final VectorizedExpression right;

  protected DoubleArithmeticExpression(VectorizedExpression left, VectorizedExpression right) {
    this.left = left;
    this.right = right;
  }

  abstract void compute(double[] l, double[] r, double[] out);

  @Override
  public Column evalSlice(RecordBatch input, int start, int end) {
    int sliceLength = end - start;
    double[] l = ((Column.DoubleColumn) left.evalSlice(input, start, end)).values();
    double[] r = ((Column.DoubleColumn) right.evalSlice(input, start, end)).values();
    double[] out = new double[sliceLength];

    compute(l, r, out);

    return new Column.DoubleColumn(out);
  }

  @Override
  public DataType type() { return DataType.DOUBLE; }


  static final class DoubleAddExpression extends DoubleArithmeticExpression {
    DoubleAddExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, double[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] + r[i];
    }
  }

  static final class DoubleSubExpression extends DoubleArithmeticExpression {
    DoubleSubExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, double[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] - r[i];
    }
  }

  static final class DoubleMulExpression extends DoubleArithmeticExpression {
    DoubleMulExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, double[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] * r[i];
    }
  }

  static final class DoubleDivExpression extends DoubleArithmeticExpression {
    DoubleDivExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(double[] l, double[] r, double[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] / r[i];
    }
  }
}

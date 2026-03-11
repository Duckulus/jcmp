package de.aminh.jcmp.vectorized.plan.expr;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.RecordBatch;

public abstract class IntBinaryExpression implements VectorizedExpression {

  private final VectorizedExpression left;
  private final VectorizedExpression right;

  protected IntBinaryExpression(VectorizedExpression left, VectorizedExpression right) {
    this.left = left;
    this.right = right;
  }

  abstract void compute(int[] l, int[] r, int[] out);


  @Override
  public Column evalSlice(RecordBatch input, int start, int end) {
    int sliceLength = end - start;
    int[] l = ((Column.IntColumn) left.evalSlice(input, start, end)).values();
    int[] r = ((Column.IntColumn) right.evalSlice(input, start, end)).values();
    int[] out = new int[sliceLength];

    compute(l, r, out);

    return new Column.IntColumn(out);
  }

  @Override
  public DataType type() {
    return DataType.INT;
  }

  static final class IntAddExpression extends IntBinaryExpression {
    IntAddExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] + r[i];
    }
  }

  static final class IntSubExpression extends IntBinaryExpression {
    IntSubExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] - r[i];
    }
  }

  static final class IntMulExpression extends IntBinaryExpression {
    IntMulExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] * r[i];
    }
  }

  static final class IntDivExpression extends IntBinaryExpression {
    IntDivExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] / r[i]; // <-- Hier war vorher ein *
    }
  }

  static final class IntEqualsExpression extends IntBinaryExpression {
    IntEqualsExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] == r[i] ? 1 : 0;
    }
  }

  static final class IntLtExpression extends IntBinaryExpression {
    IntLtExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] < r[i] ? 1 : 0;
    }
  }

  static final class IntGtExpression extends IntBinaryExpression {
    IntGtExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] > r[i] ? 1 : 0;
    }
  }

  static final class IntLeExpression extends IntBinaryExpression {
    IntLeExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] <= r[i] ? 1 : 0;
    }
  }

  static final class IntGeExpression extends IntBinaryExpression {
    IntGeExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = l[i] >= r[i] ? 1 : 0;
    }
  }

  static final class IntAndExpression extends IntBinaryExpression {
    IntAndExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(int[] l, int[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = (l[i] == 1 && r[i] == 1) ? 1 : 0;
    }
  }

  static final class IntOrExpression extends IntBinaryExpression {
    IntOrExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(int[] l, int[] r, int[] out) {
      for (int i = 0; i < out.length; i++) out[i] = (l[i] == 1 || r[i] == 1) ? 1 : 0;
    }
  }
}

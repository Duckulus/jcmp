package de.aminh.jcmp.vectorized.plan.expr;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.vectorized.VectorPool;

public abstract class IntBinaryExpression implements VectorizedExpression {

  private final VectorizedExpression left;
  private final VectorizedExpression right;

  protected IntBinaryExpression(VectorizedExpression left, VectorizedExpression right) {
    this.left = left;
    this.right = right;
  }

  abstract void compute(int[] l, int[] r, int[] out, int size);

  @Override
  public Column eval(RecordBatch input, VectorPool pool) {
    Column.IntColumn leftCol = (Column.IntColumn) left.eval(input, pool);
    Column.IntColumn rightCol = (Column.IntColumn) right.eval(input, pool);

    int[] l = leftCol.values();
    int[] r = rightCol.values();
    int[] out = pool.getIntVector();

    int size = input.size();

    compute(l, r, out, size);

    leftCol.release(pool);
    rightCol.release(pool);

    return new Column.IntColumn(size, out);
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
    void compute(int[] l, int[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] + r[i];
    }
  }

  static final class IntSubExpression extends IntBinaryExpression {
    IntSubExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] - r[i];
    }
  }

  static final class IntMulExpression extends IntBinaryExpression {
    IntMulExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] * r[i];
    }
  }

  static final class IntDivExpression extends IntBinaryExpression {
    IntDivExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] / r[i];
    }
  }

  static final class IntEqualsExpression extends IntBinaryExpression {
    IntEqualsExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] == r[i] ? 1 : 0;
    }
  }

  static final class IntLtExpression extends IntBinaryExpression {
    IntLtExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] < r[i] ? 1 : 0;
    }
  }

  static final class IntGtExpression extends IntBinaryExpression {
    IntGtExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] > r[i] ? 1 : 0;
    }
  }

  static final class IntLeExpression extends IntBinaryExpression {
    IntLeExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] <= r[i] ? 1 : 0;
    }
  }

  static final class IntGeExpression extends IntBinaryExpression {
    IntGeExpression(VectorizedExpression left, VectorizedExpression right) {
      super(left, right);
    }

    @Override
    void compute(int[] l, int[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = l[i] >= r[i] ? 1 : 0;
    }
  }

  static final class IntAndExpression extends IntBinaryExpression {
    IntAndExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(int[] l, int[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = (l[i] == 1 && r[i] == 1) ? 1 : 0;
    }
  }

  static final class IntOrExpression extends IntBinaryExpression {
    IntOrExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(int[] l, int[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) out[i] = (l[i] == 1 || r[i] == 1) ? 1 : 0;
    }
  }
}
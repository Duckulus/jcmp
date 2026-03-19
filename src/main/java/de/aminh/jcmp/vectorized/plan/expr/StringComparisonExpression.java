package de.aminh.jcmp.vectorized.plan.expr;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.vectorized.VectorPool;

public abstract class StringComparisonExpression implements VectorizedExpression {
  private final VectorizedExpression left;
  private final VectorizedExpression right;

  protected StringComparisonExpression(VectorizedExpression left, VectorizedExpression right) {
    this.left = left;
    this.right = right;
  }

  abstract void compute(String[] l, String[] r, int[] out, int size);

  @Override
  public Column eval(RecordBatch input, VectorPool pool) {
    Column.StringColumn leftCol = (Column.StringColumn) left.eval(input, pool);
    Column.StringColumn rightCol = (Column.StringColumn) right.eval(input, pool);

    String[] l = leftCol.values();
    String[] r = rightCol.values();
    int[] out = pool.getIntVector();

    int size = input.size();

    compute(l, r, out, size);

    leftCol.release(pool);
    rightCol.release(pool);

    return new Column.IntColumn(size, out);
  }

  @Override
  public DataType type() { return DataType.INT; }

  static final class StringEqualsExpression extends StringComparisonExpression {
    StringEqualsExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(String[] l, String[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) {
        out[i] = l[i].equals(r[i]) ? 1 : 0;
      }
    }
  }

  static final class StringLtExpression extends StringComparisonExpression {
    StringLtExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(String[] l, String[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) {
        out[i] = l[i].compareTo(r[i]) < 0 ? 1 : 0;
      }
    }
  }

  static final class StringGtExpression extends StringComparisonExpression {
    StringGtExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(String[] l, String[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) {
        out[i] = l[i].compareTo(r[i]) > 0 ? 1 : 0;
      }
    }
  }

  static final class StringLeExpression extends StringComparisonExpression {
    StringLeExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(String[] l, String[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) {
        out[i] = l[i].compareTo(r[i]) <= 0 ? 1 : 0;
      }
    }
  }

  static final class StringGeExpression extends StringComparisonExpression {
    StringGeExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(String[] l, String[] r, int[] out, int size) {
      for (int i = 0; i < size; i++) {
        out[i] = l[i].compareTo(r[i]) >= 0 ? 1 : 0;
      }
    }
  }
}
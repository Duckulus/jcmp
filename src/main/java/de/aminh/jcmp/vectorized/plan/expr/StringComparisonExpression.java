package de.aminh.jcmp.vectorized.plan.expr;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.RecordBatch;

public abstract class StringComparisonExpression implements VectorizedExpression {
  private final VectorizedExpression left;
  private final VectorizedExpression right;

  protected StringComparisonExpression(VectorizedExpression left, VectorizedExpression right) {
    this.left = left;
    this.right = right;
  }

  abstract void compute(String[] l, String[] r, int[] out);

  @Override
  public Column eval(RecordBatch input) {
    String[] l = ((Column.StringColumn) left.eval(input)).values();
    String[] r = ((Column.StringColumn) right.eval(input)).values();
    int[] out = new int[input.size()];

    compute(l, r, out);

    return new Column.IntColumn(out);
  }

  @Override
  public DataType type() { return DataType.INT; }


  static final class StringEqualsExpression extends StringComparisonExpression {
    StringEqualsExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(String[] l, String[] r, int[] out) {
      for (int i = 0; i < out.length; i++) {
        out[i] = l[i].equals(r[i]) ? 1 : 0;
      }
    }
  }

  static final class StringLtExpression extends StringComparisonExpression {
    StringLtExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(String[] l, String[] r, int[] out) {
      for (int i = 0; i < out.length; i++) {
        out[i] = l[i].compareTo(r[i]) < 0 ? 1 : 0;
      }
    }
  }

  static final class StringGtExpression extends StringComparisonExpression {
    StringGtExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(String[] l, String[] r, int[] out) {
      for (int i = 0; i < out.length; i++) {
        out[i] = l[i].compareTo(r[i]) > 0 ? 1 : 0;
      }
    }
  }

  static final class StringLeExpression extends StringComparisonExpression {
    StringLeExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(String[] l, String[] r, int[] out) {
      for (int i = 0; i < out.length; i++) {
        out[i] = l[i].compareTo(r[i]) <= 0 ? 1 : 0;
      }
    }
  }

  static final class StringGeExpression extends StringComparisonExpression {
    StringGeExpression(VectorizedExpression left, VectorizedExpression right) { super(left, right); }
    @Override void compute(String[] l, String[] r, int[] out) {
      for (int i = 0; i < out.length; i++) {
        out[i] = l[i].compareTo(r[i]) >= 0 ? 1 : 0;
      }
    }
  }
}
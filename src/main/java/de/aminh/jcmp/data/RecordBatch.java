package de.aminh.jcmp.data;

import de.aminh.jcmp.util.TablePrinter;
import de.aminh.jcmp.vectorized.VectorPool;
import org.jspecify.annotations.NonNull;

public record RecordBatch(int size, Attribute[] attributes, Column[] columns) {

  public static RecordBatch empty(Attribute[] attributes, VectorPool pool) {
    Column[] columns = new Column[attributes.length];
    for (int i = 0; i < columns.length; i++) {
      columns[i] = attributes[i].type().createColumn(0, pool);
    }
    return new RecordBatch(
            0,
            attributes,
            columns
    );
  }

  public RecordBatch merge(RecordBatch other) {
    Column[] newColumns = new Column[this.columns.length];
    for (int i = 0; i < this.columns.length; i++) {
      newColumns[i] = this.columns[i].merge(other.columns[i]);
    }
    return new RecordBatch(this.size + other.size, this.attributes, newColumns);
  }

  public RecordBatch copyMask(int[] mask, VectorPool pool) {
    int matches = 0;
    for(int value : mask) {
      if (value == 1) matches++;
    }
    Column[] newColumns = new Column[columns.length];
    for(int i = 0; i < columns.length; i++) {
      newColumns[i] = columns[i].copyMask(mask, matches, pool);
    }
    return new RecordBatch(matches, attributes, newColumns);
  }

  public void release(VectorPool pool) {
    for (Column col : columns) {
      col.release(pool);
    }
  }

  @Override
  public @NonNull String toString() {
    return TablePrinter.format(this, 100);
  }

}

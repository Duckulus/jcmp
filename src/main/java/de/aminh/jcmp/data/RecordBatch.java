package de.aminh.jcmp.data;

import de.aminh.jcmp.util.TablePrinter;
import org.jspecify.annotations.NonNull;

public record RecordBatch(int size, Attribute[] attributes, Column[] columns) {

  public static RecordBatch empty(Attribute[] attributes) {
    Column[] columns = new Column[attributes.length];
    for (int i = 0; i < columns.length; i++) {
      columns[i] = attributes[i].type().createColumn(0);
    }
    return new RecordBatch(
            0,
            attributes,
            columns
    );
  }

  @Override
  public @NonNull String toString() {
    return TablePrinter.format(this, 100);
  }

}

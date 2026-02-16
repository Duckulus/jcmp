package de.aminh.plan;

import de.aminh.data.Attribute;
import de.aminh.data.Column;

public record RecordBatch(int size, Attribute[] attributes, Column[] columns) {

  public static RecordBatch newBatch(int size) {
    return new RecordBatch(
            size,
            new Attribute[0],
            new Column[0]
    );
  }

}

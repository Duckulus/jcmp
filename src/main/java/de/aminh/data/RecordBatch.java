package de.aminh.data;

public record RecordBatch(int size, Attribute[] attributes, Column[] columns) {

  public static RecordBatch empty(Attribute[] attributes) {
    return new RecordBatch(
            0,
            attributes,
            new Column[0]
    );
  }

}

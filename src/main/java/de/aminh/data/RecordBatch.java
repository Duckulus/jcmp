package de.aminh.data;

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

}

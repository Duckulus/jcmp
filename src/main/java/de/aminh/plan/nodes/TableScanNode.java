package de.aminh.plan.nodes;

import de.aminh.Configuration;
import de.aminh.data.Attribute;
import de.aminh.data.Column;
import de.aminh.data.Table;
import de.aminh.exceptions.ColumnNotFoundException;
import de.aminh.plan.PlanNode;
import de.aminh.plan.RecordBatch;

import java.util.List;

public class TableScanNode implements PlanNode {

  private final List<String> columnNames;
  private final int batchSize;

  private final Attribute[] inputAttributes;
  private final Column[] inputColumns;

  private int cursor = 0;
  private final int totalRows;

  public TableScanNode(Table table, List<String> columnNames) {
    this(table, columnNames, Configuration.BATCH_SIZE);
  }

  public TableScanNode(Table table, List<String> columnNames, int batchSize) {
    if (columnNames.isEmpty()) {
      throw new IllegalArgumentException("Table scan needs at least 1 column");
    }
    this.columnNames = columnNames;
    this.batchSize = batchSize;

    int numCols = columnNames.size();

    // We are assuming that all the provided columns have the same length since they are part of the same logical table
    this.totalRows = table.getColumn(columnNames.getFirst()).length();

    inputAttributes = new Attribute[numCols];
    inputColumns = new Column[numCols];
    for (int i = 0; i < numCols; i++) {
      String colName = columnNames.get(i);

      Attribute attribute = table.getAttribute(colName);
      if (attribute == null) throw new ColumnNotFoundException(colName);
      inputAttributes[i] = attribute;

      Column column = table.getColumn(colName);
      if (column == null) throw new ColumnNotFoundException(colName);
      inputColumns[i] = column;
    }
  }

  @Override
  public void open() {

  }

  @Override
  public RecordBatch next() {
    if (cursor >= totalRows) {
      return null;
    }
    int rowCount = Math.min(batchSize, totalRows - cursor);
    Column[] outputColumns = new Column[columnNames.size()];
    for (int i = 0; i < columnNames.size(); i++) {
      outputColumns[i] = inputColumns[i].copySlice(cursor, rowCount);
    }
    cursor += rowCount;
    return new RecordBatch(rowCount, inputAttributes, outputColumns);
  }

  @Override
  public void close() {

  }
}

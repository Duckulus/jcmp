package de.aminh.jcmp.vectorized.impl;

import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.exceptions.ColumnNotFoundException;
import de.aminh.jcmp.vectorized.VectorizedExecutor;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode.TableScanNode;

import java.util.List;

public class TableScanExecutor implements VectorizedExecutor {

  private final TableScanNode planNode;

  private Attribute[] inputAttributes;
  private Column[] inputColumns;

  private int cursor = 0;
  private int totalRows;

  public TableScanExecutor(TableScanNode planNode) {
    this.planNode = planNode;
  }

  @Override
  public void init() {
    if (planNode.columnNames().isEmpty()) {
      throw new IllegalArgumentException("Table scan needs at least 1 column");
    }

    List<String> columnNames = planNode.columnNames();
    int numCols = columnNames.size();

    Table table = planNode.table();
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
  public RecordBatch next() {
    if (cursor >= totalRows) {
      return null;
    }
    int rowCount = Math.min(planNode.batchSize(), totalRows - cursor);
    List<String> columnNames = planNode.columnNames();
    Column[] outputColumns = new Column[columnNames.size()];
    for (int i = 0; i < columnNames.size(); i++) {
      outputColumns[i] = inputColumns[i].copySlice(cursor, rowCount);
    }
    cursor += rowCount;
    return new RecordBatch(rowCount, inputAttributes, outputColumns);
  }

  @Override
  public VectorizedPlanNode planNode() {
    return planNode;
  }

}

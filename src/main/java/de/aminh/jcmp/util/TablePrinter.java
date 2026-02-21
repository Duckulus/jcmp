package de.aminh.jcmp.util;

import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.Column.DoubleColumn;
import de.aminh.jcmp.data.Column.IntColumn;
import de.aminh.jcmp.data.Column.StringColumn;
import de.aminh.jcmp.data.RecordBatch;
import org.jspecify.annotations.NonNull;

public class TablePrinter {

  public static @NonNull String format(RecordBatch batch, int rowLimit) {
    if (batch.attributes() == null || batch.attributes().length == 0) {
      return "Empty RecordBatch (0 columns)";
    }

    int printLimit = Math.min(batch.size(), rowLimit);
    int[] colWidths = new int[batch.attributes().length];

    for (int c = 0; c < batch.attributes().length; c++) {
      colWidths[c] = batch.attributes()[c].name().length();
      for (int r = 0; r < printLimit; r++) {
        String val = getValueAsString(batch.columns()[c], r);
        if (val != null && val.length() > colWidths[c]) {
          colWidths[c] = val.length();
        }
      }
    }

    StringBuilder sb = new StringBuilder();
    String separator = buildSeparator(colWidths);

    sb.append(separator).append("\n|");
    for (int c = 0; c < batch.attributes().length; c++) {
      sb.append(String.format(" %-" + colWidths[c] + "s |", batch.attributes()[c].name()));
    }
    sb.append("\n").append(separator).append("\n");

    for (int r = 0; r < printLimit; r++) {
      sb.append("|");
      for (int c = 0; c < batch.attributes().length; c++) {
        String val = getValueAsString(batch.columns()[c], r);
        sb.append(String.format(" %-" + colWidths[c] + "s |", val == null ? "null" : val));
      }
      sb.append("\n");
    }

    sb.append(separator);

    if (batch.size() > printLimit) {
      sb.append(String.format("\n... and %d more tuples ...", batch.size() - printLimit));
    }

    return sb.toString();
  }

  private static String buildSeparator(int[] colWidths) {
    StringBuilder sep = new StringBuilder("+");
    for (int width : colWidths) {
      sep.append("-".repeat(width + 2)).append("+");
    }
    return sep.toString();
  }

  private static String getValueAsString(Column col, int row) {
    return switch (col) {
      case IntColumn ic -> String.valueOf(ic.values()[row]);
      case DoubleColumn dc -> String.valueOf(dc.values()[row]);
      case StringColumn sc -> sc.values()[row];
    };
  }
}
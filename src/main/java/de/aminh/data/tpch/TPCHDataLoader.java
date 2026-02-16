package de.aminh.data.tpch;

import de.aminh.data.Attribute;
import de.aminh.data.Column;
import de.aminh.data.Table;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TPCHDataLoader {

  private final Map<String, Column> allColumns = new HashMap<>();

  public Table loadData() {
    for (TPCHSchema schema : TPCHSchema.values()) {
      readTable(schema);
    }
    return new TPCHTable(allColumns);
  }

  private void readTable(TPCHSchema schema) {
    String path = getFilePath(schema);
    int nLines= countLines(path);

    Column[] columns = new Column[schema.getAttributes().size()];
    for (int i = 0; i < schema.getAttributes().size(); i++) {
      Attribute attribute = schema.getAttributes().get(i);
      Column col = attribute.type().createColumn(nLines);
      columns[i] = col;
      allColumns.put(attribute.name(), col);
    }

    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path))) {
      String line;
      int currentRow = 0;
      while ((line = bufferedReader.readLine()) != null) {
        String[] split = line.split("\\|");
        for (int i = 0; i < columns.length; i++) {
          columns[i].parseAndSetValue(currentRow, split[i]);
        }
        currentRow++;
      }
      IO.println("Finished reading %d rows of table %s".formatted(nLines, schema.getTableName()));
    } catch (IOException e) {
      throw new RuntimeException("There was an error reading in the data", e);
    }
  }

  private int countLines(String path) {
    try (InputStream is = new BufferedInputStream(Files.newInputStream(Path.of(path)))) {
      byte[] c = new byte[1024 * 32];
      int count = 0;
      int readChars;
      boolean empty = true;

      while ((readChars = is.read(c)) != -1) {
        empty = false;
        for (int i = 0; i < readChars; ++i) {
          if (c[i] == '\n') {
            ++count;
          }
        }
      }
      return (count == 0 && !empty) ? 1 : count;
    } catch (IOException e) {
      throw new RuntimeException("Error while counting lines", e);
    }
  }

  private static String getFilePath(TPCHSchema schema) {
    return "./data/tpch/" + schema.getTableName() + ".tbl";
  }

}

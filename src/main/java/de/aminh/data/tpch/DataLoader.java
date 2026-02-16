package de.aminh.data.tpch;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class DataLoader {

  public static TPCHData loadData() {
    TPCHData data = new TPCHData();
    for (TableSchema schema : TableSchema.values()) {
      readTable(schema, data);
    }
    return data;
  }

  private static void readTable(TableSchema schema, TPCHData data) {
    String path = getFilePath(schema);
    int nLines= countLines(path);

    Column[] columns = new Column[schema.getAttributes().size()];
    for (int i = 0; i < schema.getAttributes().size(); i++) {
      Attribute attribute = schema.getAttributes().get(i);
      Column col = attribute.type().createColumn(nLines);
      columns[i] = col;
      data.addColumn(attribute.name(), col);
    }

    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path))) {
      String line;
      int currentRow = 0;
      while ((line = bufferedReader.readLine()) != null) {
        String[] split = line.split("\\|");
        for (int i = 0; i < columns.length; i++) {
          columns[i].parseAndAdd(currentRow, split[i]);
        }
        currentRow++;
      }
      IO.println("Finished reading %d rows of table %s".formatted(nLines, schema.getTableName()));
    } catch (IOException e) {
      throw new RuntimeException("There was an error reading in the data", e);
    }
  }

  private static int countLines(String path) {
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

  private static String getFilePath(TableSchema schema) {
    return "./data/tpch/" + schema.getTableName() + ".tbl";
  }

}

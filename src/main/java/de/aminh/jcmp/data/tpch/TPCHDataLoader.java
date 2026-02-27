package de.aminh.jcmp.data.tpch;

import de.aminh.jcmp.Main;
import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.Column;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class TPCHDataLoader {

  private static final int BUFFER_SIZE = 64 * 1024;

  @SuppressWarnings("unused")
  public static void writeBinaryData(TPCHTable table, String fileName) {
    try (DataOutputStream out = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(binaryFileName(fileName)), BUFFER_SIZE))) {
      for(TPCHSchema schema : TPCHSchema.values()) {
        for(Attribute attribute : schema.getAttributes()) {
          Column column = table.getColumn(attribute.name());
          out.writeInt(column.length());
          switch (column) {
            case Column.IntColumn(int[] values) -> {
              for (int value : values) {
                out.writeInt(value);
              }
            }
            case Column.DoubleColumn(double[] values) -> {
              for (double value : values) {
                out.writeDouble(value);
              }
            }
            case Column.StringColumn(String[] values) -> {
              for (String value : values) {
                out.writeUTF(value);
              }
            }
          }
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("There was an error writing out data", e);
    }
  }

  public static TPCHTable loadBinaryData(String fileName) {
    long currentTimeMillis = System.currentTimeMillis();
    try (DataInputStream in = new DataInputStream(
            new BufferedInputStream(new FileInputStream(binaryFileName(fileName)), BUFFER_SIZE))) {
      Map<String, Column> allColumns = new HashMap<>();
      for(TPCHSchema schema : TPCHSchema.values()) {
        for(Attribute attribute : schema.getAttributes()) {
          int columnLength = in.readInt();
          if (!Main.COLUMN_WHITELIST.contains(attribute.name())) {
            switch (attribute.type()) {
              case INT -> in.skipNBytes((long) columnLength * Integer.BYTES);
              case DOUBLE -> in.skipNBytes((long) columnLength * Double.BYTES);
              case STRING -> {
                for (int i = 0; i < columnLength; i++) {
                  int utfLength = in.readUnsignedShort();
                  in.skipNBytes(utfLength);
                }
              }
            }
            continue;
          }
          Column column = attribute.type().createColumn(columnLength);
          allColumns.put(attribute.name(), column);
          switch (column) {
            case Column.IntColumn(int[] values) -> {
              for(int i = 0; i < values.length; i++) {
                values[i] = in.readInt();
              }
            }
            case Column.DoubleColumn(double[] values) -> {
              for(int i = 0; i < values.length; i++) {
                values[i] = in.readDouble();
              }
            }
            case Column.StringColumn(String[] values) -> {
              for(int i = 0; i < values.length; i++) {
                values[i] = in.readUTF();
              }
            }
          }
        }
      }
      IO.println("Finished reading %s in %dms".formatted(fileName, System.currentTimeMillis() - currentTimeMillis));
      return new TPCHTable(allColumns);
    } catch (IOException e) {
      throw new RuntimeException("There was an error reading in data", e);
    }
  }

  private static String binaryFileName(String fileName) {
    return "./data/tpch/%s".formatted(fileName);
  }

  @SuppressWarnings("unused")
  public static TPCHTable loadCsvData() {
    Map<String, Column> allColumns = new HashMap<>();
    for (TPCHSchema schema : TPCHSchema.values()) {
      readCsvTable(schema, allColumns);
    }
    return new TPCHTable(allColumns);
  }

  private static void readCsvTable(TPCHSchema schema, Map<String, Column> allColumns) {
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
      long currentTimeMillis = System.currentTimeMillis();
      while ((line = bufferedReader.readLine()) != null) {
        String[] split = line.split("\\|");
        for (int i = 0; i < columns.length; i++) {
          columns[i].parseAndSetValue(currentRow, split[i]);
        }
        currentRow++;
      }
      IO.println("Finished reading %d rows of table %s in %dms".formatted(nLines, schema.getTableName(), System.currentTimeMillis() - currentTimeMillis));
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

  private static String getFilePath(TPCHSchema schema) {
    return "./data/tpch/" + schema.getTableName() + ".tbl";
  }

}

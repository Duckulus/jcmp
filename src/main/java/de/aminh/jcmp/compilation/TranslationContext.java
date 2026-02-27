package de.aminh.jcmp.compilation;

import de.aminh.jcmp.data.Table;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TranslationContext {

  private final Table table;

  private final StringBuilder prelude;
  private final StringBuilder code;

  private final Map<String, String> symbolTable = new HashMap<>();

  private final AtomicInteger symbolId = new AtomicInteger(0);

  public TranslationContext(Table table) {
    this.table = table;
    this.prelude = new StringBuilder();
    this.code = new StringBuilder();
  }

  public StringBuilder prelude() {
    return prelude;
  }

  public StringBuilder code() {
    return code;
  }

  public Table table() {
    return table;
  }

  public void declareSymbol(String columnName, String symbolName) {
    symbolTable.put(columnName, symbolName);
  }

  public String resolveSymbol(String columnName) {
    String symbolName = symbolTable.get(columnName);
    if (symbolName == null) {
      throw new IllegalArgumentException("Symbol for column \"%s\" not found".formatted(columnName));
    }
    return symbolName;
  }

  public int nextId() {
    return symbolId.getAndIncrement();
  }
}

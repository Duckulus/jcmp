package de.aminh.jcmp.compilation;

import de.aminh.jcmp.data.Table;

public class TranslationContext {

  private final Table table;

  private final StringBuilder prelude;
  private final StringBuilder code;
  private String currentIndexVar;

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

  public String currentIndexVar() {
    return currentIndexVar;
  }

  public void setCurrentIndexVar(String currentIndexVar) {
    this.currentIndexVar = currentIndexVar;
  }

  public Table table() {
    return table;
  }

}

package de.aminh.jcmp.exceptions;

public class ColumnNotFoundException extends RuntimeException {
  public ColumnNotFoundException(String name) {
    super(String.format("Column %s not found", name));
  }
}

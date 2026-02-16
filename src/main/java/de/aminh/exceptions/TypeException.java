package de.aminh.exceptions;

public class TypeException extends RuntimeException {
  public TypeException(String message, Object... args) {
    super(String.format(message, args));
  }
}

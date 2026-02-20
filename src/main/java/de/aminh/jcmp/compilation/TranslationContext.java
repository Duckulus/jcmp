package de.aminh.jcmp.compilation;

public record TranslationContext(StringBuilder prelude, StringBuilder code) {

  public TranslationContext() {
    this(new StringBuilder(), new StringBuilder());
  }

}

package de.aminh.jcmp.compilation.impl;

import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;

public class RootTranslator implements NodeTranslator {

  private NodeTranslator input;

  public void setInput(NodeTranslator input) {
    this.input = input;
  }

  @Override
  public void produce(TranslationContext ctx) {
    input.produce(ctx);
  }

  @Override
  public void consume(TranslationContext ctx) {

  }

}

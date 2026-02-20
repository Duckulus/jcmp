package de.aminh.jcmp.compilation;

public interface NodeTranslator {

  void produce(TranslationContext ctx);

  void consume(TranslationContext ctx);

}

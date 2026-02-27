package de.aminh.jcmp.compilation;

import de.aminh.jcmp.plan.PlanNode;

public interface NodeTranslator {

  void produce(TranslationContext ctx);

  void consume(TranslationContext ctx);

  PlanNode getPlanNode();

}

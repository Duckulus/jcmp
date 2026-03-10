package de.aminh.jcmp.compilation;

import de.aminh.jcmp.plan.logical.PlanNode;

public interface NodeTranslator {

  void produce(TranslationContext ctx);

  void consume(TranslationContext ctx, NodeTranslator caller);

  PlanNode getPlanNode();

}

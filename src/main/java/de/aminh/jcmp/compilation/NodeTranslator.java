package de.aminh.jcmp.compilation;

import de.aminh.jcmp.plan.PlanNode;

import java.util.List;

public interface NodeTranslator {

  void produce(TranslationContext ctx);

  void consume(TranslationContext ctx, List<String> inputColumns);

  PlanNode getPlanNode();

}

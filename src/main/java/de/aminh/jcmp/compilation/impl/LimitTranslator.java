package de.aminh.jcmp.compilation.impl;

import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.PlanNode.LimitNode;

import java.util.List;

public class LimitTranslator implements NodeTranslator {

  private final LimitNode planNode;

  private NodeTranslator input;
  private final NodeTranslator parent;

  public LimitTranslator(LimitNode planNode, NodeTranslator parent) {
    this.planNode = planNode;
    this.parent = parent;
  }

  public void setInput(NodeTranslator input) {
    this.input = input;
  }

  @Override
  public void produce(TranslationContext ctx) {
    input.produce(ctx);
    ctx.prelude().append("int limit = 0;\n");
  }

  @Override
  public void consume(TranslationContext ctx, List<String> inputColumns) {
    ctx.code().append("""
            if (limit >= %d) {
              break;
            }
            limit++;
            """.formatted(planNode.limit()));
    parent.consume(ctx, inputColumns);
  }

  @Override
  public PlanNode getPlanNode() {
    return planNode;
  }
}

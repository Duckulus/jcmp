package de.aminh.jcmp.compilation.impl;

import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.PlanNode.LimitNode;

public class LimitTranslator implements NodeTranslator {

  private final LimitNode planNode;

  private NodeTranslator input;
  private final NodeTranslator parent;

  int limitId;

  public LimitTranslator(LimitNode planNode, NodeTranslator parent) {
    this.planNode = planNode;
    this.parent = parent;
  }

  public void setInput(NodeTranslator input) {
    this.input = input;
  }

  @Override
  public void produce(TranslationContext ctx) {
    limitId = ctx.nextId();
    input.produce(ctx);
    ctx.prelude().append("int limit_%d = 0;\n".formatted(limitId));
  }

  @Override
  public void consume(TranslationContext ctx, NodeTranslator caller) {
    ctx.code().append("""
            if (limit_%d >= %d) {
              break;
            }
            limit_%d++;
            """.formatted(limitId, planNode.limit(), limitId));
    parent.consume(ctx, this);
  }

  @Override
  public PlanNode getPlanNode() {
    return planNode;
  }
}

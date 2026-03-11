package de.aminh.jcmp.compilation.impl;

import de.aminh.jcmp.compilation.JavaCodeGen;
import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.PlanNode.SelectionNode;

public class SelectionTranslator implements NodeTranslator {

  private final SelectionNode planNode;

  private NodeTranslator input;
  private final NodeTranslator parent;

  public SelectionTranslator(SelectionNode planNode, NodeTranslator parent) {
    this.planNode = planNode;
    this.parent = parent;
  }

  public void setInput(NodeTranslator input) {
    this.input = input;
  }

  @Override
  public void produce(TranslationContext ctx) {
    input.produce(ctx);
  }

  @Override
  public void consume(TranslationContext ctx, NodeTranslator caller) {
    ctx.code().append(
            "if (%s) {\n".formatted(
                    JavaCodeGen.translateExpression(planNode.predicate(), ctx)
            )
    );
    parent.consume(ctx, this);
    ctx.code().append("}\n");
  }

  @Override
  public PlanNode getPlanNode() {
    return planNode;
  }
}

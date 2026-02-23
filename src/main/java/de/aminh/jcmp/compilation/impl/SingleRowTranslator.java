package de.aminh.jcmp.compilation.impl;

import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.PlanNode.SingleRowNode;

import java.util.ArrayList;
import java.util.List;

public class SingleRowTranslator implements NodeTranslator {

  private final SingleRowNode singleRowNode;

  private final NodeTranslator parent;

  public SingleRowTranslator(SingleRowNode singleRowNode, NodeTranslator parent) {
    this.singleRowNode = singleRowNode;
    this.parent = parent;
  }

  @Override
  public void produce(TranslationContext ctx) {
    parent.consume(ctx, new ArrayList<>());
  }

  @Override
  public void consume(TranslationContext ctx, List<String> inputColumns) {

  }

  @Override
  public PlanNode getPlanNode() {
    return singleRowNode;
  }
}

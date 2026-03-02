package de.aminh.jcmp.compilation.impl;

import de.aminh.jcmp.compilation.JavaCodeGen;
import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;
import de.aminh.jcmp.plan.Expression;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.PlanNode.ProjectionNode;

public class ProjectionTranslator implements NodeTranslator {

  private final ProjectionNode planNode;

  private NodeTranslator input;
  private final NodeTranslator parent;

  public ProjectionTranslator(ProjectionNode planNode, NodeTranslator parent) {
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
    for (int i = 0; i < planNode.expressions().length; i++) {
      String variableName = "proj" + ctx.nextId();
      String outputColName = planNode.outputSchema()[i].name();
      Expression expr = planNode.expressions()[i];
      String projectedValue = JavaCodeGen.translateExpression(expr, ctx);
      ctx.code().append("%s %s = %s;\n"
              .formatted(JavaCodeGen.getTypeName(expr.type()), variableName, projectedValue));
      ctx.declareSymbol(outputColName, variableName);
    }
    parent.consume(ctx, this);
  }

  @Override
  public PlanNode getPlanNode() {
    return planNode;
  }
}

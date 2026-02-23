package de.aminh.jcmp.compilation.impl;

import de.aminh.jcmp.compilation.JavaCodeGen;
import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;
import de.aminh.jcmp.plan.Expression;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.PlanNode.ProjectionNode;

import java.util.ArrayList;
import java.util.List;

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
  public void consume(TranslationContext ctx, List<String> inputColumns) {
    List<String> outputColumns = new ArrayList<>();
    for (int i = 0; i < planNode.expressions().length; i++) {
      String variableName = "proj_" + i;
      outputColumns.add(variableName);
      Expression expr = planNode.expressions()[i];
      ctx.code().append("%s %s = %s;\n"
              .formatted(JavaCodeGen.getTypeName(expr.type()), variableName, JavaCodeGen.translateExpression(expr, ctx)));
    }
    parent.consume(ctx, outputColumns);
  }

  @Override
  public PlanNode getPlanNode() {
    return planNode;
  }
}

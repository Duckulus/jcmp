package de.aminh.jcmp.compilation;

import de.aminh.jcmp.compilation.impl.RootTranslator;
import de.aminh.jcmp.compilation.impl.TableScanTranslator;
import de.aminh.jcmp.plan.PlanNode;

public class JavaQueryTranspiler {

  public static String translateQuery(PlanNode node) {
    RootTranslator parent = new RootTranslator();
    NodeTranslator translator = preparePlan(node, parent);
    parent.setInput(translator);

    TranslationContext ctx = new TranslationContext();
    parent.produce(ctx);

    return ctx.prelude().append(ctx.code()).toString();
  }

  public static NodeTranslator preparePlan(PlanNode node, NodeTranslator parent) {
    return switch (node) {
      case PlanNode.AggregationNode aggregationNode -> null;
      case PlanNode.LimitNode limitNode -> null;
      case PlanNode.ProjectionNode projectionNode -> null;
      case PlanNode.SelectionNode selectionNode -> null;
      case PlanNode.SingleRowNode singleRowNode -> null;
      case PlanNode.TableScanNode tableScanNode -> new TableScanTranslator(tableScanNode, parent);
    };
  }


}

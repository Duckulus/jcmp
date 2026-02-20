package de.aminh.jcmp.compilation;

import de.aminh.jcmp.compilation.impl.HashAggregationTranslator;
import de.aminh.jcmp.compilation.impl.RootTranslator;
import de.aminh.jcmp.compilation.impl.SelectionTranslator;
import de.aminh.jcmp.compilation.impl.TableScanTranslator;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.plan.PlanNode;

public class JavaQueryTranspiler {

  public static String translateQuery(Table table, PlanNode node) {
    RootTranslator parent = new RootTranslator();
    NodeTranslator translator = preparePlan(node, parent);
    parent.setInput(translator);

    TranslationContext ctx = new TranslationContext(table);
    parent.produce(ctx);

    return """
            import de.aminh.jcmp.compilation.CompiledQuery;
            import de.aminh.jcmp.data.*;
            import de.aminh.jcmp.data.Column.DoubleColumn;
            import de.aminh.jcmp.data.Column.IntColumn;
            import de.aminh.jcmp.data.Column.StringColumn;
            import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
            import it.unimi.dsi.fastutil.ints.IntArrayList;
            
            import java.util.*;
            
            public class TestCompiledQuery implements CompiledQuery{
              @Override
              public RecordBatch execute(Table table) {
            """ +
            ctx.prelude().append("\n")
                    .append(ctx.code()) +
            """
                      }
                    }
                    """;
  }

  public static NodeTranslator preparePlan(PlanNode node, NodeTranslator parent) {
    return switch (node) {
      case PlanNode.AggregationNode aggregationNode -> {
        HashAggregationTranslator aggregationTranslator = new HashAggregationTranslator(aggregationNode, parent);
        NodeTranslator child = preparePlan(aggregationNode.child(), aggregationTranslator);
        aggregationTranslator.setInput(child);
        yield aggregationTranslator;
      }
      case PlanNode.LimitNode limitNode -> null;
      case PlanNode.ProjectionNode projectionNode -> null;
      case PlanNode.SelectionNode selectionNode -> {
        SelectionTranslator selectionTranslator = new SelectionTranslator(selectionNode, parent);
        NodeTranslator child = preparePlan(selectionNode.child(), selectionTranslator);
        selectionTranslator.setInput(child);
        yield selectionTranslator;
      }
      case PlanNode.SingleRowNode singleRowNode -> null;
      case PlanNode.TableScanNode tableScanNode -> new TableScanTranslator(tableScanNode, parent);
    };
  }


}

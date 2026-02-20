package de.aminh.jcmp.compilation.impl;

import de.aminh.jcmp.compilation.JavaCodeGen;
import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;
import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.plan.PlanNode.TableScanNode;

public class TableScanTranslator implements NodeTranslator {

  private final TableScanNode planNode;
  private final NodeTranslator parent;

  public TableScanTranslator(TableScanNode planNode, NodeTranslator parent) {
    this.planNode = planNode;
    this.parent = parent;
  }

  @Override
  public void produce(TranslationContext ctx) {
    for (String columnName : planNode.columnNames()) {
      // T[] attr = table.getTColumnValues("attr");
      Attribute attribute = planNode.table().getAttribute(columnName);
      String typeName = JavaCodeGen.getTypeName(attribute.type());
      String attributeName = attribute.name();
      String capitalizedTypeName = JavaCodeGen.getCapitalizedTypeName(attribute.type());
      ctx.prelude().append(
              "%s[] %s = table.get%sColumnValues(\"%s\");%n".formatted(
                      typeName, attributeName, capitalizedTypeName, attributeName
              )
      );
    }
  }

  @Override
  public void consume(TranslationContext ctx) {

  }

}

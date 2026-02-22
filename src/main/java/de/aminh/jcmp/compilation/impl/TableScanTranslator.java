package de.aminh.jcmp.compilation.impl;

import de.aminh.jcmp.compilation.JavaCodeGen;
import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;
import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.PlanNode.TableScanNode;

import java.util.List;

public class TableScanTranslator implements NodeTranslator {

  private final TableScanNode planNode;

  private final NodeTranslator parent;

  public TableScanTranslator(TableScanNode planNode, NodeTranslator parent) {
    this.planNode = planNode;
    this.parent = parent;
  }

  @Override
  public void produce(TranslationContext ctx) {
    ctx.prelude().append(
            "int inputRowCount = table.getColumn(\"%s\").length();\n".formatted(
                    planNode.columnNames().getFirst()
            )
    );
    ctx.prelude().append("\n");
    for (String columnName : planNode.columnNames()) {
      Attribute attribute = planNode.table().getAttribute(columnName);
      String typeName = JavaCodeGen.getTypeName(attribute.type());
      String attributeName = attribute.name();
      String capitalizedTypeName = JavaCodeGen.getCapitalizedTypeName(attribute.type());
      ctx.prelude().append(
              "%s[] %s = table.get%sColumnValues(\"%s\");\n".formatted(
                      typeName, attributeName, capitalizedTypeName, attributeName
              )
      );
    }

    ctx.code().append("for (int i = 0; i < inputRowCount; i++) {\n");
    ctx.setCurrentIndexVar("i");
    List<String> columnNames = planNode.columnNames().stream()
            .map(colName -> "%s[%s]".formatted(colName, ctx.currentIndexVar()))
            .toList();
    parent.consume(ctx, columnNames);
    ctx.code().append("}\n");
  }

  @Override
  public void consume(TranslationContext ctx, List<String> inputColumns) {

  }

  @Override
  public PlanNode getPlanNode() {
    return planNode;
  }
}

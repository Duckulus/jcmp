package de.aminh.jcmp.compilation.impl;

import de.aminh.jcmp.compilation.JavaCodeGen;
import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;
import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.plan.logical.PlanNode;
import de.aminh.jcmp.plan.logical.PlanNode.TableScanNode;

import java.util.ArrayList;
import java.util.List;

public class TableScanTranslator implements NodeTranslator {

  private final TableScanNode planNode;

  private final NodeTranslator parent;

  int inputRowCountId;

  public TableScanTranslator(TableScanNode planNode, NodeTranslator parent) {
    this.planNode = planNode;
    this.parent = parent;
  }

  @Override
  public void produce(TranslationContext ctx) {
    inputRowCountId = ctx.nextId();
    ctx.prelude().append(
            "int inputRowCount_%d = table.getColumn(\"%s\").length();\n".formatted(
                    inputRowCountId, planNode.columnNames().getFirst()
            )
    );
    ctx.prelude().append("\n");
    List<String> columnArrayNames = new ArrayList<>();
    int arr_id = ctx.nextId();
    for (String columnName : planNode.columnNames()) {
      Attribute attribute = planNode.table().getAttribute(columnName);
      String typeName = JavaCodeGen.getTypeName(attribute.type());
      String columnArrayName = "%s_arr_%d".formatted(columnName, arr_id);
      columnArrayNames.add(columnArrayName);
      String attributeName = attribute.name();
      String capitalizedTypeName = JavaCodeGen.getCapitalizedTypeName(attribute.type());
      ctx.prelude().append(
              "%s[] %s = table.get%sColumnValues(\"%s\");\n".formatted(
                      typeName, columnArrayName, capitalizedTypeName, attributeName
              )
      );
    }

    String loopVar = "i_" + ctx.nextId();
    ctx.code().append("for (int %s = 0; %s < inputRowCount_%d; %s++) {\n".formatted(loopVar, loopVar, inputRowCountId, loopVar));
    for (int i = 0; i < planNode.columnNames().size(); i++) {
      String columnSymbolName = "%s[%s]".formatted(columnArrayNames.get(i), loopVar);
      String colName = planNode.outputSchema()[i].name();
      ctx.declareSymbol(colName, columnSymbolName);
    }
    parent.consume(ctx, this);
    ctx.code().append("}\n");
  }

  @Override
  public void consume(TranslationContext ctx, NodeTranslator caller) {

  }

  @Override
  public PlanNode getPlanNode() {
    return planNode;
  }
}

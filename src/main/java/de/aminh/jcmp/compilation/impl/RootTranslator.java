package de.aminh.jcmp.compilation.impl;

import com.google.common.collect.Streams;
import de.aminh.jcmp.compilation.JavaCodeGen;
import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;
import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.plan.PlanNode;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Takes care of materializing and returning the result of another Translator
 */
public class RootTranslator implements NodeTranslator {

  private NodeTranslator input;

  public void setInput(NodeTranslator input) {
    this.input = input;
  }

  @Override
  public void produce(TranslationContext ctx) {
    Attribute[] attributes = input.getPlanNode().outputSchema();
    for(int i = 0; i< attributes.length; i++) {
      String parameterizedArrayListType = JavaCodeGen.fastArrayListType(attributes[i].type(), true);
      String arrayListType = JavaCodeGen.fastArrayListType(attributes[i].type(), false);
      ctx.code().append("%s output_%d = new %s();\n".formatted(parameterizedArrayListType, i, arrayListType));
    }
    ctx.code().append("\n");

    input.produce(ctx);

    String attributesString = Arrays.stream(attributes)
            .map(attr -> "new Attribute(\"%s\", DataType.%s)".formatted(attr.name(), attr.type())).collect(Collectors.joining(",\n"));
    ctx.code().append("""
            Attribute[] attributes = new Attribute[]{
                        %s
                };
            """.formatted(attributesString));

    String columnsString = Streams.mapWithIndex(Arrays.stream(attributes),
                    (attr, i) -> {
                      assert attr != null;
                      return "new %sColumn((%s[]) output_%d.%s)".formatted(
                              JavaCodeGen.getCapitalizedTypeName(attr.type()),
                              JavaCodeGen.getTypeName(attr.type()),
                              i,
                              JavaCodeGen.fastArrayConversionFunctionCall(attr.type())
                      );
                    }).collect(Collectors.joining(",\n"));
    ctx.code().append("""
            return new RecordBatch(%s, attributes, new Column[]{
                        %s
                });
            """.formatted("output_0.size()",columnsString));

  }

  @Override
  public void consume(TranslationContext ctx, List<String> inputColumns) {
    for(int i = 0; i < inputColumns.size(); i++) {
      ctx.code().append("output_%d.add(%s);\n".formatted(i, inputColumns.get(i)));
    }
  }

  @Override
  public PlanNode getPlanNode() {
    return null;
  }
}

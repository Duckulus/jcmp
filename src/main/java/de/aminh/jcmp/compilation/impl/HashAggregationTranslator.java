package de.aminh.jcmp.compilation.impl;

import com.google.common.collect.Streams;
import de.aminh.jcmp.compilation.JavaCodeGen;
import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.plan.Aggregate;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.PlanNode.AggregationNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HashAggregationTranslator implements NodeTranslator {

  private final AggregationNode planNode;

  private NodeTranslator input;
  private final NodeTranslator parent;

  public HashAggregationTranslator(AggregationNode planNode, NodeTranslator parent) {
    this.planNode = planNode;
    this.parent = parent;
  }


  public void setInput(NodeTranslator input) {
    this.input = input;
  }

  @Override
  public void produce(TranslationContext ctx) {
    List<String> keyVars = planNode.keys().stream().map(key -> "%s %s".formatted(
            JavaCodeGen.getTypeName(ctx.table().getAttribute(key).type()), key
    )).toList();
    String attributes = String.join(";\n   ", keyVars) + ";";
    String constructorArgs = String.join(", ", keyVars);
    String constructorAssignments = planNode.keys().stream().map(key -> "this.%s = %s;".formatted(key, key)).collect(Collectors.joining("\n"));
    String equalsComparisons = planNode.keys().stream().map(key -> {
      if (ctx.table().getAttribute(key).type() == DataType.STRING) {
        return "Objects.equals(this.%s, that.%s)".formatted(key, key);
      } else {
        return "this.%s == that.%s".formatted(key, key);
      }
    }).collect(Collectors.joining(" && "));
    String hashValues = String.join(", ", planNode.keys());
    ctx.prelude().append("""
            class CompoundKey {
              %s
              public CompoundKey(%s) {
                %s
              }
              @Override
              public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                CompoundKey that = (CompoundKey) o;
                return %s;
              }
              @Override
              public int hashCode() {
                return Objects.hash(%s);
              }
            }
            """.formatted(attributes, constructorArgs, constructorAssignments, equalsComparisons, hashValues));

    String aggregateAttributes = Streams.mapWithIndex(planNode.aggregates().stream(), (Aggregate agg, long i) ->
            {
              assert agg != null;
              return "%s agg%d;".formatted(JavaCodeGen.getTypeName(agg.outputType()), i);
            }
    ).collect(Collectors.joining("\n  "));
    ctx.prelude().append("""
            class AggregationState {
              int count;
              %s
            }
            
            """.formatted(aggregateAttributes));
    ctx.prelude().append("Map<CompoundKey, AggregationState> aggregationMap = new HashMap<>();\n");
    ctx.prelude().append("\n");

    input.produce(ctx);

    ctx.code().append("Set<Map.Entry<CompoundKey, AggregationState>> entries = aggregationMap.entrySet();\n");

    List<String> outputColumns = new ArrayList<>();
    for (int i = 0; i < planNode.keys().size(); i++) {
      outputColumns.add("key" + i);
    }
    for (int i = 0; i < planNode.aggregates().size(); i++) {
      outputColumns.add("agg" + i);
    }

    ctx.code().append("for (Map.Entry<CompoundKey, AggregationState> entry : entries) {\n");
    ctx.code().append("  CompoundKey key = (CompoundKey) entry.getKey();\n");
    for (int i = 0; i < planNode.keys().size(); i++) {
      String varType = JavaCodeGen.getTypeName(ctx.table().getAttribute(planNode.keys().get(i)).type());
      String varName = "key" + i;
      String attributeName = planNode.keys().get(i);
      ctx.code().append("  %s %s = key.%s;\n".formatted(varType, varName, attributeName));
    }
    ctx.code().append("  AggregationState state = (AggregationState) entry.getValue();\n");
    for (int i = 0; i < planNode.aggregates().size(); i++) {
      String varType = JavaCodeGen.getTypeName(planNode.aggregates().get(i).outputType());
      String varName = "agg" + i;
      ctx.code().append("  %s %s = state.%s".formatted(varType, varName, varName));
      if (planNode.aggregates().get(i) instanceof Aggregate.Avg) {
        ctx.code().append(" / ((double) state.count)");
      }
      ctx.code().append(";\n");
    }
    parent.consume(ctx, outputColumns);
    ctx.code().append("}\n\n");
  }

  @Override
  public void consume(TranslationContext ctx, List<String> inputColumns) {
    String keys = planNode.keys().stream().map(key -> "%s[%s]".formatted(
            key, ctx.currentIndexVar()
    )).collect(Collectors.joining(", "));
    ctx.code().append("CompoundKey key = new CompoundKey(%s);\n".formatted(keys));
    ctx.code().append("AggregationState state = (AggregationState) aggregationMap.get(key);\n");
    ctx.code().append("if (state == null) {\n");
    ctx.code().append("  state = new AggregationState();\n");
    ctx.code().append("  aggregationMap.put(key, state);\n");
    ctx.code().append("}\n");
    ctx.code().append("state.count++;\n");
    for (int i = 0; i < planNode.aggregates().size(); i++) {
      Aggregate aggregate = planNode.aggregates().get(i);
      switch (aggregate) {
        case Aggregate.CountStar _ -> ctx.code().append("state.agg%d++;\n".formatted(i));
        case Aggregate.Sum(_), Aggregate.Avg(_) ->
                ctx.code().append("state.agg%d += %s;\n".formatted(i, JavaCodeGen.translateExpression(aggregate.expressionOrNull(), ctx)));
      }
    }
  }

  @Override
  public PlanNode getPlanNode() {
    return planNode;
  }
}

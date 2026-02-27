package de.aminh.jcmp.compilation.impl;

import com.google.common.collect.Streams;
import de.aminh.jcmp.compilation.JavaCodeGen;
import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.plan.Aggregate;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.PlanNode.AggregationNode;

import java.util.List;
import java.util.stream.Collectors;

public class HashAggregationTranslator implements NodeTranslator {

  private final AggregationNode planNode;

  private NodeTranslator input;
  private final NodeTranslator parent;

  int aggregationId;

  public HashAggregationTranslator(AggregationNode planNode, NodeTranslator parent) {
    this.planNode = planNode;
    this.parent = parent;
  }

  public void setInput(NodeTranslator input) {
    this.input = input;
  }

  private String compoundKeyClass() {
    return "CompoundKey_" + aggregationId;
  }

  private String stateClass() {
    return "AggregationState_" + aggregationId;
  }

  private String lookupKeyVar() {
    return "lookupKey_" + aggregationId;
  }

  private String mapVar() {
    return "aggregationMap_" + aggregationId;
  }

  private String lastStateVar() {
    return "lastState_" + aggregationId;
  }

  @Override
  public void produce(TranslationContext ctx) {
    aggregationId = ctx.nextId();
    List<String> keyVars = planNode.keys().stream().map(key -> "%s %s".formatted(
            JavaCodeGen.getTypeName(ctx.table().getAttribute(key).type()), key
    )).toList();
    String attributes = String.join(";\n   ", keyVars) + ";";
    String equalsComparisons;
    if (planNode.keys().isEmpty()) {
      equalsComparisons = "true";
    } else {
      equalsComparisons = planNode.keys().stream().map(key -> {
        if (ctx.table().getAttribute(key).type() == DataType.STRING) {
          return "Objects.equals(this.%s, that.%s)".formatted(key, key);
        } else {
          return "this.%s == that.%s".formatted(key, key);
        }
      }).collect(Collectors.joining(" && "));
    }
    String hashValues = String.join(", ", planNode.keys());
    ctx.prelude().append("""
            class %s {
              %s
            
              @Override
              public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                %s that = (%s) o;
                return %s;
              }
              @Override
              public int hashCode() {
                return Objects.hash(%s);
              }
            }
            %s %s = new %s();
            """.formatted(compoundKeyClass(), attributes, compoundKeyClass(), compoundKeyClass(), equalsComparisons, hashValues, compoundKeyClass(), lookupKeyVar(), compoundKeyClass()));

    String aggregateAttributes = Streams.mapWithIndex(planNode.aggregates().stream(), (Aggregate agg, long i) ->
            {
              assert agg != null;
              return "%s agg%d;".formatted(JavaCodeGen.getTypeName(agg.outputType()), i);
            }
    ).collect(Collectors.joining("\n  "));
    ctx.prelude().append("""
            class %s {
              int count;
              %s
            }
            
            """.formatted(stateClass(), aggregateAttributes));
    ctx.prelude().append("%s %s = null;\n".formatted(stateClass(), lastStateVar()));
    for (String key : planNode.keys()) {
      DataType type = ctx.table().getAttribute(key).type();
      ctx.prelude().append("%s last_%s_%d = %s;\n".formatted(JavaCodeGen.getTypeName(type), key, aggregationId, JavaCodeGen.getNullValue(type)));
    }

    ctx.prelude().append("Map<%s, %s> %s = new HashMap<>();\n".formatted(compoundKeyClass(), stateClass(), mapVar()));
    ctx.prelude().append("\n");

    input.produce(ctx);

    ctx.code().append("Set<Map.Entry<%s, %s>> %sEntries = %s.entrySet();\n".formatted(compoundKeyClass(), stateClass(), mapVar(), mapVar()));

    ctx.code().append("for (Map.Entry<%s, %s> entry : %sEntries) {\n".formatted(compoundKeyClass(), stateClass(), mapVar()));
    ctx.code().append("  %s key = (%s) entry.getKey();\n".formatted(compoundKeyClass(), compoundKeyClass()));
    for (int i = 0; i < planNode.keys().size(); i++) {
      String varType = JavaCodeGen.getTypeName(ctx.table().getAttribute(planNode.keys().get(i)).type());
      String varName = "key_%d_%d".formatted(aggregationId, i);
      String attributeName = planNode.keys().get(i);
      ctx.code().append("  %s %s = key.%s;\n".formatted(varType, varName, attributeName));

      String outputColName = planNode.outputSchema()[i].name();
      ctx.declareSymbol(outputColName, varName);
    }
    ctx.code().append("  %s state = (%s) entry.getValue();\n".formatted(stateClass(), stateClass()));
    for (int i = 0; i < planNode.aggregates().size(); i++) {
      String varType = JavaCodeGen.getTypeName(planNode.aggregates().get(i).outputType());
      String varName = "agg_%d_%d".formatted(aggregationId, i);
      String stateFieldName = "agg%d".formatted(i);
      ctx.code().append("  %s %s = state.%s".formatted(varType, varName, stateFieldName));
      if (planNode.aggregates().get(i) instanceof Aggregate.Avg) {
        ctx.code().append(" / ((double) state.count)");
      }
      ctx.code().append(";\n");

      String outputColName = planNode.outputSchema()[planNode.keys().size() + i].name();
      ctx.declareSymbol(outputColName, varName);
    }
    parent.consume(ctx);
    ctx.code().append("}\n\n");
  }

  @Override
  public void consume(TranslationContext ctx) {
    ctx.code().append("%s state;\n".formatted(stateClass()));

    String fastPath = "state = %s;".formatted(lastStateVar());

    StringBuilder slowPath = new StringBuilder();
    List<String> keyValues = planNode.keys().stream().map(ctx::resolveSymbol).toList();
    for (int i = 0; i < planNode.keys().size(); i++) {
      slowPath.append("%s.%s = %s;\n".formatted(lookupKeyVar(), planNode.keys().get(i), keyValues.get(i)));
    }
    slowPath.append("""
            state = (%s) %s.get(%s);
            if (state == null) {
              state = new %s();
              %s persistentKey = new %s();
            """.formatted(stateClass(), mapVar(), lookupKeyVar(), stateClass(), compoundKeyClass(), compoundKeyClass()));
    for (int i = 0; i < planNode.keys().size(); i++) {
      slowPath.append("persistentKey.%s = %s;\n".formatted(planNode.keys().get(i), keyValues.get(i)));
    }
    slowPath.append("""
              %s.put(persistentKey, state);
            }
            """.formatted(mapVar()));
    slowPath.append("%s = state;\n".formatted(lastStateVar()));
    for (int i = 0; i < planNode.keys().size(); i++) {
      slowPath.append("last_%s_%d = %s;\n".formatted(planNode.keys().get(i), aggregationId, keyValues.get(i)));
    }

    StringBuilder fastPathCondition = new StringBuilder("%s != null".formatted(lastStateVar()));
    for (int i = 0; i < planNode.keys().size(); i++) {
      fastPathCondition.append(" && ");
      String keyName = planNode.keys().get(i);
      switch (ctx.table().getAttribute(keyName).type()) {
        case INT, DOUBLE ->
                fastPathCondition.append("%s == last_%s_%d".formatted(keyValues.get(i), keyName, aggregationId));
        case STRING ->
                fastPathCondition.append("%s.equals(last_%s_%d)".formatted(keyValues.get(i), keyName, aggregationId));
      }
    }
    ctx.code().append("""
            if (%s) {
              %s
            } else {
              %s
            }
            """.formatted(fastPathCondition, fastPath, slowPath));

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
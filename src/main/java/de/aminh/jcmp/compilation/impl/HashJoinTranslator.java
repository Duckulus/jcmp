package de.aminh.jcmp.compilation.impl;

import com.google.common.collect.Streams;
import de.aminh.jcmp.compilation.JavaCodeGen;
import de.aminh.jcmp.compilation.NodeTranslator;
import de.aminh.jcmp.compilation.TranslationContext;
import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.plan.Expression;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.PlanNode.JoinNode;

import java.util.Arrays;
import java.util.List;

public class HashJoinTranslator implements NodeTranslator {

  private final JoinNode planNode;

  private NodeTranslator leftInput;
  private Attribute[] leftSchema;

  private NodeTranslator rightInput;

  private final NodeTranslator parent;

  private int joinId;

  public HashJoinTranslator(JoinNode planNode, NodeTranslator parent) {
    this.planNode = planNode;
    this.parent = parent;
  }

  public void setLeftInput(NodeTranslator leftInput) {
    this.leftInput = leftInput;
    this.leftSchema = leftInput.getPlanNode().outputSchema();
  }

  public void setRightInput(NodeTranslator rightInput) {
    this.rightInput = rightInput;
  }

  private String keyClass() {
    return "Key_" + joinId;
  }

  private String leftTupleClass() {
    return "LeftTuple_" + joinId;
  }

  private String mapVar() {
    return "joinMap_" + joinId;
  }

  private String rightKeyVar() {
    return "rightKey_" + joinId;
  }

  private String leftMatchesVar() {
    return "leftMatches_" + joinId;
  }


  @Override
  public void produce(TranslationContext ctx) {
    joinId = ctx.nextId();

    List<Attribute> leftKeyAttrs = Streams.mapWithIndex(planNode.leftExprs().stream(),
            (exp, i) -> {
              assert exp != null;
              return new Attribute("key_" + i, exp.type());
            }).toList();
    ctx.prelude().append(JavaCodeGen.generateDataClass(keyClass(), leftKeyAttrs));
    ctx.prelude().append(JavaCodeGen.generateDataClass(leftTupleClass(), Arrays.asList(leftSchema)));
    ctx.prelude().append("Map<%s, List<%s>> %s = new HashMap<>();\n".formatted(keyClass(), leftTupleClass(), mapVar()));
    ctx.prelude().append("%s %s = new %s();\n".formatted(keyClass(), rightKeyVar(), keyClass()));

    leftInput.produce(ctx);
    rightInput.produce(ctx);
  }

  @Override
  public void consume(TranslationContext ctx, NodeTranslator caller) {
    if (caller == leftInput) {
      ctx.code().append("%s leftKey = new %s();\n".formatted(keyClass(), keyClass()));
      for (int i = 0; i < planNode.leftExprs().size(); i++) {
        Expression exp = planNode.leftExprs().get(i);
        ctx.code().append("leftKey.%s = %s;\n".formatted("key_" + i, JavaCodeGen.translateExpression(exp, ctx)));
      }
      ctx.code().append("%s leftTuple = new %s();\n".formatted(leftTupleClass(), leftTupleClass()));
      for (Attribute attribute : leftSchema) {
        String attrName = attribute.name();
        ctx.code().append("leftTuple.%s = %s;\n".formatted(attrName, ctx.resolveSymbol(attrName)));
      }
      ctx.code().append("""
              List<%s> tuples = (List<%s>) %s.get(leftKey);
              if (tuples == null) {
                tuples = new ArrayList<>();
                %s.put(leftKey, tuples);
              }
              tuples.add(leftTuple);
              """.formatted(leftTupleClass(), leftTupleClass(), mapVar(), mapVar()));
    } else if (caller == rightInput) {
      for (int i = 0; i < planNode.rightExprs().size(); i++) {
        Expression exp = planNode.rightExprs().get(i);
        ctx.code().append("%s.%s = %s;\n".formatted(rightKeyVar(), "key_" + i, JavaCodeGen.translateExpression(exp, ctx)));
      }
      ctx.code().append("""
              List<%s> %s = (List<%s>) %s.get(%s);
              if (%s == null) {
                continue;
              }
              for(%s leftMatch_%d : %s) {
              """.formatted(leftTupleClass(), leftMatchesVar(), leftTupleClass(), mapVar(), rightKeyVar(),
              leftMatchesVar(),
              leftTupleClass(), joinId, leftMatchesVar()));
      for (Attribute leftAttribute : leftSchema) {
        ctx.declareSymbol(leftAttribute.name(), "leftMatch_%d.".formatted(joinId) + leftAttribute.name());
      }
      if (planNode.residualFilter() != null) {
        ctx.code().append("if (%s) {\n".formatted(JavaCodeGen.translateExpression(planNode.residualFilter(), ctx)));
      }
      parent.consume(ctx, this);
      if (planNode.residualFilter() != null) {
        ctx.code().append("}\n");
      }
      ctx.code().append("}");
    } else {
      throw new IllegalArgumentException("Invalid Caller: " + caller);
    }
  }

  @Override
  public PlanNode getPlanNode() {
    return planNode;
  }
}

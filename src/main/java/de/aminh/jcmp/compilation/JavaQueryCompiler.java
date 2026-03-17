package de.aminh.jcmp.compilation;

import de.aminh.jcmp.compilation.translators.*;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.plan.PlanNode;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.SimpleCompiler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public class JavaQueryCompiler {

  private static final boolean DEBUG_CODEGEN = Boolean.getBoolean("jcmp.debug.codegen");

  private static final AtomicInteger queryCounter = new AtomicInteger(0);

  public static CompiledQuery compile(Table table, PlanNode node) {
    String className = "GeneratedQuery_" + queryCounter.getAndIncrement();
    String javaCode = transpileQueryToJava(table, node, className);
    return compileJavaQuery(javaCode, className);
  }

  private static String transpileQueryToJava(Table table, PlanNode node, String className) {
    RootTranslator parent = new RootTranslator();
    NodeTranslator translator = preparePlan(node, parent);
    parent.setInput(translator);

    TranslationContext ctx = new TranslationContext(table);
    parent.produce(ctx);

    String code = """
            import de.aminh.jcmp.compilation.CompiledQuery;
            import de.aminh.jcmp.data.*;
            import de.aminh.jcmp.data.Column.DoubleColumn;
            import de.aminh.jcmp.data.Column.IntColumn;
            import de.aminh.jcmp.data.Column.StringColumn;
            import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
            import it.unimi.dsi.fastutil.ints.IntArrayList;
            
            import java.util.*;
            
            public class %s implements CompiledQuery{
              @Override
              public RecordBatch execute(Table table) {
            """.formatted(className) +
            ctx.prelude() + "\n"
            + ctx.code() +
            """
                      }
                    }
                    """;
    if (DEBUG_CODEGEN) {
      try {
        Files.writeString(Path.of("generated", className + ".java"), code);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return code;
  }

  private static CompiledQuery compileJavaQuery(String javaCode, String className) {
    SimpleCompiler compiler = new SimpleCompiler();
    try {
      compiler.cook(javaCode);
      Class<?> compiledClass = compiler.getClassLoader().loadClass(className);
      return (CompiledQuery) compiledClass.getDeclaredConstructor().newInstance();
    } catch (CompileException | ClassNotFoundException | InvocationTargetException | InstantiationException |
             IllegalAccessException | NoSuchMethodException e) {
      throw new RuntimeException("There was an error while compiling the query", e);
    }
  }

  public static NodeTranslator preparePlan(PlanNode node, NodeTranslator parent) {
    return switch (node) {
      case PlanNode.AggregationNode aggregationNode -> {
        HashAggregationTranslator aggregationTranslator = new HashAggregationTranslator(aggregationNode, parent);
        NodeTranslator child = preparePlan(aggregationNode.child(), aggregationTranslator);
        aggregationTranslator.setInput(child);
        yield aggregationTranslator;
      }
      case PlanNode.LimitNode limitNode -> {
        LimitTranslator limitTranslator = new LimitTranslator(limitNode, parent);
        NodeTranslator child = preparePlan(limitNode.child(), limitTranslator);
        limitTranslator.setInput(child);
        yield limitTranslator;
      }
      case PlanNode.ProjectionNode projectionNode -> {
        ProjectionTranslator projectionTranslator = new ProjectionTranslator(projectionNode, parent);
        NodeTranslator child = preparePlan(projectionNode.child(), projectionTranslator);
        projectionTranslator.setInput(child);
        yield projectionTranslator;
      }
      case PlanNode.SelectionNode selectionNode -> {
        SelectionTranslator selectionTranslator = new SelectionTranslator(selectionNode, parent);
        NodeTranslator child = preparePlan(selectionNode.child(), selectionTranslator);
        selectionTranslator.setInput(child);
        yield selectionTranslator;
      }
      case PlanNode.SingleRowNode singleRowNode -> new SingleRowTranslator(singleRowNode, parent);
      case PlanNode.TableScanNode tableScanNode -> new TableScanTranslator(tableScanNode, parent);
      case PlanNode.JoinNode joinNode -> {
        HashJoinTranslator hashJoinTranslator = new HashJoinTranslator(joinNode, parent);
        NodeTranslator leftChild = preparePlan(joinNode.leftChild(), hashJoinTranslator);
        NodeTranslator rightChild = preparePlan(joinNode.rightChild(), hashJoinTranslator);
        hashJoinTranslator.setLeftInput(leftChild);
        hashJoinTranslator.setRightInput(rightChild);
        yield hashJoinTranslator;
      }
    };
  }


}

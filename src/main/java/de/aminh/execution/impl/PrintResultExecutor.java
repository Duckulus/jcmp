package de.aminh.execution.impl;

import de.aminh.data.Attribute;
import de.aminh.data.RecordBatch;
import de.aminh.execution.VectorizedExecutor;
import de.aminh.plan.PlanNode;
import de.aminh.plan.Planner;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * This Executor prints out the tuples produced by its child
 * It immediately consumes all input and doesn't produce any output
 */
public class PrintResultExecutor implements VectorizedExecutor {

  public static void print(PlanNode node) {
    PrintResultExecutor printer = new PrintResultExecutor(Planner.plan(node));
    printer.init();
    printer.next();
  }

  private final VectorizedExecutor child;

  public PrintResultExecutor(VectorizedExecutor child) {
    this.child = child;
  }

  @Override
  public void init() {
   child.init();
  }

  @Override
  public RecordBatch next() {
    RecordBatch batch;
    boolean printHeader = true;
    while ((batch = child.next()) != null) {
      if (printHeader) {
        IO.println(Arrays.stream(batch.attributes()).map(Attribute::name).collect(Collectors.joining(" | ")));
        printHeader = false;
      }
      for (int i = 0; i < batch.size(); i++) {
        int finalI = i;
        IO.println(
                Arrays.stream(batch.columns()).map(col -> col.getValue(finalI).toString()).collect(Collectors.joining(" | "))
        );
      }
    }
    return null;
  }
}

package de.aminh.jcmp.execution.impl;

import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.execution.VectorizedExecutor;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.Planner;
import de.aminh.jcmp.util.TablePrinter;

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
    while ((batch = child.next()) != null) {
      IO.println(TablePrinter.format(batch, Integer.MAX_VALUE));
      IO.println("");
    }
    return null;
  }
}

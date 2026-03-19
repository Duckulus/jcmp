package de.aminh.jcmp.vectorized.executors;

import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.util.TablePrinter;
import de.aminh.jcmp.vectorized.ExecutionContext;
import de.aminh.jcmp.vectorized.VectorizedExecutor;
import de.aminh.jcmp.vectorized.VectorizedPlanner;
import de.aminh.jcmp.vectorized.plan.VectorizedPlanNode;

/**
 * This Executor prints out the tuples produced by its child
 * It immediately consumes all input and doesn't produce any output
 */
@SuppressWarnings("unused")
public class PrintResultExecutor implements VectorizedExecutor {

  public static void print(PlanNode node) {
    ExecutionContext ctx = new ExecutionContext();
    PrintResultExecutor printer = new PrintResultExecutor(ctx, VectorizedPlanner.plan(ctx, node));
    printer.init();
    printer.next();
  }

  private final ExecutionContext ctx;

  private final VectorizedExecutor child;

  public PrintResultExecutor(ExecutionContext ctx, VectorizedExecutor child) {
    this.ctx = ctx;
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
      batch.release(ctx.pool());
    }
    return null;
  }

  @Override
  public VectorizedPlanNode planNode() {
    return null;
  }

}

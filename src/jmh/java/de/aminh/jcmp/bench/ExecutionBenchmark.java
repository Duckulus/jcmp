package de.aminh.jcmp.bench;

import de.aminh.jcmp.TPCHHandwritten;
import de.aminh.jcmp.TPCHPlans;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.data.tpch.TPCHDataLoader;
import de.aminh.jcmp.execution.VectorizedExecutor;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.Planner;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class ExecutionBenchmark {

  private Table tpcTable;
  private PlanNode node;

  @Setup
  public void setup() {
    tpcTable = new TPCHDataLoader().loadData();
    node = TPCHPlans.q1(tpcTable);
  }

  @Benchmark
  public void measureQ1Vectorized(Blackhole bh) {
    VectorizedExecutor executor = Planner.plan(node);
    executor.init();
    RecordBatch batch;
    while ((batch = executor.next()) != null) {
      bh.consume(batch);
    }
  }

  @Benchmark
  public void measureQ1Handwritten(Blackhole bh) {
    bh.consume(TPCHHandwritten.q1(tpcTable));
  }
}

package de.aminh.jcmp.bench;

import de.aminh.jcmp.Main;
import de.aminh.jcmp.TPCHPlans;
import de.aminh.jcmp.compilation.CompiledQuery;
import de.aminh.jcmp.compilation.JavaQueryTranspiler;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.data.tpch.TPCHDataLoader;
import de.aminh.jcmp.execution.VectorizedExecutor;
import de.aminh.jcmp.plan.logical.PlanNode;
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

  @Param({"Q1", "Q5", "Q6"})
  private String tpchQuery;

  private Table tpcTable;
  private PlanNode node;
  private CompiledQuery preCompiledQuery;

  @Setup(Level.Trial)
  public void setup() {
    tpcTable = TPCHDataLoader.loadBinaryData(Main.INPUT_FILE);

    node = switch (tpchQuery) {
      case "Q1" -> TPCHPlans.q1(tpcTable);
      case "Q5" -> TPCHPlans.q5(tpcTable);
      case "Q6" -> TPCHPlans.q6(tpcTable);
      default -> throw new IllegalArgumentException("Unknown query: " + tpchQuery);
    };
    
    preCompiledQuery = JavaQueryTranspiler.compile(tpcTable, node);
  }

  @Benchmark
  public void measureVectorizedExec(Blackhole bh) {
    VectorizedExecutor executor = Planner.plan(node);
    executor.init();
    RecordBatch batch;
    while ((batch = executor.next()) != null) {
      bh.consume(batch);
    }
  }

  @Benchmark
  public void measureCompiledExec(Blackhole bh) {
    bh.consume(preCompiledQuery.execute(tpcTable));
  }

  @Benchmark
  public void measureCompilation(Blackhole bh) {
    bh.consume(JavaQueryTranspiler.compile(tpcTable, node));
  }

  @Benchmark
  public void measureCompiledEndToEnd(Blackhole bh) {
    CompiledQuery query = JavaQueryTranspiler.compile(tpcTable, node);
    bh.consume(query.execute(tpcTable));
  }

}

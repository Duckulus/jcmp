package de.aminh.jcmp.bench;

import de.aminh.jcmp.TPCHHandwritten;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.data.tpch.TPCHDataLoader;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
public class HandwrittenBenchmark {

  private Table tpcTable;

  @Setup(Level.Trial)
  public void setup() {
    tpcTable = TPCHDataLoader.loadBinaryData("tpch_sf1.bin");
  }

  @Benchmark
  public void measureHandwrittenExec(Blackhole bh) {
    bh.consume(TPCHHandwritten.q1(tpcTable));
  }

}

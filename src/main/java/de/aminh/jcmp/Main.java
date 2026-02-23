package de.aminh.jcmp;

import de.aminh.jcmp.compilation.CompiledQuery;
import de.aminh.jcmp.compilation.JavaQueryTranspiler;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.data.tpch.TPCHDataLoader;
import de.aminh.jcmp.execution.impl.PrintResultExecutor;

public class Main {
  static void main() {
    System.setProperty("jcmp.debug.codegen", "true");
    Table table = new TPCHDataLoader().loadData();

    long start = System.currentTimeMillis();
    PrintResultExecutor.print(
            TPCHPlans.q6(table)
    );
    IO.println("Vectorized: %dms".formatted(System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    IO.println(TPCHHandwritten.q1(table).toString());
    IO.println("Handwritten: %dms".formatted(System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    CompiledQuery queryInstance = JavaQueryTranspiler.compile(table, TPCHPlans.q6(table));
    IO.println("Compilation: %dms".formatted(System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    IO.println(queryInstance.execute(table));
    IO.println("Compiled: %dms".formatted(System.currentTimeMillis() - start));
  }

}

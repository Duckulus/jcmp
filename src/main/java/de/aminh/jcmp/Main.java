package de.aminh.jcmp;

import de.aminh.jcmp.compilation.CompiledQuery;
import de.aminh.jcmp.compilation.JavaQueryTranspiler;
import de.aminh.jcmp.data.tpch.TPCHDataLoader;
import de.aminh.jcmp.data.tpch.TPCHTable;
import de.aminh.jcmp.execution.impl.PrintResultExecutor;

public class Main {
  static void main() {
    System.setProperty("jcmp.debug.codegen", "true");
//    TPCHTable table = TPCHDataLoader.loadCsvData();
//    TPCHDataLoader.writeBinaryData(table, "tpch_sf1.bin");
    TPCHTable table = TPCHDataLoader.loadBinaryData("tpch_sf1.bin");

    long start = System.currentTimeMillis();
    PrintResultExecutor.print(
            TPCHPlans.q1(table)
    );
    IO.println("Vectorized: %dms".formatted(System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    IO.println(TPCHHandwritten.q1(table).toString());
    IO.println("Handwritten: %dms".formatted(System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    CompiledQuery queryInstance = JavaQueryTranspiler.compile(table, TPCHPlans.q1(table));
    IO.println("Compilation: %dms".formatted(System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    IO.println(queryInstance.execute(table));
    IO.println("Compiled: %dms".formatted(System.currentTimeMillis() - start));
  }

}

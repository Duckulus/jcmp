package de.aminh.jcmp;

import de.aminh.jcmp.compilation.CompiledQuery;
import de.aminh.jcmp.compilation.JavaQueryTranspiler;
import de.aminh.jcmp.data.tpch.TPCHDataLoader;
import de.aminh.jcmp.data.tpch.TPCHTable;
import de.aminh.jcmp.execution.impl.PrintResultExecutor;

import java.util.Set;

public class Main {

  public static final String INPUT_FILE = "tpch_sf1.bin";

  public static Set<String> COLUMN_WHITELIST = Set.of(
          "l_returnflag",
          "l_linestatus",
          "l_quantity",
          "l_extendedprice",
          "l_discount",
          "l_tax",
          "l_shipdate"
  );

  static void main() {
    System.setProperty("jcmp.debug.codegen", "true");
//    TPCHTable table = TPCHDataLoader.loadCsvData();
//    TPCHDataLoader.writeBinaryData(table, "tpch_sf5.bin");
    TPCHTable table = TPCHDataLoader.loadBinaryData(INPUT_FILE);

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

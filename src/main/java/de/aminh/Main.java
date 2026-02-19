package de.aminh;

import de.aminh.data.Table;
import de.aminh.data.tpch.TPCHDataLoader;
import de.aminh.execution.impl.PrintResultExecutor;

public class Main {
  static void main() {
    Table table = new TPCHDataLoader().loadData();

    long start = System.currentTimeMillis();
    PrintResultExecutor.print(
            TPCHQueries.tpch1(table)
    );
    IO.println("Executed query in %dms".formatted(System.currentTimeMillis() - start));

  }

}

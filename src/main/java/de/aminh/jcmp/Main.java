package de.aminh.jcmp;

import de.aminh.jcmp.compilation.JavaQueryTranspiler;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.data.tpch.TPCHTable;
import de.aminh.jcmp.plan.PlanNode;

import java.util.HashMap;
import java.util.List;

public class Main {
  static void main() {
    Table table = new TPCHTable(new HashMap<>());
//
//    long start = System.currentTimeMillis();
//    PrintResultExecutor.print(
//            TPCHPlans.q1(table)
//    );
//    IO.println("Vectorized: %dms".formatted(System.currentTimeMillis() - start));
//    start = System.currentTimeMillis();
//    IO.println(TPCHHandwritten.q1(table).toString());
//    IO.println("Handwritten: %dms".formatted(System.currentTimeMillis() - start));
    IO.println(JavaQueryTranspiler.translateQuery(
            new PlanNode.TableScanNode(
                    table,
                    List.of(
                            "l_returnflag", "l_linestatus", "l_quantity",
                            "l_extendedprice", "l_discount", "l_tax", "l_shipdate"
                    )
            )
    ));

  }

}

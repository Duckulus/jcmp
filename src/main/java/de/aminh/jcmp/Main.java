package de.aminh.jcmp;

import de.aminh.jcmp.compilation.CompiledQuery;
import de.aminh.jcmp.compilation.JavaQueryCompiler;
import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.data.tpch.TPCHDataLoader;
import de.aminh.jcmp.data.tpch.TPCHTable;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.vectorized.VectorizedExecutor;
import de.aminh.jcmp.vectorized.VectorizedPlanner;

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
          "l_shipdate",
          "l_orderkey",
          "l_suppkey",
          "r_regionkey",
          "r_name",
          "n_nationkey",
          "n_regionkey",
          "n_name",
          "s_suppkey",
          "s_nationkey",
          "c_custkey",
          "c_nationkey",
          "o_orderkey",
          "o_custkey",
          "o_orderdate"
  );

  static void main() {
    System.setProperty("jcmp.debug.codegen", "true");

    TPCHTable table = TPCHDataLoader.loadBinaryData(INPUT_FILE);

    PlanNode query = TPCHPlans.q6(table);

    CompiledQuery c = JavaQueryCompiler.compile(table, query);
    System.out.println(c.execute(table));
  }

}

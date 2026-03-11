package de.aminh.jcmp;

import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.plan.Aggregate;
import de.aminh.jcmp.plan.BinaryOperator;
import de.aminh.jcmp.plan.Expression;
import de.aminh.jcmp.plan.PlanNode;
import de.aminh.jcmp.plan.PlanNode.AggregationNode;
import de.aminh.jcmp.plan.PlanNode.SelectionNode;
import de.aminh.jcmp.plan.PlanNode.TableScanNode;

import java.util.List;


public class TPCHPlans {

  public static PlanNode q1(Table table) {
    PlanNode scan = new TableScanNode(
            table,
            List.of(
                    "l_returnflag", "l_linestatus", "l_quantity",
                    "l_extendedprice", "l_discount", "l_tax", "l_shipdate"
            )
    );

    PlanNode filter = new SelectionNode(
            scan,
            new Expression.Binary(
                    BinaryOperator.LE,
                    new Expression.ColumnValue("l_shipdate", DataType.STRING),
                    new Expression.LiteralString("1998-09-02")
            )
    );

    Expression colQty = new Expression.ColumnValue("l_quantity", DataType.DOUBLE);
    Expression colPrice = new Expression.ColumnValue("l_extendedprice", DataType.DOUBLE);
    Expression colDiscount = new Expression.ColumnValue("l_discount", DataType.DOUBLE);
    Expression colTax = new Expression.ColumnValue("l_tax", DataType.DOUBLE);

    Expression oneMinusDiscount = new Expression.Binary(
            BinaryOperator.MINUS,
            new Expression.LiteralDouble(1.0),
            colDiscount
    );

    Expression discountedPrice = new Expression.Binary(
            BinaryOperator.TIMES,
            colPrice,
            oneMinusDiscount
    );

    Expression onePlusTax = new Expression.Binary(
            BinaryOperator.PLUS,
            new Expression.LiteralDouble(1.0),
            colTax
    );

    Expression charge = new Expression.Binary(
            BinaryOperator.TIMES,
            discountedPrice,
            onePlusTax
    );

    return new AggregationNode(
            filter,
            List.of(
                    new Aggregate.Sum(colQty),
                    new Aggregate.Sum(colPrice),
                    new Aggregate.Sum(discountedPrice),
                    new Aggregate.Sum(charge),
                    new Aggregate.Avg(colQty),
                    new Aggregate.Avg(colPrice),
                    new Aggregate.Avg(colDiscount),
                    new Aggregate.CountStar()
            ),
            List.of(
                    "sum_qty",
                    "sum_base_price",
                    "sum_disc_price",
                    "sum_charge",
                    "avg_qty",
                    "avg_price",
                    "avg_disc",
                    "count_order"
            ),
            List.of("l_returnflag", "l_linestatus")
    );
  }

  public static PlanNode q5(Table table) {
    PlanNode regionScan = new PlanNode.SelectionNode(
            new PlanNode.TableScanNode(table, List.of("r_regionkey", "r_name")),
            new Expression.Binary(BinaryOperator.EQUALS,
                    new Expression.ColumnValue("r_name", DataType.STRING),
                    new Expression.LiteralString("ASIA")
            )
    );

    PlanNode nationScan = new PlanNode.TableScanNode(table, List.of("n_nationkey", "n_regionkey", "n_name"));

    PlanNode asianNations = new PlanNode.JoinNode(
            regionScan,
            nationScan,
            List.of(new Expression.ColumnValue("r_regionkey", DataType.INT)),
            List.of(new Expression.ColumnValue("n_regionkey", DataType.INT)),
            null
    );

    PlanNode customerScan = new PlanNode.TableScanNode(table, List.of("c_custkey", "c_nationkey"));

    PlanNode filteredCustomer = new PlanNode.JoinNode(
            asianNations,
            customerScan,
            List.of(new Expression.ColumnValue("n_nationkey", DataType.INT)),
            List.of(new Expression.ColumnValue("c_nationkey", DataType.INT)),
            null
    );

    PlanNode ordersScan = new PlanNode.SelectionNode(
            new PlanNode.TableScanNode(table, List.of("o_orderkey", "o_custkey", "o_orderdate")),
            new Expression.Binary(BinaryOperator.AND,
                    new Expression.Binary(BinaryOperator.GE,
                            new Expression.ColumnValue("o_orderdate", DataType.STRING),
                            new Expression.LiteralString("1994-01-01")
                    ),
                    new Expression.Binary(BinaryOperator.LT,
                            new Expression.ColumnValue("o_orderdate", DataType.STRING),
                            new Expression.LiteralString("1995-01-01")
                    )
            )
    );

    PlanNode join1 = new PlanNode.JoinNode(
            filteredCustomer,
            ordersScan,
            List.of(new Expression.ColumnValue("c_custkey", DataType.INT)),
            List.of(new Expression.ColumnValue("o_custkey", DataType.INT)),
            null
    );

    PlanNode lineitemScan = new PlanNode.TableScanNode(table, List.of("l_orderkey", "l_suppkey", "l_extendedprice", "l_discount"));

    PlanNode join2 = new PlanNode.JoinNode(
            join1,
            lineitemScan,
            List.of(new Expression.ColumnValue("o_orderkey", DataType.INT)),
            List.of(new Expression.ColumnValue("l_orderkey", DataType.INT)),
            null
    );

    PlanNode supplierScan = new PlanNode.TableScanNode(table, List.of("s_suppkey", "s_nationkey"));

    PlanNode join3 = new PlanNode.JoinNode(
            supplierScan,
            join2,
            List.of(
                    new Expression.ColumnValue("s_suppkey", DataType.INT),
                    new Expression.ColumnValue("s_nationkey", DataType.INT)
            ),
            List.of(
                    new Expression.ColumnValue("l_suppkey", DataType.INT),
                    new Expression.ColumnValue("c_nationkey", DataType.INT)
            ),
            null
    );

    PlanNode preAggProjection = new PlanNode.ProjectionNode(
            join3,
           List.of(
                    new Expression.ColumnValue("n_name", DataType.STRING),
                    new Expression.Binary(BinaryOperator.TIMES,
                            new Expression.ColumnValue("l_extendedprice", DataType.DOUBLE),
                            new Expression.Binary(BinaryOperator.MINUS,
                                    new Expression.LiteralDouble(1.0),
                                    new Expression.ColumnValue("l_discount", DataType.DOUBLE)
                            )
                    )
           ),
            List.of("n_name", "revenue_computed")
    );

    return new PlanNode.AggregationNode(
            preAggProjection,
            List.of(new Aggregate.Sum(new Expression.ColumnValue("revenue_computed", DataType.DOUBLE))),
            List.of("revenue"),
            List.of("n_name")
    );
  }

  public static PlanNode q6(Table table) {
    PlanNode scan = new TableScanNode(
            table,
            List.of("l_shipdate", "l_discount", "l_quantity", "l_extendedprice")
    );

    Expression dateGe = new Expression.Binary(BinaryOperator.GE, new Expression.ColumnValue("l_shipdate", DataType.STRING), new Expression.LiteralString("1994-01-01"));
    Expression dateLt = new Expression.Binary(BinaryOperator.LT, new Expression.ColumnValue("l_shipdate", DataType.STRING), new Expression.LiteralString("1995-01-01"));
    Expression discountGe = new Expression.Binary(BinaryOperator.GE, new Expression.ColumnValue("l_discount", DataType.DOUBLE), new Expression.LiteralDouble(0.05));
    Expression discountLe = new Expression.Binary(BinaryOperator.LE, new Expression.ColumnValue("l_discount", DataType.DOUBLE), new Expression.LiteralDouble(0.07));
    Expression quantityLt = new Expression.Binary(BinaryOperator.LT, new Expression.ColumnValue("l_quantity", DataType.DOUBLE), new Expression.LiteralDouble(24.0));

    Expression filterCondition = new Expression.Binary(BinaryOperator.AND,
            new Expression.Binary(BinaryOperator.AND,
                    new Expression.Binary(BinaryOperator.AND, dateGe, dateLt),
                    new Expression.Binary(BinaryOperator.AND, discountGe, discountLe)
            ),
            quantityLt
    );

    PlanNode selection = new SelectionNode(scan, filterCondition);

    Expression revenueCalculation = new Expression.Binary(
            BinaryOperator.TIMES,
            new Expression.ColumnValue("l_extendedprice", DataType.DOUBLE),
            new Expression.ColumnValue("l_discount", DataType.DOUBLE)
    );

    return new AggregationNode(
            selection,
            List.of(new Aggregate.Sum(revenueCalculation)),
            List.of("revenue"),
            List.of()
    );
  }

}
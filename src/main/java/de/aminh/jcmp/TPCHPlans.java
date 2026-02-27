package de.aminh.jcmp;

import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.plan.Aggregate;
import de.aminh.jcmp.plan.Expression;
import de.aminh.jcmp.plan.Expression.Binary;
import de.aminh.jcmp.plan.Expression.BinaryOperator;
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
            new Binary(
                    BinaryOperator.LE,
                    new Expression.ColumnValue("l_shipdate", DataType.STRING),
                    new Expression.LiteralString("1998-09-02")
            )
    );

    Expression colQty = new Expression.ColumnValue("l_quantity", DataType.DOUBLE);
    Expression colPrice = new Expression.ColumnValue("l_extendedprice", DataType.DOUBLE);
    Expression colDiscount = new Expression.ColumnValue("l_discount", DataType.DOUBLE);
    Expression colTax = new Expression.ColumnValue("l_tax", DataType.DOUBLE);

    Expression oneMinusDiscount = new Binary(
            BinaryOperator.MINUS,
            new Expression.LiteralDouble(1.0),
            colDiscount
    );

    Expression discountedPrice = new Binary(
            BinaryOperator.TIMES,
            colPrice,
            oneMinusDiscount
    );

    Expression onePlusTax = new Binary(
            BinaryOperator.PLUS,
            new Expression.LiteralDouble(1.0),
            colTax
    );

    Expression charge = new Binary(
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

  public static PlanNode q6(Table table) {
    PlanNode scan = new TableScanNode(
            table,
            List.of("l_shipdate", "l_discount", "l_quantity", "l_extendedprice")
    );

    Expression dateGe = new Binary(BinaryOperator.GE, new Expression.ColumnValue("l_shipdate", DataType.STRING), new Expression.LiteralString("1994-01-01"));
    Expression dateLt = new Binary(BinaryOperator.LT, new Expression.ColumnValue("l_shipdate", DataType.STRING), new Expression.LiteralString("1995-01-01"));
    Expression discountGe = new Binary(BinaryOperator.GE, new Expression.ColumnValue("l_discount", DataType.DOUBLE), new Expression.LiteralDouble(0.05));
    Expression discountLe = new Binary(BinaryOperator.LE, new Expression.ColumnValue("l_discount", DataType.DOUBLE), new Expression.LiteralDouble(0.07));
    Expression quantityLt = new Binary(BinaryOperator.LT, new Expression.ColumnValue("l_quantity", DataType.DOUBLE), new Expression.LiteralDouble(24.0));

    Expression filterCondition = new Binary(BinaryOperator.AND,
            new Binary(BinaryOperator.AND,
                    new Binary(BinaryOperator.AND, dateGe, dateLt),
                    new Binary(BinaryOperator.AND, discountGe, discountLe)
            ),
            quantityLt
    );

    PlanNode selection = new SelectionNode(scan, filterCondition);

    Expression revenueCalculation = new Binary(
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
package de.aminh.jcmp;

import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.plan.Aggregate;
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
                    Expression.BinaryOperator.LE,
                    new Expression.ColumnValue("l_shipdate", DataType.STRING),
                    new Expression.LiteralString("1998-09-02")
            )
    );

    Expression colQty = new Expression.ColumnValue("l_quantity", DataType.DOUBLE);
    Expression colPrice = new Expression.ColumnValue("l_extendedprice", DataType.DOUBLE);
    Expression colDiscount = new Expression.ColumnValue("l_discount", DataType.DOUBLE);
    Expression colTax = new Expression.ColumnValue("l_tax", DataType.DOUBLE);

    // expr: (1 - l_discount)
    Expression oneMinusDiscount = new Expression.Binary(
            Expression.BinaryOperator.MINUS,
            new Expression.LiteralDouble(1.0),
            colDiscount
    );

    // expr: l_extendedprice * (1 - l_discount)
    Expression discountedPrice = new Expression.Binary(
            Expression.BinaryOperator.TIMES,
            colPrice,
            oneMinusDiscount
    );

    // expr: (1 + l_tax)
    Expression onePlusTax = new Expression.Binary(
            Expression.BinaryOperator.PLUS,
            new Expression.LiteralDouble(1.0),
            colTax
    );

    // expr: l_extendedprice * (1 - l_discount) * (1 + l_tax)
    Expression charge = new Expression.Binary(
            Expression.BinaryOperator.TIMES,
            discountedPrice,
            onePlusTax
    );

    return new AggregationNode(
            filter,
            List.of(
                    new Aggregate.Sum(colQty),              // sum(l_quantity)
                    new Aggregate.Sum(colPrice),            // sum(l_extendedprice)
                    new Aggregate.Sum(discountedPrice),     // sum(l_extendedprice * (1 - l_discount))
                    new Aggregate.Sum(charge),              // sum(l_extendedprice * (1 - l_discount) * (1 + l_tax))
                    new Aggregate.Avg(colQty),              // avg(l_quantity)
                    new Aggregate.Avg(colPrice),            // avg(l_extendedprice)
                    new Aggregate.Avg(colDiscount),         // avg(l_discount)
                    new Aggregate.CountStar()               // count(*)
            ),
            List.of("l_returnflag", "l_linestatus")         // GROUP BY l_returnflag, l_linestatus
    );
  }

}

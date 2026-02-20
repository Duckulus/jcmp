package de.aminh.jcmp;

import de.aminh.jcmp.data.*;
import de.aminh.jcmp.data.Column.DoubleColumn;
import de.aminh.jcmp.data.Column.IntColumn;
import de.aminh.jcmp.data.Column.StringColumn;

import java.util.HashMap;
import java.util.Map;

public class TPCHHandwritten {

  public static RecordBatch q1(Table table) {
    String[] l_returnflag = table.getStringColumnValues("l_returnflag");
    String[] l_linestatus = table.getStringColumnValues("l_linestatus");
    double[] l_quantity = table.getDoubleColumnValues("l_quantity");
    double[] l_extendedprice = table.getDoubleColumnValues("l_extendedprice");
    double[] l_discount = table.getDoubleColumnValues("l_discount");
    double[] l_tax = table.getDoubleColumnValues("l_tax");
    String[] l_shipdate = table.getStringColumnValues("l_shipdate");

    record CompoundKey(String l_returnflag, String l_linestatus) {
    }
    class AggregationState {
      double sum_qty;
      double sum_base_price;
      double sum_disc_price;
      double sum_charge;
      double sum_l_discount;
      int count;
    }

    Map<CompoundKey, AggregationState> aggregationMap = new HashMap<>();
    for (int i = 0; i < l_returnflag.length; i++) {
      if (l_shipdate[i].compareTo("1998-09-02") <= 0) {
        CompoundKey key = new CompoundKey(l_returnflag[i], l_linestatus[i]);
        AggregationState state = aggregationMap.computeIfAbsent(key, _ -> new AggregationState());
        state.sum_qty += l_quantity[i];
        state.sum_base_price += l_extendedprice[i];
        state.sum_disc_price += l_extendedprice[i] * (1 - l_discount[i]);
        state.sum_charge += l_extendedprice[i] * (1 - l_discount[i]) * (1 + l_tax[i]);
        state.sum_l_discount += l_discount[i];
        state.count++;
      }
    }

    Attribute[] attributes = new Attribute[]{
            new Attribute("l_returnflag", DataType.STRING),
            new Attribute("l_linestatus", DataType.STRING),
            new Attribute("agg_0", DataType.DOUBLE),
            new Attribute("agg_1", DataType.DOUBLE),
            new Attribute("agg_2", DataType.DOUBLE),
            new Attribute("agg_3", DataType.DOUBLE),
            new Attribute("agg_4", DataType.DOUBLE),
            new Attribute("agg_5", DataType.DOUBLE),
            new Attribute("agg_6", DataType.DOUBLE),
            new Attribute("agg_7", DataType.INT),
    };

    var entries = aggregationMap.entrySet();
    var outputRows = entries.size();
    String[] output0 = new String[outputRows];
    String[] output1 = new String[outputRows];
    double[] output2 = new double[outputRows];
    double[] output3 = new double[outputRows];
    double[] output4 = new double[outputRows];
    double[] output5 = new double[outputRows];
    double[] output6 = new double[outputRows];
    double[] output7 = new double[outputRows];
    double[] output8 = new double[outputRows];
    int[] output9 = new int[outputRows];
    int i = 0;
    for (Map.Entry<CompoundKey, AggregationState> entry : entries) {
      CompoundKey key = entry.getKey();
      output0[i] = key.l_returnflag;
      output1[i] = key.l_linestatus;
      AggregationState state = entry.getValue();
      output2[i] = state.sum_qty;
      output3[i] = state.sum_base_price;
      output4[i] = state.sum_disc_price;
      output5[i] = state.sum_charge;
      output6[i] = state.sum_qty / state.count;
      output7[i] = state.sum_base_price / state.count;
      output8[i] = state.sum_l_discount / state.count;
      output9[i] = state.count;
      i++;
    }

    return new RecordBatch(outputRows, attributes, new Column[]{
            new StringColumn(output0),
            new StringColumn(output1),
            new DoubleColumn(output2),
            new DoubleColumn(output3),
            new DoubleColumn(output4),
            new DoubleColumn(output5),
            new DoubleColumn(output6),
            new DoubleColumn(output7),
            new DoubleColumn(output8),
            new IntColumn(output9),
    });
  }

}

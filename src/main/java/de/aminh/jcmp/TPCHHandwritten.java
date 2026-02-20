package de.aminh.jcmp;

import de.aminh.jcmp.data.*;
import de.aminh.jcmp.data.Column.DoubleColumn;
import de.aminh.jcmp.data.Column.IntColumn;
import de.aminh.jcmp.data.Column.StringColumn;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TPCHHandwritten {

  public static RecordBatch q1(Table table) {
    ArrayList<String> output0 = new ArrayList<>();
    ArrayList<String> output1 = new ArrayList<>();
    DoubleArrayList output2 = new DoubleArrayList();
    DoubleArrayList output3 = new DoubleArrayList();
    DoubleArrayList output4 = new DoubleArrayList();
    DoubleArrayList output5 = new DoubleArrayList();
    DoubleArrayList output6 = new DoubleArrayList();
    DoubleArrayList output7 = new DoubleArrayList();
    DoubleArrayList output8 = new DoubleArrayList();
    IntArrayList output9 = new IntArrayList();

    int inputRows = table.getColumn("l_returnflag").length();

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
    for (int i = 0; i < inputRows; i++) {
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



    var entries = aggregationMap.entrySet();
    var outputRows = entries.size();

    for (Map.Entry<CompoundKey, AggregationState> entry : entries) {
      CompoundKey key = entry.getKey();
      String key0 = key.l_returnflag;
      String key1 = key.l_linestatus;
      AggregationState state = entry.getValue();
      double agg0 = state.sum_qty;
      double agg1 = state.sum_base_price;
      double agg2 = state.sum_disc_price;
      double agg3 = state.sum_charge;
      double agg4 = state.sum_qty / state.count;
      double agg5 = state.sum_base_price / state.count;
      double agg6 = state.sum_l_discount / state.count;
      int agg7 = state.count;

      output0.add(key0);
      output1.add(key1);
      output2.add(agg0);
      output3.add(agg1);
      output4.add(agg2);
      output5.add(agg3);
      output6.add(agg4);
      output7.add(agg5);
      output8.add(agg6);
      output9.add(agg7);
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

    return new RecordBatch(outputRows, attributes, new Column[]{
            new StringColumn(output0.toArray(String[]::new)),
            new StringColumn(output1.toArray(String[]::new)),
            new DoubleColumn(output2.toDoubleArray()),
            new DoubleColumn(output3.toDoubleArray()),
            new DoubleColumn(output4.toDoubleArray()),
            new DoubleColumn(output5.toDoubleArray()),
            new DoubleColumn(output6.toDoubleArray()),
            new DoubleColumn(output7.toDoubleArray()),
            new DoubleColumn(output8.toDoubleArray()),
            new IntColumn(output9.toIntArray()),
    });
  }

}

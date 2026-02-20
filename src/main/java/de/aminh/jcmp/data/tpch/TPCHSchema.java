package de.aminh.jcmp.data.tpch;

import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.DataType;

import java.util.List;

public enum TPCHSchema {
  REGION("region", List.of(
          new Attribute("r_regionkey", DataType.INT),
          new Attribute("r_name", DataType.STRING),
          new Attribute("r_comment", DataType.STRING)
  )),

  NATION("nation", List.of(
          new Attribute("n_nationkey", DataType.INT),
          new Attribute("n_name", DataType.STRING),
          new Attribute("n_regionkey", DataType.INT),
          new Attribute("n_comment", DataType.STRING)
  )),

  SUPPLIER("supplier", List.of(
          new Attribute("s_suppkey", DataType.INT),
          new Attribute("s_name", DataType.STRING),
          new Attribute("s_address", DataType.STRING),
          new Attribute("s_nationkey", DataType.INT),
          new Attribute("s_phone", DataType.STRING),
          new Attribute("s_acctbal", DataType.DOUBLE),
          new Attribute("s_comment", DataType.STRING)
  )),

  CUSTOMER("customer", List.of(
          new Attribute("c_custkey", DataType.INT),
          new Attribute("c_name", DataType.STRING),
          new Attribute("c_address", DataType.STRING),
          new Attribute("c_nationkey", DataType.INT),
          new Attribute("c_phone", DataType.STRING),
          new Attribute("c_acctbal", DataType.DOUBLE),
          new Attribute("c_mktsegment", DataType.STRING),
          new Attribute("c_comment", DataType.STRING)
  )),

  PART("part", List.of(
          new Attribute("p_partkey", DataType.INT),
          new Attribute("p_name", DataType.STRING),
          new Attribute("p_mfgr", DataType.STRING),
          new Attribute("p_brand", DataType.STRING),
          new Attribute("p_type", DataType.STRING),
          new Attribute("p_size", DataType.INT),
          new Attribute("p_container", DataType.STRING),
          new Attribute("p_retailprice", DataType.DOUBLE),
          new Attribute("p_comment", DataType.STRING)
  )),

  PARTSUPP("partsupp", List.of(
          new Attribute("ps_partkey", DataType.INT),
          new Attribute("ps_suppkey", DataType.INT),
          new Attribute("ps_availqty", DataType.INT),
          new Attribute("ps_supplycost", DataType.DOUBLE),
          new Attribute("ps_comment", DataType.STRING)
  )),

  ORDERS("orders", List.of(
          new Attribute("o_orderkey", DataType.INT),
          new Attribute("o_custkey", DataType.INT),
          new Attribute("o_orderstatus", DataType.STRING),
          new Attribute("o_totalprice", DataType.DOUBLE),
          new Attribute("o_orderdate", DataType.STRING),
          new Attribute("o_orderpriority", DataType.STRING),
          new Attribute("o_clerk", DataType.STRING),
          new Attribute("o_shippriority", DataType.INT),
          new Attribute("o_comment", DataType.STRING)
  )),

  LINEITEM("lineitem", List.of(
          new Attribute("l_orderkey", DataType.INT),
          new Attribute("l_partkey", DataType.INT),
          new Attribute("l_suppkey", DataType.INT),
          new Attribute("l_linenumber", DataType.INT),
          new Attribute("l_quantity", DataType.DOUBLE),
          new Attribute("l_extendedprice", DataType.DOUBLE),
          new Attribute("l_discount", DataType.DOUBLE),
          new Attribute("l_tax", DataType.DOUBLE),
          new Attribute("l_returnflag", DataType.STRING),
          new Attribute("l_linestatus", DataType.STRING),
          new Attribute("l_shipdate", DataType.STRING),
          new Attribute("l_commitdate", DataType.STRING),
          new Attribute("l_receiptdate", DataType.STRING),
          new Attribute("l_shipinstruct", DataType.STRING),
          new Attribute("l_shipmode", DataType.STRING),
          new Attribute("l_comment", DataType.STRING)
  ));

  private final String tableName;
  private final List<Attribute> attributes;

  TPCHSchema(String tableName, List<Attribute> attributes) {
    this.tableName = tableName;
    this.attributes = attributes;
  }

  public String getTableName() {
    return tableName;
  }

  public List<Attribute> getAttributes() {
    return attributes;
  }
}

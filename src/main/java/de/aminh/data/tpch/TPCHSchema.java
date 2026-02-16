package de.aminh.data.tpch;

import de.aminh.data.Attribute;
import de.aminh.data.DataType;

import java.util.List;

public enum TPCHSchema {
  CUSTOMER("customer", List.of(
          new Attribute("c_custkey", DataType.INT),
          new Attribute("c_name", DataType.STRING),
          new Attribute("c_address", DataType.STRING),
          new Attribute("c_nationkey", DataType.INT),
          new Attribute("c_phone", DataType.STRING),
          new Attribute("c_acctbal", DataType.DOUBLE),
          new Attribute("c_mktsegment", DataType.STRING),
          new Attribute("c_comment", DataType.STRING)
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

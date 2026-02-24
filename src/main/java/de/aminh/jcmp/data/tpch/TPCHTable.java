package de.aminh.jcmp.data.tpch;

import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.Column;
import de.aminh.jcmp.data.Table;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is an in-memory representation of the TPC-H-Schema.
 * We treat it as a single big table that contains all the attributes of the schema.
 * We can do this because the attribute names are prefixed and thus unique.
 * For our purposes this is fine to do
 */
public class TPCHTable implements Table {

  private final Map<String, Column> columns;
  private transient Map<String, Attribute> attributes;

  public TPCHTable(Map<String, Column> columns) {
    this.columns = columns;
    initAttributes();
  }

  private void initAttributes() {
    this.attributes = new HashMap<>();
    for (TPCHSchema schema : TPCHSchema.values()) {
      for (Attribute attribute : schema.getAttributes()) {
        attributes.put(attribute.name(), attribute);
      }
    }
  }

  @Override
  public Column getColumn(String name) {
    return columns.get(name);
  }

  @Override
  public Attribute getAttribute(String name) {
    return attributes.get(name);
  }

}

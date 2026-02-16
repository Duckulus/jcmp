package de.aminh.data;

public interface Table {

  Column getColumn(String name);

  Attribute getAttribute(String name);

}

package de.aminh.plan;

public interface PlanNode {

  void open();

  RecordBatch next();

  void close();

}

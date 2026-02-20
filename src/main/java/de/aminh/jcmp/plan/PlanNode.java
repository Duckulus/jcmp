package de.aminh.jcmp.plan;

import com.google.common.collect.Streams;
import de.aminh.jcmp.Configuration;
import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.exceptions.TypeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public sealed interface PlanNode {

  record LimitNode(PlanNode child, int limit) implements PlanNode {

  }

  record ProjectionNode(PlanNode child, Expression[] expressions) implements PlanNode {

  }

  /**
   * This node is used in queries that don't have a FROM clause, e.g. SELECT 1;
   */
  record SingleRowNode() implements PlanNode {

  }

  record TableScanNode(Table table, List<String> columnNames, int batchSize) implements PlanNode {
    public TableScanNode(Table table, List<String> columnNames) {
      this(table, columnNames, Configuration.BATCH_SIZE);
    }
  }

  record SelectionNode(PlanNode child, Expression predicate) implements PlanNode {

  }

  // TODO aggregation should support arbitrary expressions as keys
  record AggregationNode(PlanNode child, List<Aggregate> aggregates, List<String> keys) implements PlanNode {

  }

  default Attribute[] outputSchema() {
    return switch (this) {
      case AggregationNode aggregationNode -> {
        List<Attribute> attributes = new ArrayList<>();
        for (String key : aggregationNode.keys) {
          Attribute keyAttribute = Arrays.stream(aggregationNode.child.outputSchema())
                  .filter(attr -> attr.name().equals(key)).findFirst()
                  .orElseThrow(() -> new TypeException("Aggregation key %s not found in input schema", key));
          attributes.add(keyAttribute);
        }
        for (int i = 0; i < aggregationNode.aggregates.size(); i++) {
          attributes.add(new Attribute("agg_" + i, aggregationNode.aggregates.get(i).outputType()));
        }
        yield attributes.toArray(Attribute[]::new);
      }
      case LimitNode limitNode -> limitNode.child.outputSchema();
      case ProjectionNode projectionNode ->
              Streams.mapWithIndex(Arrays.stream(projectionNode.expressions), (exp, i) -> {
                assert exp != null;
                return new Attribute("col_" + i, exp.type());
              }).toArray(Attribute[]::new);
      case SelectionNode selectionNode -> selectionNode.child.outputSchema();
      case SingleRowNode _ -> new Attribute[0];
      case TableScanNode tableScanNode ->
              tableScanNode.columnNames.stream().map(tableScanNode.table::getAttribute).toArray(Attribute[]::new);

    };
  }

}

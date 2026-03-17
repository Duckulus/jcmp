package de.aminh.jcmp.plan;

import com.google.common.collect.Streams;
import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.exceptions.TypeException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public sealed interface PlanNode {

  record LimitNode(PlanNode child, int limit) implements PlanNode {

  }

  record ProjectionNode(PlanNode child, List<Expression> expressions, List<String> columnAliases) implements PlanNode {

  }

  /**
   * This node is used in queries that don't have a FROM clause, e.g. SELECT 1;
   */
  record SingleRowNode() implements PlanNode {

  }

  record TableScanNode(Table table, List<String> columnNames) implements PlanNode {

  }

  record SelectionNode(PlanNode child, Expression predicate) implements PlanNode {

  }

  // TODO aggregation should support arbitrary expressions as keys
  record AggregationNode(PlanNode child, List<Aggregate> aggregates, List<String> aggregateColumnAliases, List<String> keys) implements PlanNode {

  }

  record JoinNode(PlanNode leftChild, PlanNode rightChild, List<Expression> leftExprs, List<Expression> rightExprs,
                  Expression residualFilter) implements PlanNode {

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
          attributes.add(new Attribute(aggregationNode.aggregateColumnAliases.get(i), aggregationNode.aggregates.get(i).outputType()));
        }
        yield attributes.toArray(Attribute[]::new);
      }
      case LimitNode limitNode -> limitNode.child.outputSchema();
      case ProjectionNode projectionNode ->
              Streams.mapWithIndex(projectionNode.expressions.stream(), (exp, i) -> {
                assert exp != null;
                return new Attribute(projectionNode.columnAliases.get((int) i), exp.type());
              }).toArray(Attribute[]::new);
      case SelectionNode selectionNode -> selectionNode.child.outputSchema();
      case SingleRowNode _ -> new Attribute[0];
      case TableScanNode tableScanNode ->
              tableScanNode.columnNames.stream().map(tableScanNode.table::getAttribute).toArray(Attribute[]::new);

      case JoinNode joinNode -> Stream.concat(
              Arrays.stream(joinNode.leftChild.outputSchema()),
              Arrays.stream(joinNode.rightChild.outputSchema())
      ).toArray(Attribute[]::new);
    };
  }

}

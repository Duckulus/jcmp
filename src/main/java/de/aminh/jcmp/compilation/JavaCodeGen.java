package de.aminh.jcmp.compilation;

import de.aminh.jcmp.data.Attribute;
import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.exceptions.TypeException;
import de.aminh.jcmp.plan.BinaryOperator;
import de.aminh.jcmp.plan.Expression;

import java.util.ArrayList;
import java.util.List;

public class JavaCodeGen {

  public static String getTypeName(DataType type) {
    return switch (type) {
      case INT -> "int";
      case DOUBLE -> "double";
      case STRING -> "String";
    };
  }

  public static String getCapitalizedTypeName(DataType type) {
    return switch (type) {
      case INT -> "Int";
      case DOUBLE -> "Double";
      case STRING -> "String";
    };
  }

  public static String getNullValue(DataType type) {
    return switch (type) {
      case INT -> "0";
      case DOUBLE -> "0d";
      case STRING -> "null";
    };
  }

  public static String translateExpression(Expression expression, TranslationContext ctx) {
    return switch (expression) {
      case Expression.Binary(BinaryOperator oper, Expression left, Expression right) -> {
        String operatorSymbol = switch (oper) {
          case PLUS -> "+";
          case MINUS -> "-";
          case TIMES -> "*";
          case DIVIDE -> "/";
          case EQUALS -> "==";
          case LT -> "<";
          case GT -> ">";
          case LE -> "<=";
          case GE -> ">=";
          case AND -> "&&";
          case OR -> "||";
        };
        if (left.type() == DataType.STRING) {
          String leftString = translateExpression(left, ctx);
          String rightString = translateExpression(right, ctx);
          yield switch (oper) {
            case EQUALS -> "%s.equals(%s)".formatted(leftString, rightString);
            case LT, GT, LE, GE -> "(%s.compareTo(%s) %s 0)".formatted(leftString, rightString, operatorSymbol);
            default -> throw new TypeException("Invalid Operator %s for String".formatted(oper));
          };
        } else {
          yield "(%s %s %s)".formatted(
                  translateExpression(left, ctx), operatorSymbol, translateExpression(right, ctx)
          );
        }

      }
      case Expression.ColumnValue(String name, _) -> ctx.resolveSymbol(name);
      case Expression.LiteralDouble(double value) -> String.valueOf(value);
      case Expression.LiteralInt(int value) -> String.valueOf(value);
      case Expression.LiteralString(String value) -> "\"" + value + "\"";
    };
  }

  public static String fastArrayListType(DataType type, boolean parameterized) {
    return switch (type) {
      case INT -> "IntArrayList";
      case DOUBLE -> "DoubleArrayList";
      case STRING -> parameterized ? "ArrayList<String>" : "ArrayList<>";
    };
  }

  public static String fastArrayConversionFunctionCall(DataType type) {
    return switch (type) {
      case INT -> "toIntArray()";
      case DOUBLE -> "toDoubleArray()";
      case STRING -> "toArray(new String[0])";
    };
  }

  public static String generateDataClass(String className, List<String> fieldNames, List<DataType> fieldTypes) {
    if (fieldNames.size() != fieldTypes.size()) {
      throw new IllegalArgumentException("fieldNames and fieldTypes must have the same size");
    }

    StringBuilder attributes = new StringBuilder();
    List<String> equalsComps = new ArrayList<>();

    for (int i = 0; i < fieldNames.size(); i++) {
      String name = fieldNames.get(i);
      DataType type = fieldTypes.get(i);

      attributes.append(JavaCodeGen.getTypeName(type))
              .append(" ")
              .append(name)
              .append(";\n  ");

      if (type == DataType.STRING) {
        equalsComps.add("Objects.equals(this.%s, that.%s)".formatted(name, name));
      } else {
        equalsComps.add("this.%s == that.%s".formatted(name, name));
      }
    }

    String equalsComparisons = equalsComps.isEmpty() ? "true" : String.join(" && ", equalsComps);
    String hashValues = String.join(", ", fieldNames);

    return """
        class %s {
          %s
          @Override
          public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            %s that = (%s) o;
            return %s;
          }
          @Override
          public int hashCode() {
            return Objects.hash(%s);
          }
        }
        """.formatted(className, attributes.toString(), className, className, equalsComparisons, hashValues);
  }

  public static String generateDataClass(String className, List<Attribute> attributes) {
    List<String> fieldNames = attributes.stream().map(Attribute::name).toList();
    List<DataType> fieldTypes = attributes.stream().map(Attribute::type).toList();
    return generateDataClass(className, fieldNames, fieldTypes);
  }

}

package de.aminh.jcmp.compilation;

import de.aminh.jcmp.data.DataType;
import de.aminh.jcmp.exceptions.TypeException;
import de.aminh.jcmp.plan.Expression;

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

  public static String translateExpression(Expression expression, TranslationContext ctx) {
    return switch (expression) {
      case Expression.Binary(Expression.BinaryOperator oper, Expression left, Expression right) -> {
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
      case Expression.ColumnValue(String name, _) -> "%s[%s]".formatted(name, ctx.currentIndexVar());
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

}

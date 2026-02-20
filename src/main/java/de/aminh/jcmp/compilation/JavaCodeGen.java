package de.aminh.jcmp.compilation;

import de.aminh.jcmp.data.DataType;

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

}

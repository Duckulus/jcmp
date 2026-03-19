package de.aminh.jcmp.vectorized;

import de.aminh.jcmp.Configuration;

public class VectorPool {

  private static final int MAX_VECTORS = 256;

  private final int[][] intVectors = new int[MAX_VECTORS][];
  private final double[][] doubleVectors = new double[MAX_VECTORS][];
  private final String[][] stringVectors = new String[MAX_VECTORS][];

  private int topInt;
  private int topDouble;
  private int topString;

  public int[] getIntVector() {
    if(topInt == 0) {
      return new int[Configuration.BATCH_SIZE];
    }
    return intVectors[--topInt];
  }

  public double[] getDoubleVector() {
    if(topDouble== 0) {
      return new double[Configuration.BATCH_SIZE];
    }
    return doubleVectors[--topDouble];
  }

  public String[] getStringVector() {
    if(topString== 0) {
      return new String[Configuration.BATCH_SIZE];
    }
    return stringVectors[--topString];
  }

  public void release(int[] vector) {
    if (topInt < MAX_VECTORS) {
      intVectors[topInt++] = vector;
    }
  }

  public void release(double[] vector) {
    if (topDouble < MAX_VECTORS) {
      doubleVectors[topDouble++] = vector;
    }
  }

  public void release(String[] vector) {
    if (topString < MAX_VECTORS) {
      stringVectors[topString++] = vector;
    }
  }

}

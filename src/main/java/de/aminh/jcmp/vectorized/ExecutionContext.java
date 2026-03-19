package de.aminh.jcmp.vectorized;

public class ExecutionContext {

  private final VectorPool pool = new VectorPool();

  public VectorPool pool() {
    return pool;
  }

}

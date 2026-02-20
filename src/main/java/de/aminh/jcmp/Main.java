package de.aminh.jcmp;

import de.aminh.jcmp.compilation.CompiledQuery;
import de.aminh.jcmp.compilation.JavaQueryTranspiler;
import de.aminh.jcmp.data.Table;
import de.aminh.jcmp.data.tpch.TPCHDataLoader;
import de.aminh.jcmp.execution.impl.PrintResultExecutor;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.janino.SimpleCompiler;

import java.lang.reflect.InvocationTargetException;

public class Main {
  static void main() throws CompileException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
    Table table = new TPCHDataLoader().loadData();

    long start = System.currentTimeMillis();
    PrintResultExecutor.print(
            TPCHPlans.q1(table)
    );
    IO.println("Vectorized: %dms".formatted(System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    IO.println(TPCHHandwritten.q1(table).toString());
    IO.println("Handwritten: %dms".formatted(System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    String javaCode = JavaQueryTranspiler.translateQuery(table,
            TPCHPlans.q1(table));
    SimpleCompiler cmp = new SimpleCompiler();
    cmp.cook(javaCode);
    Class<?> compiledClass = cmp.getClassLoader().loadClass("TestCompiledQuery");
    CompiledQuery queryInstance = (CompiledQuery) compiledClass.getDeclaredConstructor().newInstance();
    IO.println(JavaQueryTranspiler.translateQuery(table,
            TPCHPlans.q1(table)));
    IO.println("Compilation: %dms".formatted(System.currentTimeMillis() - start));

    start = System.currentTimeMillis();
    IO.println(queryInstance.execute(table));
    IO.println("Compiled: %dms".formatted(System.currentTimeMillis() - start));
  }

}

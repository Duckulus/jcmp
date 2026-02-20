package de.aminh.jcmp.compilation;

import de.aminh.jcmp.data.RecordBatch;
import de.aminh.jcmp.data.Table;

/**
 * This interface is implemented by the generated Queries
 */
public interface CompiledQuery {

  RecordBatch execute(Table table);

}

package io.harborl.drip.task.core;

import java.util.List;

/**
 * A Infinite Task Stream is used to fetch the tasks with a adaptive chunk size.
 * 
 * @author Harbor Luo
 * @since 0.0.1
 *
 */
public interface TaskStream {
  
  /** Returns next chunk of task list. */
  List<AyncTask> nextChunk();
}

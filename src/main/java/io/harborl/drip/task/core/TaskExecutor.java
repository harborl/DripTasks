package io.harborl.drip.task.core;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A Task Executor is used to submit and execute the tasks.
 * It also need to provide a graceful shutdown method.
 * 
 * @author Harbor Luo
 * @since 0.0.1
 *
 */
public interface TaskExecutor {
  
  /** Submits the task list to execute. */
  void submit(List<AyncTask> tasks);
  
  /** Submits the task list to execute and latch the complete status. */
  void submit(List<AyncTask> tasks, final CountDownLatch latch);
  
  /** Shutdown and wait it to be completed in a given time period. */
  void shudownAndAwait(long timeout, TimeUnit unit) throws InterruptedException;
  
  /** Returns the task size in processing and pending,
   *  it might be a approximate number. */
  int size();
}

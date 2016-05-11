package io.harborl.drip.task.core;

/**
 * A AyncTask is used to abstract the task which 
 * will be submit and execute asynchronously.<br/>
 * 
 * @author Harbor Luo
 * @since 0.0.1
 *
 */
public interface AyncTask {
  
  /** Executes the task's real works. */
  void exec();
  
  /** Invokes when the execution failed. */
  void onFailure(Throwable th);
  
  /** Invokes when the execution completes successfully. */
  void onSucceed();
  
  /** Returns the Task's name */
  String name();

}

package io.harborl.drip.task.core;

import io.harborl.drip.task.core.utils.Logger;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Scheduler used to schedule the task gather and task executor
 *  with a fix time period loop.
 *  <p/>
 * It also implements the {@linkplain Maintenanceable} interface, so the instance
 *  is able to return the maintenance info and provide shutdown trigger.
 * 
 * @author Harbor Luo
 * @since 0.0.1
 *
 */
public final class SimpleScheduler implements Maintenanceable {

  /** A task stream is used to fetch the tasks with adaptive chunk size. */
  private final TaskStream taskStream;
  
  /** A task executor is used to execute tasks that are submitted by scheduler. */
  private final TaskExecutor taskExecutor;
  
  /** The underlying time thread pool based scheduler. */
  private final ScheduledExecutorService implService;
  
  /** The time period to schedule. */
  private final long period;
  
  /** The time unit used to measure the time period. */
  private final TimeUnit unit;
  
  /** It is used to wait a latch to return in the await() method. */
  private final CountDownLatch latch;
  
  /** A working information used to indicate the maintenance info. */
  private volatile String workingInfo = "[Scheduler] I am idle zZZ";
  
  /** Guarded by this, which guarantees instance shutdowns only once. */
  private boolean shutdown;
  
  /** Guarded by this, which guarantees instance starts only once. */
  private boolean startup;
  
  private SimpleScheduler(
      TaskStream gather, 
      TaskExecutor executor, 
      long period, TimeUnit unit) {
    this.taskStream = gather;
    this.taskExecutor = executor;
    this.latch = new CountDownLatch(1);
    this.period = period;
    this.unit = unit;

    /* We use the thread pool based scheduler for the underlying implementation. */
    this.implService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

      @Override
      public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, "[Task Scheduler] - single producer - ");
        thread.setDaemon(false);
        return thread;
      }

    });
  }

  /** 
   * Create a new Scheduler instance and inject the specified task gather, 
   * task executer and the schedule time period. 
   */
  public static SimpleScheduler valueOf(
      TaskStream taskGather, 
      TaskExecutor executor, long period, 
      TimeUnit unit) {
    return new SimpleScheduler(taskGather, executor, period, unit);
  }

  /** Starts the single thread scheduler loop. */
  public synchronized SimpleScheduler start() {
    if (startup) return this;

    /* 
     * This method call guarantees that:
     * "If any execution of this task
     * takes longer than its period, then subsequent executions
     * may start late, but will not concurrently execute."
     */
    implService.scheduleAtFixedRate(new Runnable() {

      /** Round counts from 1. */
      final AtomicInteger rounds = new AtomicInteger(1);

      @Override
      public void run() {
        workingInfo = "[Scheduler] I am busy - gather next chunk - in round #" + rounds;
        try {
          List<AyncTask> tasks = taskStream.nextChunk();
          if (tasks != null && tasks.size() > 0) {
            CountDownLatch latch = new CountDownLatch(tasks.size());
            taskExecutor.submit(tasks, latch);
            try {
              latch.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
          /* We suppress all of exceptions just to
           * makes sure the scheduler loop be going-on, no matter what 
           * kind of error happens.
           */
        } catch (Throwable t) {
          /*
           * Reports above suppressed error to console, which is used
           * to find and track the root cause.
           */
          Logger.err.println("[SimpleScheduler loop] [ERROR] - " + t);
          t.printStackTrace();
        }
        
        workingInfo = "[Scheduler] I am idle zZZ - Round #" + rounds.getAndIncrement() + " completed!";
      }
    }, 5, period, unit);

    Logger.out.println("Scheduler starts successfully.");
    startup = true;

    return this;
  }

  /** Waits to the return of latch. */
  public SimpleScheduler await() {
    try {
      this.latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    
    return this;
  }

  @Override
  public synchronized void shutdown() {
    if (shutdown) return;

    try {
      this.implService.shutdown();
      this.implService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    try {
      this.taskExecutor.shudownAndAwait(Long.MAX_VALUE, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    Logger.out.println("Scheduler shotdown successfully.");
    
    this.latch.countDown();
    shutdown = true;
  }

  @Override
  public String info() {
    final int N = this.taskExecutor.size();
    if (N > 0) {
      return "[Task executor] I am buzy - task size: "  + N;
    } else {
      return workingInfo;
    }
  }
}

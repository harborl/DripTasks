package io.harborl.drip.task.core;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A thread pool based task executor implementation.
 * It used the {@link ThreadPoolExecutor} as the underlying executor
 * and provides a graceful shutdown method.
 * 
 * @author Harbor Luo
 * @since 0.0.1
 *
 */
public final class ThreadPoolTaskExecutor implements TaskExecutor {

  /** 
   * Thanks to bellow thread pool based underlying executor provided by Doug Lea,<br/>
   *  which simplifies much works of concurrent task execution.
   */
  private final ThreadPoolExecutor implService;
  
  /** Constructs a instance with the specified concurrent level. */
  public ThreadPoolTaskExecutor(int concurentLevel) {
    this.implService = (ThreadPoolExecutor)Executors.newFixedThreadPool(concurentLevel, new ThreadFactory() {

      @Override
      public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, "[Task Executor] - consumer - ");
        thread.setDaemon(false);
        return thread;
      }

    });
  }

  @Override
  public void shudownAndAwait(long timeout, TimeUnit unit) throws InterruptedException {
    implService.shutdown();
    implService.awaitTermination(timeout, unit);
  }

  @Override
  public void submit(List<AyncTask> tasks) {
    if (tasks == null || tasks.size() == 0) return;
    
    for (final AyncTask task : tasks) {
      implService.execute(new NamedRunnable("[Task Executor] - consumer - %s", task.name()) {

        @Override
        public void exec() {
          try {
            task.exec();
            task.onSucceed();
          } catch (Throwable th) {
            task.onFailure(th);
          }
        }

      });
    }
  }
  
  @Override
  public void submit(List<AyncTask> tasks, final CountDownLatch latch) {
    if (tasks == null || tasks.size() == 0) return;
    
    for (final AyncTask task : tasks) {
      implService.execute(new NamedRunnable("[Task Executor] - consumer - %s", task.name()) {

        @Override
        public void exec() {
          try {
            task.exec();
            task.onSucceed();
          } catch (Throwable th) {
            task.onFailure(th);
          } finally {
            latch.countDown();
          }
        }

      });
    }
  }

  @Override
  public int size() {
    return implService.getQueue().size() + implService.getActiveCount();
  }

}

package io.harborl.drip.task.core.utils;

import java.io.PrintStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A simple single thread logger implementation, 
 * which is used to separate the IO operations from computation operations 
 * and finally to enhance the overall capacities of service response.
 * 
 * @author Harbor Luo
 * @since 0.0.1
 *
 */
public enum Logger {
  out(System.out, 1024, "std-out"),
  err(System.err, 1024, "std-err"),
  ;

  private final ThreadPoolExecutor exec;
  private final PrintStream writer;
  private final String name;
  private Logger(PrintStream writer, final int capacity, final String name) {
    this.writer = writer;
    this.name = name;
    
    /* Operation Strategy:
     * - Single threaded.
     * - With a bounded queue.
     * - And Caller-Runs if overloaded.
     */
    exec = new ThreadPoolExecutor(1, 1,
        0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<Runnable>(capacity),
        new ThreadFactory() {

          @Override
          public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "[Logger " + name + "] - capacity:" + capacity);
            thread.setDaemon(false);
            return thread;
          }

      });
    exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
  }

  public void start() { }

  public void log(final String log) {
    exec.execute(new Runnable() {

      @Override
      public void run() {
        writer.println(log);
      }

    });
  }
  
  public void println(String string) {
    log(string);
  }

  public void stop() {
    this.exec.shutdown();
    try {
      // Wait for the remaining log to be print out.
      this.exec.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  public static void dismissAll() {
    for (Logger logger: Logger.values()) {
      logger.stop();
    }
  }
  
  public String getName() {
    return name;
  }

  public static void main(String args[]) {
    Logger.out.log("I am here.");
    Logger.err.log("Nop!");
    
    Logger.dismissAll();
  }


}

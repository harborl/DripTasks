package io.harborl.drip.task.core;

/**
 * A {@code Runnable} implementation.<br/>
 * It owns a name and always changes current thread name, too.
 * 
 * @author Harbor Luo
 * @since V0.0.1
 * 
 */
public abstract class NamedRunnable implements Runnable {
  private final String threadName;

  public NamedRunnable(String format, Object... args) {
    this.threadName = String.format(format, args);
  }

  @Override 
  public final void run() {
    String odderName = Thread.currentThread().getName();
    Thread.currentThread().setName(threadName);
    try {
      exec();
    } finally {
      Thread.currentThread().setName(odderName);
    }
  }

  protected abstract void exec();
}

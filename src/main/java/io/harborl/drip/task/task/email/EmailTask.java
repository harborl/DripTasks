package io.harborl.drip.task.task.email;

import io.harborl.drip.task.core.AyncTask;
import io.harborl.drip.task.core.utils.Logger;
import io.harborl.drip.task.core.utils.Util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The domain based definition of a Drip mail's task 
 * which will be submitted and execute asynchronously.
 * <br/>
 * It also provides to email sending function through
 * the underlying suitable mail service selected by msg type.
 * 
 * @author Harbor Luo
 * @version 0.0.1
 *
 */
public class EmailTask implements AyncTask {

  static final AtomicInteger idInc = new AtomicInteger();
  private final String name;
  
  private EmailTask(String task) {
    this.name = task;
  }

  public static EmailTask valueOf(String task) {
    Util.GuardsNull(task, "task is null");
    return new EmailTask(task);
  }

  @Override
  public void exec() {
    Logger.out.println("Sending - " + this.name);
  }
  
  @Override
  public void onFailure(Throwable th) {
    Logger.out.println(name + " send mail failed!");
  }

  @Override
  public void onSucceed() {
    Logger.out.println(name + " send mail successed.");
  }

  @Override
  public String name() {
    return this.name;
  }

}

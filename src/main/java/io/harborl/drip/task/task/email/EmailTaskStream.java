package io.harborl.drip.task.task.email;

import io.harborl.drip.task.core.AyncTask;
import io.harborl.drip.task.core.TaskStream;
import io.harborl.drip.task.core.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * The domain based definition of a Drip mail's task stream 
 * which is used to fetch the tasks with a adaptive chunk size.<br/>
 * 
 * @author Harbor Luo
 * @since 0.0.1
 *
 */
public class EmailTaskStream implements TaskStream {

  private EmailTaskStream() {
    Logger.out.println("$> new task stream - ");
  }

  public static TaskStream newInstance() {
    return new EmailTaskStream();
  }

  @Override
  public List<AyncTask> nextChunk() {
    List<AyncTask> ayncTasks = new ArrayList<AyncTask>();

    // Fills up the task here.
    
    return ayncTasks;
  }

}

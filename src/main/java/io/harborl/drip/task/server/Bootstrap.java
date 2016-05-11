package io.harborl.drip.task.server;

import io.harborl.drip.task.core.NetworkMaintenanceAgent;
import io.harborl.drip.task.core.SimpleScheduler;
import io.harborl.drip.task.core.ThreadPoolTaskExecutor;
import io.harborl.drip.task.core.utils.Logger;
import io.harborl.drip.task.task.email.EmailTaskStream;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


/**
 * The Java's command line main entrance of
 * <em>One single thread time scheduled producer with N concurrence levels of consumer.</em>
 * <p/>
 * 
 * <h5>Usage:</h5>
 * <pre>
 * [1]>$ java -cp $CLASSPATH com.homethy.drip.mail.task.server.Bootstrap $port $period > bootstrap.log &
 * [2]>$ echo "info" | nc localhost $port
 * [3]>$ echo "shutdown" | nc localhost $port
 * </pre>
 * 
 * <h5>Explanation of above code:</h5>
 * <ol>
 *   <li>Launch the jvm server process using 'Java' dev tool with specified entrance and args.</li>
 *   <li>Fetch the maintenance info through a TCP text based request.</li>
 *   <li>Shutdown the server <em>gracefully</em> through a TCP text based request.</li>
 * </ol>
 * 
 * <h5>Note</h5> 
 * {@code nc } dev tool is just an example, 
 * you can use any TCP connection supported tool such as {@code telnet etc.} 
 * to communicate with server instance.
 * <p/>
 * {@code kill -TERM $pid} can also invoke the graceful shutdown.
 * 
 * @author Harbor Luo
 * @since 0.0.1
 *
 */
public final class Bootstrap {
  
  public static void main(String[] args) {
    
    /* Currently, we don't support to scale out the task execution to another process or machine.
     * But many solutions can reach above goal easily, 
     * for example, using tasks sharding through task gather.
     * Or splitting the task source to mulit-partitions then one task gather per partition. */
    if (args.length < 2) {
      System.out.println("Usage: command [Port] [Period seconds] <Sharding/Shardings>");
      System.exit(-1);
    }
    
    final int PORT = Integer.valueOf(args[0]);
    final int PERIOD = Integer.valueOf(args[1]);

    /* Creates a scheduler and start it. */
    SimpleScheduler scheduler = SimpleScheduler.valueOf(
        EmailTaskStream.newInstance(),
        new ThreadPoolTaskExecutor(Runtime.getRuntime().availableProcessors() + 1),
        PERIOD,
        TimeUnit.SECONDS
      ).start();

    try {
      /* Delegates scheduler's 'maintenance' relevant actions to a NetworkMaintenanceAgent. */
      final NetworkMaintenanceAgent theAgent 
        = NetworkMaintenanceAgent.valueOf(
            PORT, 
            scheduler
          );

      /* In case of typing ^C, or a system-wide event, such as user logoff or system shutdown.
       * Except of kill -9 #pid, for which it is not helpful. */
      Runtime.getRuntime().addShutdownHook(new Thread() {
          @Override
          public void run() {
            try {
              theAgent.close();
            } catch (IOException ignored) { }
          }
      });

      /* Main thread blocks here. */
      theAgent.start();

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      scheduler.shutdown();
      Logger.dismissAll();
      System.exit(0);
    }
  }

}

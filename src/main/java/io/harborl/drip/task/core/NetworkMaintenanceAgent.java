package io.harborl.drip.task.core;

import io.harborl.drip.task.core.utils.Exceptions;
import io.harborl.drip.task.core.utils.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;

/**
 * A NetworkMaintenanceAgent used to provide the shutdown and info fetch 
 * functionality of maintenance.<br/>
 * <p/>
 * They are defined as bellow:
 * <ul>
 *   <li><strong>Shutdown:</strong> It provides graceful shutdown timing event and strategy through 
 *   the network communication.</li>
 *   <li><strong>Maintenance Info:</strong> It provides info fetch timing event and returns
 *    maintenance info through the network communication.</li>
 * </ul>
 * 
 * <strong><em>In this implementation:</em></strong> <p/>
 * When you send a text command 'shutdown', then
 * the shutdown timing event emits. And then, the underlying's graceful shutdown method will be invoked.
 * <p/>
 * When you send a text command 'info', then
 * the info fetch timing event emits. And then, the underlying's info call will be invoked.
 * <p/>
 * 
 * @author Harbor Luo
 * @since 0.0.1
 *
 */
public final class NetworkMaintenanceAgent implements Closeable {
  
  private final ServerSocket serverSocket;
  private final ExecutorService shutdownExecutor;
  private final ExecutorService networkExecutor;
  private final Maintenanceable underlying;
  
  // Guarded by Memory consistency of volatile which is written by single thread
  private volatile boolean shutdown;

  private NetworkMaintenanceAgent(int port, Maintenanceable underlying) throws IOException {
    serverSocket = new ServerSocket(port);
    /* DO NOT CHANGE shutdownExecutor, which guarantees the shutdown operation correction. */
    shutdownExecutor = Executors.newSingleThreadExecutor();

    /* For network executor, you can change the execute strategy according requests. */
    networkExecutor = Executors.newCachedThreadPool(new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {
          Thread thread = new Thread(r, "[NetworkMaintenanceAgent] - network handler - ");
          thread.setDaemon(false);
          return thread;
        }

      });
    this.underlying = underlying;
  }

  /**
   * Creates a Instance of NetworkMaintenanceAgent with relevant configurations.
   * 
   * @param port the listening port 
   * @param underlying the underlying instance need to shutdown
   * @return returns the created instance
   * @throws IOException throws when the network listening failed
   */
  public static NetworkMaintenanceAgent valueOf(int port, Maintenanceable underlying) 
      throws IOException {
    return new NetworkMaintenanceAgent(port, underlying);
  }

  /** Starts this agent. */
  public void start() {
    try {
      for (;!shutdown;) {

        /* Even shutdown flag make sure that current loop breaks immediately
         * when close operations have done, 
         * but the serverSocket.accept call might blocked there which happens-before checking the flag, 
         * So we need to handle the RejectedExecutionException for above case.
         * Another solution: assign 'DiscardPolicy' as the rejected execution handler of thread pool.
         */
        try {
          networkExecutor.execute(
              new NetHandler(serverSocket.accept())
           );
        } catch (RejectedExecutionException shutdownAlready) { }

      }
    } catch (IOException unrecoverable) {
      closeGracefully(); // In case of network disconnected, then try to close gracefully.
    }
  }

  /** 
   * Tries to close all of the elements including the underlying shutdown instance.<br/>
   * <strong><tt>Note:</tt></strong> 
   * It submit a shutdown task to a single thread, which avoid the duplicated shutdown operation.
   */
  private void closeGracefully() {
    try {
      shutdownExecutor.submit(new Runnable() {
        @Override
        public void run() {
          if (!shutdown) {
            try {
              underlying.shutdown();
            } catch (Throwable ignored) { }
            
            try {
              serverSocket.close();
            } catch (Throwable ignored) { }
            
            try {
              networkExecutor.shutdownNow();
            } catch (Throwable ignored) { }
            
            try {
              shutdownExecutor.shutdownNow();
            } catch (Throwable ignored) { }
            
            shutdown = true;
          }
        }
      }).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException exeError) {
      throw Exceptions.launderThrowable(exeError.getCause());
    } catch (RejectedExecutionException shutdownAlready) { }
  }

  /**
   * A Network handler used to handle the accepted TCP connection from client.
   */
  class NetHandler extends NamedRunnable {
    private final Socket socket;
    NetHandler(Socket socket) {
      super("[NetworkMaintenanceAgent Handler]");
      this.socket = socket; 
    }

    /** Reads a line of text from current connection stream. */
    private String readLine() throws UnsupportedEncodingException, IOException {
      BufferedReader reader = 
          new BufferedReader(new InputStreamReader(this.socket.getInputStream(), "utf-8"));
      return reader.readLine();
    }
    
    /** Writes a line of text to current connection sink. */
    private void writeLine(String text) throws UnsupportedEncodingException, IOException {
      BufferedWriter writer = 
          new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream(), "utf-8"));
      writer.write(text);
      writer.flush();
    }

    /** 
     * Implementation:<br/>
     * Invokes the graceful shutdown action when receives a 'shutdown' text command.<br/>
     * Invokes the maintenance info fetch action when receives a 'info' text command.<br/>
     * */
    public void exec() {
      try {

        String command = readLine();
        if (command.trim().equals("shutdown")) {
          Logger.out.println("#=> Hit command - " + command);
          writeLine("Shutting down ..." + "\r\n");
          closeGracefully();
        } else if (command.trim().equals("info")) {
          Logger.out.println("#=> Hit command - " + command);
          writeLine(underlying.info() + "\r\n");
        } else {
          writeLine("Ouch!\r\n");
          Logger.out.println("#=> Misse command - " + command);
        }

      } catch (Throwable ignored) {
        ignored.printStackTrace();
      } finally {
        try {
          this.socket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  @Override
  public void close() throws IOException {
    closeGracefully();
  }
}
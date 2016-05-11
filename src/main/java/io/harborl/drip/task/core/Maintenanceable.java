package io.harborl.drip.task.core;

/**
 * A {@code Maintenanceable} instance is able to provides the graceful shutdown
 *  and maintenance info fetch functionalities.
 * 
 * @author Harbor Luo
 * @since 0.0.1
 *
 */
public interface Maintenanceable {
  
  /** Invokes the graceful shutdown behave. */
  void shutdown();
  
  /** Returns the maintenance information. */
  String info();
}

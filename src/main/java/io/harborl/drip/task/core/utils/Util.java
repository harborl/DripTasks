package io.harborl.drip.task.core.utils;

import java.util.Random;

/**
 *  Some misc utility functions or instances.<br/>
 * @author Harbor Luo
 * @since 0.0.1
 *
 */
public class Util {
  private Util() { }
  
  public static final String[] EMPTY_STRING_ARRAY = new String[0];
  public static final Random RANDOM = new Random();
  
  public static void sleep(int millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
  
  /** Makes sure the isNull is not null, otherwise, throw a NullPointerException error. */
  public static void GuardsNull(Object isNull, String message) {
    if (isNull == null) throw new NullPointerException(message);
  }
  
  /** Delay random [0, n) milliseconds uniformly. */
  public static void uniformRandomDelay(int n) {
    if (n <= 0)
      throw new IllegalArgumentException("n must be positive");

    try {
      Thread.sleep(RANDOM.nextInt(n));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}

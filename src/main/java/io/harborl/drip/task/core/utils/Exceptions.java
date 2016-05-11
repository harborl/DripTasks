package io.harborl.drip.task.core.utils;

/** 
 * A utility class used to handle the exception relevant issues.
 * 
 * @author Harbor Luo
 * @since v0.0.1
 *
 */
public final class Exceptions {

  private Exceptions()  { }
  
  /** Copied from [JCIP 5.3.2] which is used to launder the throwable cause */
  public static RuntimeException launderThrowable(Throwable t) {
    if (t instanceof RuntimeException) {
      return (RuntimeException) t;
    } else if (t instanceof Error) {
      throw (Error)t;
    } else {
      return new IllegalStateException("Not unchecked", t);
    }
  }
}

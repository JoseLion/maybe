package io.github.joselion.maybe.util.function;

/**
 * The same as {@link java.lang.Runnable Runnable}, but the lambda expression
 * throws an {@code E} exception.
 * 
 * This is a functional interface whose functional method is
 * {@link #run()}
 * 
 * @param <E> the type of checked exception that the runnable throws
 * @author Jose Luis Leon
 * @since v0.1.0
 */
@FunctionalInterface
public interface ThrowingRunnable<E extends Throwable> {

  /**
   * Runs the operation, or throws an exception.
   * 
   * @throws E which extends from {@link Throwable}
   */
  void run() throws E;
}

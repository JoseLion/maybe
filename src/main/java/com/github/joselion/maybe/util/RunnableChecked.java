package com.github.joselion.maybe.util;

/**
 * The same as {@link java.lang.Runnable Runnable}, but the lambda expression
 * throws an {@code E} exception.
 * 
 * This is a functional interface whose functional method is
 * {@link #runChecked()}
 * 
 * @param <E> the type of checked exception that the runnable throws
 * @author Jose Luis Leon
 * @since v0.1.0
 */
@FunctionalInterface
public interface RunnableChecked<E extends Exception> {

  /**
   * Runs the operation, or throws an exception.
   * 
   * @throws E which extends from {@link Exception}
   */
  void runChecked() throws E;
}

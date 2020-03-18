package com.github.joselion.maybe.util;

/**
 * The same as {@link java.lang.Runnable Runnable}, but the lambda expression
 * throws an {@code E} checked exception.
 * 
 * This is a functional interface whose functional method is
 * {@link #runChecked()}
 * 
 * @param <E> the type of checked exception that the runnable throws
 * @since v0.1.0
 */
@FunctionalInterface
public interface RunnableChecked<E extends Exception> {

  /**
   * Runs the operation, or throes a checked exception.
   * 
   * @throws E which extends from {@link Exception}
   */
  public void runChecked() throws E;
}

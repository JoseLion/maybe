package com.github.joselion.maybe.util;

/**
 * ConsumerChecked
 * 
 * The same as {@link java.util.function.Consumer Consumer}, but the lambda
 * expression throws an {@code E} checked exception.
 * 
 * This is a functional interface whose functional method is
 * {@link #acceptChecked(Object)}
 * 
 * @param <T> the type of argument passed to the function
 * @param <E> the type of exception that the function throws
 * @author Jose Luis Leon
 * @since v0.3.0
 */
@FunctionalInterface
public interface ConsumerChecked<T, E extends Exception> {

  /**
   * Accepts this function with the given argument, or throws an exception.
   * 
   * @param t the consumer argument
   * @throws E which extends from {@link Exception}
   */
  void acceptChecked(T t) throws E;
}

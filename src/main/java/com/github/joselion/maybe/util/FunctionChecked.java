package com.github.joselion.maybe.util;

/**
 * The same as {@link java.util.function.Function Function}, but the lambda
 * expression throws an {@code E} checked exception.
 * 
 * This is a functional interface whose functional method is
 * {@link #applyChecked()}
 * 
 * @param <T> the type of argument passed to the function
 * @param <R> the type of results returned by the function
 * @param <E> the type of checked exception that the function throws
 * @since v0.1.0
 */
@FunctionalInterface
public interface FunctionChecked<T, R, E extends Exception> {

  /**
   * Applies this function to the given argument, or throws a checked exception.
   * 
   * @param t the function argument
   * @return the function result
   * @throws E which extends from {@link Exception}
   */
  public R applyChecked(T t) throws E;
}

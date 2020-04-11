package com.github.joselion.maybe.util;

/**
 * The same as {@link java.util.function.BiFunction BiFunciton}, but the lambda
 * expression throws an {@code E} checked exception.
 * 
 * This is a functional interface whose functional method is
 * {@link #applyChecked(Object, Object)}
 * 
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <R> the type of results returned by the function
 * @param <E> the type of checked exception that the function throws
 * @since v0.1.0
 */
@FunctionalInterface
public interface BiFunctionChecked<T, U, R, E extends Exception> {

  /**
   * Applies this function to the given arguments, or throws a checked exception.
   * 
   * @param t the function first argument
   * @param u the function second argument
   * @return the function result
   * @throws E which extends from {@link Exception}
   */
  public R applyChecked(T t, U u) throws E;
}

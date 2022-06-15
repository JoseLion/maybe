package io.github.joselion.maybe.util.function;

/**
 * The same as {@link java.util.function.Function Function}, but the lambda
 * expression throws an {@code E} checked exception.
 * 
 * This is a functional interface whose functional method is
 * {@link #apply(Object)}
 * 
 * @param <T> the type of argument passed to the function
 * @param <R> the type of results returned by the function
 * @param <E> the type of exception that the function throws
 * @author Jose Luis Leon
 * @since v0.1.0
 */
@FunctionalInterface
public interface ThrowingFunction<T, R, E extends Exception> {

  /**
   * Applies this function to the given argument, or throws an exception.
   * 
   * @param t the function argument
   * @return the function result
   * @throws E which extends from {@link Exception}
   */
  R apply(T t) throws E;

  /**
   * Returns a function that always returns its input argument.
   *
   * @param <T> the type of the input and output objects to the function
   * @param <E> the type of exception that the function throws
   * @return a function that always returns its input argument
   */
  static <T, E extends Exception> ThrowingFunction<T, T, E> identity() {
      return t -> t;
  }
}

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
 * @param <E> the type of checked exception that the function throws
 * @since v0.3.0
 */
@FunctionalInterface
public interface ConsumerChecked<T, E extends Exception> {
  
  public void acceptChecked(T t) throws E;
}
package com.github.joselion.maybe.util;

/**
 * BiConsumerChecked
 * 
 * The same as {@link java.util.function.BiConsumer BiConsumer}, but the lambda
 * expression throws an {@code E} checked exception.
 * 
 * This is a functional interface whose functional method is
 * {@link #acceptChecked(Object, Object)}
 * 
 * @param <T> the first type of argument passed to the function
 * @param <U> the second type of argument passed to the function
 * @param <E> the type of checked exception that the function throws
 * @since v0.1.0
 */
@FunctionalInterface
public interface BiConsumerChecked<T, U, E extends Exception> {

  public void acceptChecked(T t, U u) throws E;
}
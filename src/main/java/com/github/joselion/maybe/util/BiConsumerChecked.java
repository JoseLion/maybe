package com.github.joselion.maybe.util;

/**
 * BiConsumerChecked
 */
@FunctionalInterface
public interface BiConsumerChecked<T, U, E extends Exception> {

  public void acceptChecked(T t, U u) throws E;
}
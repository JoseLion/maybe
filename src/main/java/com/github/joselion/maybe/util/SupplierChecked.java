package com.github.joselion.maybe.util;

/**
 * The same as {@link java.util.function.Supplier Supplier}, but the lambda
 * expression throws an {@code E} checked exception.
 * 
 * This is a functional interface whose functional method is
 * {@link #getChecked()}
 * 
 * @param <T> the type of results supplied by the supplier
 * @param <E> the type of checked exception that the supplier throws
 * @since v0.1.0
 */
@FunctionalInterface
public interface SupplierChecked<T, E extends Exception> {

  /**
   * Gets a result or throws a checked exception.
   * 
   * @return a result
   * @throws E which extends from {@link Exception}
   */
  public T getChecked() throws E;
}

package com.github.joselion.maybe.util;

/**
 * The same as {@link java.util.function.Supplier Supplier}, but the lambda
 * expression throws an {@code E} exception.
 * 
 * This is a functional interface whose functional method is
 * {@link #getChecked()}
 * 
 * @param <T> the type of results supplied by the supplier
 * @param <E> the type of checked exception that the supplier throws
 * @since v0.1.0
 */
@FunctionalInterface
public interface SupplierChecked<T, E extends Throwable> {

  /**
   * Gets a result or throws an exception.
   * 
   * @return a result
   * @throws E which extends from {@link Throwable}
   */
  public T getChecked() throws E;
}

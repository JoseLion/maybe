package com.github.joselion.maybe;

import java.util.Optional;
import java.util.function.Function;

/**
 * ResolveHandler is an API to handle the posible error of a {@link Maybe}'s
 * resolve operation. It can return back to maybe to continue linking operations,
 * or use termimal methods to return a safe value.
 * 
 * @param <T> the type of the value passed through the {@code Maybe}
 * @param <E> the type of exception that the resolve operation may throw
 * 
 * @since v0.3.2
 */
class ResolveHandler<T, E extends Exception> {

  private final Optional<T> success;
  
  private final Optional<E> error;

  private ResolveHandler(final T success, final E error) {
    this.success = Optional.ofNullable(success);
    this.error = Optional.ofNullable(error);
  }

  protected static <T, E extends Exception> ResolveHandler<T, E> withSuccess(final T success) {
    return new ResolveHandler<>(success, null);
  }

  protected static <T, E extends Exception> ResolveHandler<T, E> withError(final E error) {
    return new ResolveHandler<>(null, error);
  }

  protected static <T, E extends Exception> ResolveHandler<T, E> withNothing() {
    return new ResolveHandler<>(null, null);
  }

  /**
   * Returns a new value in case an error exits. The error is passed in the argunment
   * of to the {@code handler} function.
   * 
   * @param handler a function that should return a new value in case of error
   * @return a new handler with the new value if error is present. The same
   *         handler instance otherwise
   */
  public ResolveHandler<T, E> onError(final Function<E, T> handler) {
    if (error.isPresent()) {
      return ResolveHandler.withSuccess(handler.apply(error.get()));
    }
    
    return this;
  }

  /**
   * Allows the ResolveHandler API to go back to the Maybe API. This is useful
   * to continue chaining more more Maybe operations.
   * 
   * @return a Maybe with the success value if present. A Maybe with nothing otherwise
   */
  public Maybe<T> and() {
    if (success.isPresent()) {
      return Maybe.just(success.get());
    }

    return Maybe.nothing();
  }
}
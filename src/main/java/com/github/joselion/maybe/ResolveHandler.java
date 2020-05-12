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
   * If an error exits, handle the error and return a new value. The error is
   * passed in the argunment of to the {@code handler} function.
   * 
   * @param handler a function that should return a new value in case of error
   * @return a new handler with the new value if error is present. The same
   *         handler instance otherwise
   */
  public ResolveHandler<T, E> onError(final Function<E, T> handler) {
    if (error.isPresent()) {
      return withSuccess(handler.apply(error.get()));
    }
    
    return this;
  }

  /**
   * Catch an error if it's instance of the {@code errorType} passed, then handle
   * the error and return a new value. The catched error is passed in the argument
   * of the {@code handler} function.
   * 
   * @param <X> the type of the error to catch
   * @param errorType a class instance of the error type to catch
   * @param handler a function that should return a new value in case of error
   * @return a new handler with the new value if the error is catched. The same
   *         handler instance otherwise
   */
  public <X extends E> ResolveHandler<T, E> catchError(final Class<X> errorType, final Function<X, T> handler) {
    if (error.isPresent() && errorType.isAssignableFrom(error.get().getClass())) {
      final X exception = errorType.cast(error.get());
      return withSuccess(handler.apply(exception));
    }

    return this;
  }

  /**
   * Allows the ResolveHandler API to go back to the Maybe API. This is useful
   * to continue chaining more Maybe operations.
   * 
   * @return a Maybe with the success value if present. A Maybe with nothing otherwise
   */
  public Maybe<T> and() {
    if (success.isPresent()) {
      return Maybe.just(success.get());
    }

    return Maybe.nothing();
  }

  /**
   * Returns the value resolved/handled if present. A default value otherwise.
   * 
   * @param defaultValue the value to return if {@code resolve} failed and/or
   *                     the error was not handled.
   * @return the resolved/handled value if present. A default value otherwise
   */
  public T orDefault(final T defaultValue) {
    return success.orElse(defaultValue);
  }

  /**
   * Returns the value resolved/handled if present. Throws the error otherwise.
   * 
   * @return the resolved/handled value if present
   * @throws E the error thrown by the {@code resolve} operation
   */
  public T orThrow() throws E {
    return success.orElseThrow(error::orElseThrow);
  }

  /**
   * Returns the value resolved/handled if present. Throws another error otherwise.
   * 
   * @param <X> the new error type
   * @param errorMapper a function that maps the new exception to throw
   * @return the resolved/handled value if present
   * @throws X a mapped exception
   */
  public <X extends Throwable> T orThrow(final Function<E, X> errorMapper) throws X {
    if (success.isPresent()) {
      return success.get();
    }

    throw errorMapper.apply(error.orElseThrow());
  }
}

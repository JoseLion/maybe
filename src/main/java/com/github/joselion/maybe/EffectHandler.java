package com.github.joselion.maybe;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.joselion.maybe.util.Helpers;

/**
 * EffectHandler is an API to handle the posible error of a {@link Maybe}'s
 * effect operation. It can return back to maybe to continue linking operations,
 * or use termimal methods to return a safe value.
 * 
 * @param <E> the type of exception that the resolve operation may throw
 * 
 * @since v0.3.2
 */
public final class EffectHandler<E extends Exception> {

  private final Optional<E> error;

  private EffectHandler(final E error) {
    this.error = Optional.ofNullable(error);
  }

  /**
   * Internal use method to instanciate a EffectHandler with an error value
   * 
   * @param <E> the type of the possible exception
   * @param error the error to instanciate the EffectHandler
   * @return a EffectHandler instance with an error value
   */
  protected static <E extends Exception> EffectHandler<E> withError(final E error) {
    return new EffectHandler<>(error);
  }

  /**
   * Internal use method to instanciate a EffectHandler neither with a success
   * nor with an error value
   * 
   * @param <E> the type of the possible exception
   * @return a EffectHandler with neither the success nor the error value
   */
  protected static <E extends Exception> EffectHandler<E> withNothing() {
    return new EffectHandler<>(null);
  }

  /**
   * Handle an error if present or if was not already handled. The error is passed
   * in the argument of the {@code handler} function.
   * 
   * @param handler a function to handle the error if exists
   * @return a new handler with nothing if the error is handled. The same handler
   *         instance otherwise
   */
  public EffectHandler<E> onError(final Consumer<E> handler) {
    if (error.isPresent()) {
      handler.accept(error.get());
      return withNothing();
    }

    return this;
  }

  /**
   * Catch an error if it's instance of the {@code errorType} passed and it was
   * not already handled. The catched error is passed in the argument of the
   * {@code handler} function.
   * 
   * @param <X>       the type of the error to catch
   * @param errorType a class instance of the error type to catch
   * @param handler   a function to handle the error if exists
   * @return a new handler with nothing if the error is catched. The same handler
   *         instance otherwise
   */
  public <X extends E> EffectHandler<E> catchError(final Class<X> errorType, final Consumer<X> handler) {
    if (error.isPresent() && errorType.isAssignableFrom(error.get().getClass())) {
      final X exception = errorType.cast(error.get());

      handler.accept(exception);
      return withNothing();
    }

    return this;
  }

  /**
   * Allows the EffectHandler API to go back to the Maybe API. This is useful to
   * continue chaining more Maybe operations.
   * 
   * @return a Maybe with the error if present. A Maybe with nothing otherwise
   */
  public Maybe<Void> and() {
    if (error.isEmpty()) {
      final Void shallowVoid = Helpers.shallowInstance(Void.class);
      return Maybe.just(shallowVoid);
    }

    return Maybe.nothing();
  }

  /**
   * Throws the error if present. Does nothing otherwise.
   * 
   * @throws E the error thrown by the {@code runEffect} operation
   */
  public void onErrorThrow() throws E {
    if (error.isPresent()) {
      throw error.get();
    }
  }

  /**
   * If an error is present, map the error to another exception and throw it. Does
   * nothing otherwise.
   * 
   * @param <X>         the new error type
   * @param errorMapper a function that maps the new exception to throw
   * @throws X a mapped exception
   */
  public <X extends Throwable> void onErrorThrow(final Function<E, X> errorMapper) throws X {
    if (error.isPresent()) {
      throw errorMapper.apply(error.get());
    }
  }
}

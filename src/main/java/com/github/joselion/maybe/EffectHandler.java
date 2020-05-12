package com.github.joselion.maybe;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class EffectHandler<E extends Exception> {

  private final Optional<E> error;

  private EffectHandler(final E error) {
    this.error = Optional.ofNullable(error);
  }

  protected static <E extends Exception> EffectHandler<E> withError(final E error) {
    return new EffectHandler<>(error);
  }

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
  public Maybe<None> and() {
    if (error.isEmpty()) {
      return Maybe.just(new None());
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

  protected static class None {
    
    public None() {
      // Placeholder instance of no value
    }
  }
}
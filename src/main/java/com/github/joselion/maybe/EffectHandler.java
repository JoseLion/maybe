package com.github.joselion.maybe;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;

/**
 * EffectHandler is an API to handle the posible error of a {@link Maybe}'s
 * effect operation. It can return back to maybe to continue linking operations,
 * or use termimal methods to return a safe value.
 * 
 * @param <E> the type of exception that the effect may throw
 * 
 * @author Jose Luis Leon
 * @since v0.3.2
 */
public final class EffectHandler<E extends Exception> {

  private final Optional<E> error;

  private EffectHandler(final @Nullable E error) {
    this.error = Optional.ofNullable(error);
  }

  /**
   * Internal use method to instanciate a EffectHandler with an error value
   * 
   * @param <E> the type of the possible exception
   * @param error the error to instanciate the EffectHandler
   * @return a EffectHandler instance with an error value
   */
  static <E extends Exception> EffectHandler<E> withError(final E error) {
    return new EffectHandler<>(error);
  }

  /**
   * Internal use method to instanciate a EffectHandler neither with a success
   * nor with an error value
   * 
   * @param <E> the type of the possible exception
   * @return a EffectHandler with neither the success nor the error value
   */
  static <E extends Exception> EffectHandler<E> withNothing() {
    return new EffectHandler<>(null);
  }

  /**
   * Internal use only.
   *
   * @return the possible thrown exception
   */
  Optional<E> error() {
    return error;
  }

  /**
   * Run an effect if the error is present and is an instance of the provided
   * type. The error is passed in the argument of to the {@code effect}
   * consumer.
   *
   * @param <X> the type of the error to match
   * @param ofType a class instance of the error type to match
   * @param effect a consumer function with the error passed in the argument
   * @return the same handler to continue chainning operations
   */
  public <X extends Exception> EffectHandler<E> doOnError(final Class<X> ofType, final Consumer<X> effect) {
    error.filter(ofType::isInstance)
      .map(ofType::cast)
      .ifPresent(effect);

    return this;
  }

  /**
   * Run an effect if the error is present and is an instance of the provided
   * type.
   *
   * @param <X> the type of the error to match
   * @param ofType a class instance of the error type to match
   * @param effect a runnable function
   * @return the same handler to continue chainning operations
   */
  public <X extends Exception> EffectHandler<E> doOnError(final Class<X> ofType, final Runnable effect) {
    return this.doOnError(ofType, caught -> effect.run());
  }

  /**
   * Run an effect if the error is present. The error is passed in the argument
   * of the {@code effect} consumer.
   * 
   * @param effect a consumer function with the error passed in the argument
   * @return the same handler to continue chainning operations
   */
  public EffectHandler<E> doOnError(final Consumer<E> effect) {
    error.ifPresent(effect);

    return this;
  }

  /**
   * Run an effect if an error is present.
   *
   * @param effect a runnable function
   * @return the same handler to continue chainning operations
   */
  public EffectHandler<E> doOnError(final Runnable effect) {
    return this.doOnError(caught -> effect.run());
  }

  /**
   * Catch the error if is present and is an instance of the provided type.
   * Assuming the error was handled returns a handler with {@code nothing}. The
   * caught error is passed in the argument of the {@code handler} consumer.
   * 
   * @param <X> the type of the error to catch
   * @param ofType a class instance of the error type to catch
   * @param handler a consumer function that recieves the caught error
   * @return a handler with nothing if an error instance of the provided type
   *         was caught. The same handler instance otherwise
   */
  public <X extends E> EffectHandler<E> catchError(final Class<X> ofType, final Consumer<X> handler) {
    return error.filter(ofType::isInstance)
      .map(ofType::cast)
      .map(caught -> {
        handler.accept(caught);
        return EffectHandler.<E>withNothing();
      })
      .orElse(this);
  }

  /**
   * Catch the error if is present and is an instance of the provided type.
   * Assuming the error was handled returns a handler with {@code nothing}.
   * 
   * @param <X> the type of the error to catch
   * @param ofType a class instance of the error type to catch
   * @param handler a runnable function
   * @return a handler with nothing if an error instance of the provided type
   *         was caught. The same handler instance otherwise
   */
  public <X extends E> EffectHandler<E> catchError(final Class<X> ofType, final Runnable handler) {
    return this.catchError(ofType, caught -> handler.run());
  }

  /**
   * Catch the error if is present. Assuming the error was handled returns a
   * handler with {@code nothing}. The caught error is passed in the argument
   * of the {@code handler} consumer.
   *
   * @param handler a consumer function that recieves the caught error
   * @return a handler with nothing if an error instance of the provided type
   *         was caught. The same handler instance otherwise
   */
  public EffectHandler<E> catchError(final Consumer<E> handler) {
    return error.map(caught -> {
      handler.accept(caught);
      return EffectHandler.<E>withNothing();
    })
    .orElse(this);
  }

  /**
   * Catch the error if is present. Assuming the error was handled returns a
   * handler with {@code nothing}.
   *
   * @param handler a runnable function
   * @return a handler with nothing if an error instance of the provided type
   *         was caught. The same handler instance otherwise
   */
  public EffectHandler<E> catchError(final Runnable handler) {
    return this.catchError(caught -> handler.run());
  }

  /**
   * Terminal operation to handle the error if present. The error is passed in
   * the argument of the {@code effect} consumer.
   *
   * @param effect a consumer function that receives the caught error
   */
  public void orElse(final Consumer<E> effect) {
    error.ifPresent(effect);
  }

  /**
   * Terminal operation to handle the error if present.
   *
   * @param effect a runnable function
   */
  public void orElse(final Runnable effect) {
    this.orElse(caught -> effect.run());
  }

  /**
   * Throws the error if present. Does nothing otherwise.
   * 
   * @throws E the error thrown by the {@code effect} operation
   */
  public void orThrow() throws E {
    if (error.isPresent()) {
      throw error.get();
    }
  }

  /**
   * If an error is present, map the error to another exception and throw it. Does
   * nothing otherwise.
   * 
   * @param <X> the new error type
   * @param mapper a function that maps the new exception to throw
   * @throws X a mapped exception
   */
  public <X extends Throwable> void orThrow(final Function<E, X> mapper) throws X {
    if (error.isPresent()) {
      throw mapper.apply(error.get());
    }
  }

  /**
   * Transforms the handler to a {@link Maybe}. Since there's nothing to
   * resolve, the {@link Maybe} will always have {@code nothing}.
   * 
   * @return a Maybe with {@code nothing}
   */
  public Maybe<Void> toMaybe() {
    return Maybe.nothing();
  }
}

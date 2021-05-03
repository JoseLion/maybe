package com.github.joselion.maybe;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;

/**
 * ResolveHandler is an API to handle the posible error of a {@link Maybe}'s
 * resolve operation. It can return back to maybe to continue linking operations,
 * or use termimal methods to return a safe value.
 * 
 * @param <T> the type of the value passed through the {@code Maybe}
 * @param <E> the type of exception that the resolve operation may throw
 * 
 * @author Jose Luis Leon
 * @since v0.3.2
 */
public final class ResolveHandler<T, E extends Exception> {

  private final Optional<T> success;

  private final Optional<E> error;

  private ResolveHandler(final @Nullable T success, final @Nullable E error) {
    this.success = Optional.ofNullable(success);
    this.error = Optional.ofNullable(error);
  }

  /**
   * Internal use method to instanciate a ResolveHandler with a success value
   * 
   * @param <T> the type of the success value
   * @param <E> the type of the possible exception
   * @param success the success value to instantiate the ResolveHandler
   * @return a ResolveHandler instance with a success value
   */
  protected static <T, E extends Exception> ResolveHandler<T, E> withSuccess(final T success) {
    return new ResolveHandler<>(success, null);
  }

  /**
   * Internal use method to instanciate a ResolveHandler with an error value
   * 
   * @param <T> the type of the success value
   * @param <E> the type of the possible exception
   * @param error the error to instanciate the ResolveHandler
   * @return a ResolveHandler instance with an error value
   */
  protected static <T, E extends Exception> ResolveHandler<T, E> withError(final E error) {
    return new ResolveHandler<>(null, error);
  }

  /**
   * Internal use method to instanciate a ResolveHandler neither with a success
   * nor with an error value
   * 
   * @param <T> the type of the success value
   * @param <E> the type of the possible exception
   * @return a ResolveHandler with neither the success nor the error value
   */
  protected static <T, E extends Exception> ResolveHandler<T, E> withNothing() {
    return new ResolveHandler<>(null, null);
  }

  /**
   * Run an effect if an error is present. The error is passed in the argunment
   * of to the {@code effect} consumer.
   * 
   * @param effect a consumer with the error passed in the argument
   * @return The same handler to continue chainning operations
   */
  public ResolveHandler<T, E> doOnError(final Consumer<? super Throwable> effect) {
    if (error.isPresent()) {
      effect.accept(error.get());
    }

    return this;
  }

  /**
   * If an error is present, handle the error and return a new value. The error
   * is passed in the argunment of to the {@code handler} function.
   * 
   * @param handler a function that should return a new value in case of error
   * @return a new handler with the new value if error is present. The same
   *         handler instance otherwise
   */
  public ResolveHandler<T, E> onError(final Function<? super Throwable, T> handler) {
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
   * If the value is present, map it to another value through the {@code mapper}
   * function. If the error is present, the {@code mapper} is never applied and,
   * the next handler will still contain the error.
   * <p>
   * If neither the value nor the error is present, it returns an empty handler.
   * 
   * @param <U> the type the value will be mapped to
   * @param mapper a function to map the current value to another (if present)
   * @return a new handler with the mapped value, the previous error, or nothing
   */
  public <U> ResolveHandler<U, E> map(final Function<T, U> mapper) {
    if (success.isPresent()) {
      return withSuccess(mapper.apply(success.get()));
    }

    if (error.isPresent()) {
      return withError(error.get());
    }

    return withNothing();
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

  /**
   * Transforms the handler to a {@link Maybe}. If the value was resolved, the
   * {@link Maybe} will contain it. It will be have {@code nothing} otherwise.
   * 
   * @return the resolved value wrapped in a {@link Maybe} if present. A
   *         {@link Maybe} with nothing otherwise
   */
  public Maybe<T> toMaybe() {
    if (success.isPresent()) {
      return Maybe.just(success.get());
    }

    return Maybe.nothing();
  }

  /**
   * Transforms the handler to an {@link Optional}. If the value was resolved,
   * the {@link Optional} will contain it. It will be {@code empty} otherwise.
   * 
   * @return the resolved value wrapped in an {@link Optional} if present. An
   *         {@code empty} {@link Optional} otherwise.
   */
  public Optional<T> toOptional() {
    return this.success;
  }

  /**
   * Map the value to an {@link AutoCloseable} resource to use it in another
   * {@code resolveClosing} or {@code runEffectClosing} that will close the
   * resource at the end of the operation.
   * <p>
   * If the value is not present, the {@link ResourceHolder} returned will be
   * empty. This means that any further {@code resolveClosing} operation or
   * {@code runEffectClosing} operation will not be executed either.
   * 
   * @param <R> the type of the {@link AutoCloseable} resource
   * @param mapper the function to map the value to a resource
   * @return a {@link ResourceHolder} with the mapped resource if the value is
   *         present. An empty {@link ResourceHolder} otherwise
   * 
   * @see ResourceHolder#resolveClosing(FunctionChecked)
   * @see ResourceHolder#runEffectClosing(ConsumerChecked)
   */
  public <R extends AutoCloseable> ResourceHolder<R> mapToResource(final Function<T, R> mapper) {
    return ResourceHolder.from(
      this.success.isPresent()
        ? mapper.apply(this.success.get())
        : null
    );
  }
}

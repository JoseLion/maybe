package com.github.joselion.maybe;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.github.joselion.maybe.exceptions.WrappingException;
import com.github.joselion.maybe.util.ConsumerChecked;
import com.github.joselion.maybe.util.FunctionChecked;

import org.eclipse.jdt.annotation.Nullable;

/**
 * ResolveHandler is an API to handle the possible error of a {@link Maybe}'s
 * resolve operation. It can return back to maybe to continue linking operations,
 * or use terminal methods to return a safe value.
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
   * Internal use method to instantiate a ResolveHandler with a success value
   * 
   * @param <T> the type of the success value
   * @param <E> the type of the possible exception
   * @param success the success value to instantiate the ResolveHandler
   * @return a ResolveHandler instance with a success value
   */
  static <T, E extends Exception> ResolveHandler<T, E> withSuccess(final T success) {
    return new ResolveHandler<>(success, null);
  }

  /**
   * Internal use method to instantiate a ResolveHandler with an error value
   * 
   * @param <T> the type of the success value
   * @param <E> the type of the possible exception
   * @param error the error to instantiate the ResolveHandler
   * @return a ResolveHandler instance with an error value
   */
  static <T, E extends Exception> ResolveHandler<T, E> withError(final E error) {
    return new ResolveHandler<>(null, error);
  }

  /**
   * Internal use method to instantiate a ResolveHandler neither with a success
   * nor with an error value
   * 
   * @param <T> the type of the success value
   * @param <E> the type of the possible exception
   * @return a ResolveHandler with neither the success nor the error value
   */
  static <T, E extends Exception> ResolveHandler<T, E> withNothing() {
    return new ResolveHandler<>(null, null);
  }

  /**
   * Internal use only.
   *
   * @return the possible success value
   */
  Optional<T> success() {
    return success;
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
   * Run an effect if an error is present and is instance of the provided type.
   * The error is passed in the argument of to the {@code effect} consumer.
   *
   * @param <X> the type of the error to match
   * @param ofType a class instance of the error type to match
   * @param effect a consumer function with the error passed in the argument
   * @return the same handler to continue chainning operations
   */
  public <X extends Exception> ResolveHandler<T, E> doOnError(final Class<X> ofType, final Consumer<X> effect) {
    if (error.isPresent() && ofType.isInstance(error.get())) {
      final X exception = ofType.cast(error.get());
      effect.accept(exception);
    }

    return this;
  }

  /**
   * Run an effect if an error is present and is instance of the provided type.
   *
   * @param <X> the type of the error to match
   * @param ofType a class instance of the error type to match
   * @param effect a runnable function
   * @return the same handler to continue chainning operations
   */
  public <X extends Exception> ResolveHandler<T, E> doOnError(final Class<X> ofType, final Runnable effect) {
    return this.doOnError(ofType, err -> effect.run());
  }

  /**
   * Run an effect if an error is present. The error is passed in the argument
   * of to the {@code effect} consumer.
   * 
   * @param effect a consumer function with the error passed in the argument
   * @return the same handler to continue chainning operations
   */
  public ResolveHandler<T, E> doOnError(final Consumer<E> effect) {
    error.ifPresent(effect);

    return this;
  }

  /**
   * Run an effect if an error is present.
   *
   * @param effect a runnable function
   * @return the same handler to continue chainning operations
   */
  public ResolveHandler<T, E> doOnError(final Runnable effect) {
    return this.doOnError(err -> effect.run());
  }

  /**
   * Catch the error if it's instance of the provided type. Then handle the
   * error and return a new value. The caught error is passed in the argument
   * of the {@code handler} function.
   * 
   * @param <X> the type of the error to catch
   * @param ofType a class instance of the error type to catch
   * @param handler a function that recieves the caught error in the argument
   *                and produces another value
   * @return a handler containing a new value if an error instance of the
   *         provided type was caught. The same handler instance otherwise
   */
  public <X extends E> ResolveHandler<T, E> catchError(final Class<X> ofType, final Function<X, T> handler) {
    if (error.isPresent() && ofType.isInstance(error.get())) {
      final X exception = ofType.cast(error.get());
      return withSuccess(handler.apply(exception));
    }

    return this;
  }

  /**
   * Catch the error if it's instance of the provided type. Then handle the
   * error and return a new value.
   *
   * @param <X> the type of the error to catch
   * @param ofType a class instance of the error type to catch
   * @param handler a supplier function that produces another value
   * @return a handler containing a new value if an error instance of the
   *         provided type was caught. The same handler instance otherwise
   */
  public <X extends E> ResolveHandler<T, E> catchError(final Class<X> ofType, final Supplier<T> handler) {
    return this.catchError(ofType, err -> handler.get());
  }

  /**
   * Catch the error of any type and handle it to return a new value. The caught
   * error is passed in the argument of the {@code handler} function.
   *
   * @param handler a function that recieves the caught error in the argument
   *                and returns another value
   * @return a handler containing a new value if an error was caught. The same
   *         handler instance otherwise
   */
  public ResolveHandler<T, E> catchError(final Function<E, T> handler) {
    return error.isPresent()
      ? withSuccess(handler.apply(error.get()))
      : this;
  }

  /**
   * Catch the error of any type and handle it to return a new value through the
   * supplier function.
   *
   * @param supplier a supplier function that produces another value
   * @return a handler containing a new value if an error was caught. The same
   *         handler instance otherwise
   */
  public ResolveHandler<T, E> catchError(final Supplier<T> supplier) {
    return catchError(err -> supplier.get());
  }

  /**
   * If the value is present, map it to another value through the {@code mapper}
   * function. If the error is present, the {@code mapper} is never applied and
   * the next handler will still contain the error.
   * <p>
   * If neither the value nor the error is present, it returns an empty handler.
   * 
   * @param <U> the type the value will be mapped to
   * @param mapper a function to map the value to another (if present)
   * @return a new handler with either the mapped value, the previous error, or
   *         nothing
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
   * If a value is present, and the value matches the given {@code predicate},
   * returns a new handler with the value, otherwise returns an empty handler.
   * If the error is present, the {@code predicate} is never applied and, the
   * next handler will still contain the error.
   * <p>
   * If neither the value nor the error is present, it returns an empty handler.
   * 
   * @param predicate a predicate to apply to the value (if present)
   * @return a new handler with either the value if it matched the predicate,
   *         the previous error, or nothing
   */
  public ResolveHandler<T, E> filter(final Predicate<T> predicate) {
    if (success.isPresent()) {
      final T value = success.get();

      return predicate.test(value)
        ? withSuccess(value)
        : withNothing();
    }

    if (error.isPresent()) {
      return withError(error.get());
    }

    return withNothing();
  }

  /**
   * If the value is present, cast the value to anoter type. If the cast fails
   * or if the error is present, it returns a new handler which contains a
   * {@link WrappingException} instance. The type of the wrapped exception will
   * be a {@link ClassCastException} if the cast operation failed, or the
   * previous expection type {@link E} given it was present.
   * <p>
   * If neither the value nor the error is present, it returns an empty handler.
   * 
   * @param <U> the type the value will be cast to
   * @param type the class instance of the type to cast
   * @return a new handler with either the cast value, a {@link WrappingException},
   *         or nothing
   */
  public <U> ResolveHandler<U, WrappingException> cast(final Class<U> type) {
    if (success.isPresent()) {
      try {
        final U newValue = type.cast(success.get());
        return withSuccess(newValue);
      } catch (ClassCastException e) {
        return withError(WrappingException.of(e));
      }
    }

    if (error.isPresent()) {
      return withError(WrappingException.of(error.get()));
    }

    return withNothing();
  }

  /**
   * Returns the resolved value if present. Another value otherwise.
   * 
   * @param other the value to return if the operation failed to resolve
   * @return the resolved value if present. Another value otherwise
   */
  public T orElse(final T other) {
    return success.orElse(other);
  }

  /**
   * Returns the resolved value if present. Otherwise, the result produced by
   * the mapping function, which has the error on its argument, and returns
   * another value.
   *
   * @param mapper the mapping function that produces another value if the
   *               opration failed to resolve
   * @return the resolved value if present. Another value otherwise
   */
  public T orElse(final Function<E, T> mapper) {
    return success.orElseGet(() -> mapper.apply(error.get()));
  }

  /**
   * Returns the resolved value if present. Otherwise, the result produced by
   * the supplying function as another value.
   *
   * @param supplier the supplying function that produces another value if the
   *                 opration failed to resolve
   * @return the resolved value if present. Another value otherwise
   */
  public T orElse(final Supplier<T> supplier) {
    return success.orElseGet(supplier::get);
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
    return success.orElseThrow(() -> errorMapper.apply(error.orElseThrow()));
  }

  /**
   * Transforms the handler to a {@link Maybe}. If the value was resolved, the
   * {@link Maybe} will contain it. It will have {@code nothing} otherwise.
   * 
   * @return the resolved value wrapped in a {@link Maybe} if present. A
   *         {@link Maybe} with nothing otherwise
   */
  public Maybe<T> toMaybe() {
    return success
      .map(Maybe::just)
      .orElseGet(Maybe::nothing);
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
    return this.success
      .map(mapper)
      .map(ResourceHolder::from)
      .orElseGet(() -> ResourceHolder.from(null));
  }
}

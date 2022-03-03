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
   * Run an effect if the operation resolved successfully. The resolved value
   * is passed in the argument of the {@code effect} consumer.
   *
   * @param effect a function that receives the resolved value
   * @return the same handler to continue chainning operations
   */
  public ResolveHandler<T, E> doOnSuccess(final Consumer<T> effect) {
    success.ifPresent(effect);

    return this;
  }

  /**
   * Run an effect if the operation resolved successfully.
   *
   * @param effect a runnable function
   * @return the same handler to continue chainning operations
   */
  public ResolveHandler<T, E> doOnSuccess(final Runnable effect) {
    return this.doOnSuccess(resolved -> effect.run());
  }

  /**
   * Run an effect if the error is present and is an instance of the provided
   * type. The error is passed in the argument of the {@code effect}
   * consumer.
   *
   * @param <X> the type of the error to match
   * @param ofType a class instance of the error type to match
   * @param effect a consumer function that receives the caught error
   * @return the same handler to continue chainning operations
   */
  public <X extends Exception> ResolveHandler<T, E> doOnError(final Class<X> ofType, final Consumer<X> effect) {
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
  public <X extends Exception> ResolveHandler<T, E> doOnError(final Class<X> ofType, final Runnable effect) {
    return this.doOnError(ofType, caught -> effect.run());
  }

  /**
   * Run an effect if the error is present. The error is passed in the argument
   * of the {@code effect} consumer.
   * 
   * @param effect a consumer function that receives the caught error
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
    return this.doOnError(caught -> effect.run());
  }

  /**
   * Catch the error if is present and is an instance of the provided type.
   * Then handle the error and return a new value. The caught error is passed
   * in the argument of the {@code handler} function.
   * 
   * @param <X> the type of the error to catch
   * @param ofType a class instance of the error type to catch
   * @param handler a function that receives the caught error and produces
   *                another value
   * @return a handler containing a new value if an error instance of the
   *         provided type was caught. The same handler instance otherwise
   */
  public <X extends E> ResolveHandler<T, E> catchError(final Class<X> ofType, final Function<X, T> handler) {
    return error.filter(ofType::isInstance)
      .map(ofType::cast)
      .map(handler::apply)
      .map(ResolveHandler::<T, E>withSuccess)
      .orElse(this);
  }

  /**
   * Catch the error if is present and is an instance of the provided type.
   * Then handle the error and return a new value.
   *
   * @param <X> the type of the error to catch
   * @param ofType a class instance of the error type to catch
   * @param handler a supplier function that produces another value
   * @return a handler containing a new value if an error instance of the
   *         provided type was caught. The same handler instance otherwise
   */
  public <X extends E> ResolveHandler<T, E> catchError(final Class<X> ofType, final Supplier<T> handler) {
    return this.catchError(ofType, caught -> handler.get());
  }

  /**
   * Catch the error if is present and handle it to return a new value. The
   * caught error is passed in the argument of the {@code handler} function.
   *
   * @param handler a function that receives the caught error and produces
   *                another value
   * @return a handler containing a new value if an error was caught. The same
   *         handler instance otherwise
   */
  public ResolveHandler<T, E> catchError(final Function<E, T> handler) {
    return error.map(handler::apply)
      .map(ResolveHandler::<T, E>withSuccess)
      .orElse(this);
  }

  /**
   * Catch the error if is present and handle it to return a new value through
   * the supplier function.
   *
   * @param supplier a supplier function that produces another value
   * @return a handler containing a new value if an error was caught. The same
   *         handler instance otherwise
   */
  public ResolveHandler<T, E> catchError(final Supplier<T> supplier) {
    return catchError(caught -> supplier.get());
  }

  /**
   * Chain another resolver if the value is present. Otherwise, ignore the
   * error and return a new handler with {@code nothing}. This operator is
   * effectively an alias for {@code .toMaybe().resolve(..)}.
   *
   * @param <S> the type of value returned by the next operation
   * @param <X> the type of exception the new resolver may throw
   * @param resolver a checked function that receives the current value and
   *                 resolves another
   * @return a new handler with either the resolved value, the thrown exception
   *         to be handled, or nothing
   */
  public <S, X extends Exception> ResolveHandler<S, X> resolve(final FunctionChecked<T, S, X> resolver) {
    return toMaybe().resolve(resolver);
  }

  /**
   * Chain another resolver covering both cases of success or error. This could
   * be useful when we don't want to ignore any possible error and handle it
   * with another resolver.
   * <p>
   * The first callback receives the resolved value, the second callback the
   * caught error. Both should resolve another value of the same type {@code S},
   * but only one of the callbacks is invoked. It depends on which whether the
   * previous value was resolved or not.
   *
   * @param <S> the type of value returned by the next operation
   * @param <X> the type of exception the new resolver may throw
   * @param successResolver a checked function that receives the current value
   *                        and resolves another
   * @param errorResolver a checked function that receives the error and
   *                      resolves another value
   * @return a new handler with either the resolved value, the thrown exception
   *         to be handled, or nothing
   */
  public <S, X extends Exception> ResolveHandler<S, X> resolve(
    final FunctionChecked<T, S, X> successResolver,
    final FunctionChecked<E, S, X> errorResolver
  ) {
    return error.map(Maybe.fromResolver(errorResolver))
      .orElseGet(() -> this.resolve(successResolver));
  }

  /**
   * If the value is present, map it to another value through the {@code mapper}
   * function. If the error is present, the {@code mapper} is never applied and
   * the next handler will still contain the error.
   * <p>
   * If neither the value nor the error is present, it returns an empty handler.
   * 
   * @param <U> the type the value will be mapped to
   * @param mapper a function that receives the resolved value and produces another
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
   * returns the same handler to continue chaining operations. Otherwise, it
   * returns an empty handler.
   * <p>
   * If the error is present, the {@code predicate} is never applied and it
   * returns the same handler to continue chaining operations
   * <p>
   * If neither the value nor the error are present, it returns an empty handler.
   * 
   * @param predicate a predicate to apply to the resolved value
   * @return a new handler with either the value if it matched the predicate,
   *         the previous error, or nothing
   */
  public ResolveHandler<T, E> filter(final Predicate<T> predicate) {
    if (success.isPresent()) {
      return success.filter(predicate)
        .map(value -> this)
        .orElseGet(ResolveHandler::withNothing);
    }

    return error.isPresent()
      ? this
      : withNothing();
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
   * @param mapper a function that receives the caught error and produces
   *               another value
   * @return the resolved value if present. Another value otherwise
   */
  public T orElse(final Function<E, T> mapper) {
    return success.orElseGet(() -> mapper.apply(error.get()));
  }

  /**
   * Returns the resolved value if present. Otherwise, the result produced by
   * the supplying function as another value.
   *
   * @apiNote Use this method instead of {@link #orElse(Object)} to do lazy
   *          evaluation of the produced value. That means that the "else"
   *          value won't be evaluated if the error is not present.
   * @param supplier the supplying function that produces another value if the
   *                 opration failed to resolve
   * @return the resolved value if present. Another value otherwise
   */
  public T orElseGet(final Supplier<T> supplier) {
    return success.orElseGet(supplier);
  }

  /**
   * Returns the resolved value if present. Just {@code null} otherwise.
   * <p>
   * It's strongly encouraged to use {@link #toOptional()} instead to better
   * handle nullability, but if you really need to return {@code null} in case
   * of error, you should only use this method.
   * <p>
   * Using {@code .orElse(null)} will result in ambiguity between
   * {@link #orElse(Function)} and {@link #orElse(Object)}.
   *
   * @return the resolved value if present. Just {@code null} otherwise.
   */
  public @Nullable T orNull() {
    return success.orElse(null);
  }

  /**
   * Returns the resolved value if present. Throws the error otherwise.
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
   * @param mapper a function that receives the caught error and produces
   *               another exception
   * @return the resolved/handled value if present
   * @throws X a mapped exception
   */
  public <X extends Throwable> T orThrow(final Function<E, X> mapper) throws X {
    return success.orElseThrow(() -> mapper.apply(error.orElseThrow()));
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
   * @param mapper a function that receives the resolved value and produces an
   *               autoclosable resource
   * @return a {@link ResourceHolder} with the mapped resource if the value is
   *         present. An empty {@link ResourceHolder} otherwise
   * 
   * @see ResourceHolder#resolveClosing(FunctionChecked)
   * @see ResourceHolder#runEffectClosing(ConsumerChecked)
   */
  public <R extends AutoCloseable> ResourceHolder<R> mapToResource(final Function<T, R> mapper) {
    return this.success.map(mapper)
      .map(ResourceHolder::from)
      .orElseGet(() -> ResourceHolder.from(null));
  }
}

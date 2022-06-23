package io.github.joselion.maybe;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.Nullable;

import io.github.joselion.maybe.util.Either;
import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingFunction;

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

  private final Either<E, T> value;

  private ResolveHandler(final Either<E, T> value) {
    this.value = value;
  }

  /**
   * Internal use method to instantiate a ResolveHandler of a success value
   * 
   * @param <T> the type of the success value
   * @param <E> the type of the possible exception
   * @param success the success value to instantiate the ResolveHandler
   * @return a ResolveHandler instance with a success value
   */
  static <T, E extends Exception> ResolveHandler<T, E> ofSuccess(final T success) {
    return new ResolveHandler<>(Either.ofRight(success));
  }

  /**
   * Internal use method to instantiate a ResolveHandler of an error value
   * 
   * @param <T> the type of the success value
   * @param <E> the type of the possible exception
   * @param error the error to instantiate the ResolveHandler
   * @return a ResolveHandler instance with an error value
   */
  static <T, E extends Exception> ResolveHandler<T, E> ofError(final E error) {
    return new ResolveHandler<>(Either.ofLeft(error));
  }

  /**
   * Internal use only.
   *
   * @return the possible success value
   */
  Optional<T> success() {
    return value.rightToOptional();
  }

  /**
   * Internal use only.
   *
   * @return the possible thrown exception
   */
  Optional<E> error() {
    return value.leftToOptional();
  }

  /**
   * Run an effect if the operation resolved successfully. The resolved value
   * is passed in the argument of the {@code effect} consumer.
   *
   * @param effect a function that receives the resolved value
   * @return the same handler to continue chainning operations
   */
  public ResolveHandler<T, E> doOnSuccess(final Consumer<T> effect) {
    value.doOnRight(effect);

    return this;
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
    value.leftToOptional()
      .filter(ofType::isInstance)
      .map(ofType::cast)
      .ifPresent(effect);

    return this;
  }

  /**
   * Run an effect if the error is present. The error is passed in the argument
   * of the {@code effect} consumer.
   * 
   * @param effect a consumer function that receives the caught error
   * @return the same handler to continue chainning operations
   */
  public ResolveHandler<T, E> doOnError(final Consumer<E> effect) {
    value.doOnLeft(effect);

    return this;
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
    return value
      .leftToOptional()
      .filter(ofType::isInstance)
      .map(ofType::cast)
      .map(handler)
      .map(ResolveHandler::<T, E>ofSuccess)
      .orElse(this);
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
    return value
      .mapLeft(handler)
      .mapLeft(ResolveHandler::<T, E>ofSuccess)
      .leftOrElse(this);
  }

  /**
   * Chain another resolver covering both cases of success or error of the
   * previous resolver in two different callbacks.
   * <p>
   * The first callback receives the resolved value, the second callback the
   * caught error. Both should resolve another value of the same type {@code S},
   * but only one of the callbacks is invoked. It depends on whether the
   * previous value was resolved or not.
   *
   * @param <S> the type of value returned by the next operation
   * @param <X> the type of exception the new resolver may throw
   * @param onSuccess a checked function that receives the current value
   *                        and resolves another
   * @param onError a checked function that receives the error and
   *                      resolves another value
   * @return a new handler with either the resolved value or the error
   */
  public <S, X extends Exception> ResolveHandler<S, X> resolve(
    final ThrowingFunction<T, S, X> onSuccess,
    final ThrowingFunction<E, S, X> onError
  ) {
    return value.unwrap(
      Maybe.partialResolver(onError),
      Maybe.partialResolver(onSuccess)
    );
  }

  /**
   * Chain another resolver function if the value was resolved. Otherwise,
   * returns a handler containing the error so it can be propagated upstream.
   *
   * @param <S> the type of value returned by the next operation
   * @param <X> the type of exception the new resolver may throw
   * @param resolver a checked function that receives the current value and
   *                 resolves another
   * @return a new handler with either the resolved value or an error
   */
  @SuppressWarnings("unchecked")
  public <S, X extends Exception> ResolveHandler<S, X> resolve(final ThrowingFunction<T, S, X> resolver) {
    return value.unwrap(
      error -> ResolveHandler.ofError((X) error),
      Maybe.partialResolver(resolver)
    );
  }

  /**
   * Chain the previous operation to an effect covering both the success or
   * error cases in two different callbacks.
   *
   * @param <X> the type of the error the effect may throw
   * @param onSuccess a consumer checked function to run in case of succeess
   * @param onError a consumer checked function to run in case of error
   * @return an {@link EffectHandler} representing the result of one of the
   *         invoked callback
   */
  public <X extends Exception> EffectHandler<X> runEffect(
    final ThrowingConsumer<T, X> onSuccess,
    final ThrowingConsumer<E, X> onError
  ) {
    return value.unwrap(
      Maybe.partialEffect(onError),
      Maybe.partialEffect(onSuccess)
    );
  }

  /**
   * Chain the previous operation to an effect if the value was resolved.
   * Otherwise, returns a handler containing the error so it can be propagated
   * upstream.
   *
   * @param <X> the type of the error the effect may throw
   * @param effect a consume checked function to run in case of succeess
   * @return a new {@link EffectHandler} representing the result of the success
   *         callback or containg the error
   */
  @SuppressWarnings("unchecked")
  public <X extends Exception> EffectHandler<X> runEffect(final ThrowingConsumer<T, X> effect) {
    return value.unwrap(
      error -> EffectHandler.ofError((X) error),
      Maybe.partialEffect(effect)
    );
  }

  /**
   * If the value is present, map it to another value through the {@code mapper}
   * function. If the error is present, the {@code mapper} is never applied and
   * the next handler will still contain the error.
   * 
   * @param <U> the type the value will be mapped to
   * @param mapper a function that receives the resolved value and produces another
   * @return a new handler with either the mapped value, or the previous error
   */
  public <U> ResolveHandler<U, E> map(final Function<T, U> mapper) {
    return value
      .mapRight(mapper)
      .unwrap(
        ResolveHandler::ofError,
        ResolveHandler::ofSuccess
      );
  }

  /**
   * If the value is present, cast the value to anoter type. If the cast fails
   * or if the error is present, it returns a new handler which contains a
   * {@link ClassCastException} error.
   * 
   * @param <U> the type the value will be cast to
   * @param type the class instance of the type to cast
   * @return a new handler with either the cast value or a ClassCastException
   *         error
   */
  public <U> ResolveHandler<U, ClassCastException> cast(final Class<U> type) {
    return value.unwrap(
      error -> ofError(new ClassCastException(error.getMessage())),
      success -> {
        try {
          return ofSuccess(type.cast(success));
        } catch (ClassCastException error) {
          return ofError(error);
        }
      }
    );
  }

  /**
   * Returns the resolved value if present. Another value otherwise.
   * 
   * @param fallback the value to return if the operation failed to resolve
   * @return the resolved value if present. Another value otherwise
   */
  public T orElse(final T fallback) {
    return value.rightOrElse(fallback);
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
    return value.unwrap(mapper, Function.identity());
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
    return value
      .rightToOptional()
      .orElseGet(supplier);
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
    return value.rightOrNull();
  }

  /**
   * Returns the resolved value if present. Throws the error otherwise.
   * 
   * @return the resolved/handled value if present
   * @throws E the error thrown by the {@code resolve} operation
   */
  public T orThrow() throws E {
    return value
      .rightToOptional()
      .orElseThrow(value::leftOrNull);
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
    return value
      .rightToOptional()
      .orElseThrow(() -> mapper.apply(value.leftOrNull()));
  }

  /**
   * Transforms the handler to a {@link Maybe} that contains either the
   * resolved value or the error.
   * 
   * @return the resolved value wrapped in a {@link Maybe} or holding the error
   */
  public Maybe<T> toMaybe() {
    return value
      .rightToOptional()
      .map(Maybe::just)
      .orElseGet(Maybe::nothing);
  }

  /**
   * Transforms the handler to an {@link Optional}. If the value was resolved,
   * the {@link Optional} will contain it. Returs an {@code empty} optional
   * otherwise.
   * 
   * @return the resolved value wrapped in an {@link Optional} if present. An
   *         {@code empty} optional otherwise.
   */
  public Optional<T> toOptional() {
    return value.rightToOptional();
  }

  /**
   * Transforms the handler to an {@link Either}, in which the left side might
   * contain the error and the right side might contain the resolved value.
   * <p>
   * The benefit of transforming to {@code Either} is that its implementation
   * ensures that only one of the two possible values is present at the same
   * time, never both nor none.
   * 
   * @return an {@code Either} with the resolved value on the right side or the
   *         error on the left
   */
  public Either<E, T> toEither() {
    return value;
  }

  /**
   * Map the value to an {@link AutoCloseable} resource to be use in either a
   * {@code resolveClosing} or a {@code runEffectClosing} operation, which will
   * close the resource when it completes. If the value was not resolved, the
   * error is propagated to the {@link ResourceHolder}.
   * 
   * @param <R> the type of the {@link AutoCloseable} resource
   * @param mapper a function that receives the resolved value and produces an
   *               autoclosable resource
   * @return a {@link ResourceHolder} with the mapped resource if the value is
   *         present or the error otherwise.
   * 
   * @see ResourceHolder#resolveClosing(ThrowingFunction)
   * @see ResourceHolder#runEffectClosing(ThrowingConsumer)
   */
  public <R extends AutoCloseable> ResourceHolder<R, E> mapToResource(final Function<T, R> mapper) {
    return value
      .mapRight(mapper)
      .unwrap(
        ResourceHolder::failure,
        ResourceHolder::from
      );
  }
}

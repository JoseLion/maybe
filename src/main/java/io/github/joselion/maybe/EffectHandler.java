package io.github.joselion.maybe;

import static java.util.Objects.isNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.Nullable;

import io.github.joselion.maybe.helpers.Commons;
import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingFunction;
import io.github.joselion.maybe.util.function.ThrowingRunnable;
import io.github.joselion.maybe.util.function.ThrowingSupplier;

/**
 * EffectHandler is an API to handle the posible error of a {@link Maybe}'s
 * effect operation. It can return back to maybe to continue linking operations,
 * or use termimal methods to return a safe value.
 *
 * @param <E> the type of exception that the effect may throw
 * @author Jose Luis Leon
 * @since v0.3.2
 */
public final class EffectHandler<E extends Throwable> {

  private final Optional<E> error;

  private EffectHandler(final @Nullable E error) {
    this.error = Optional.ofNullable(error);
  }

  /**
   * Internal use method to instantiate an {@link EffectHandler} that has no
   * error (yet).
   *
   * @param <E> the type of the possible exception
   * @return a EffectHandler with neither the success nor the error value
   */
  static <E extends Throwable> EffectHandler<E> empty() {
    return new EffectHandler<>(null);
  }

  /**
   * Internal use method to instantiate an {@link EffectHandler} with an error.
   *
   * @param <E> the type of the possible exception
   * @param error the error to instanciate the EffectHandler
   * @return a EffectHandler instance with an error value
   */
  static <E extends Throwable> EffectHandler<E> failure(final E error) {
    return new EffectHandler<>(
      isNull(error) // NOSONAR
        ? Commons.cast(new NullPointerException("The \"Maybe<T>\" error was null"))
        : error
    );
  }

  /**
   * Internal use only.
   *
   * @return the possible thrown exception
   */
  Optional<E> error() {
    return this.error;
  }

  /**
   * Runs an effect if the operation succeeds.
   *
   * @param effect the runnable to run on success
   * @return the same handler to continue chainning operations
   */
  public EffectHandler<E> doOnSuccess(final Runnable effect) {
    if (this.error.isEmpty()) {
      effect.run();
    }

    return this;
  }

  /**
   * Run an effect if the error is present and is an instance of the provided
   * type. The error is passed in the argument of to the {@code effect}
   * consumer.
   *
   * @param <X> the type of the error to match
   * @param ofType a class instance of the error type to match
   * @param effect a consumer that recieves the caught error
   * @return the same handler to continue chainning operations
   */
  public <X extends Throwable> EffectHandler<E> doOnError(final Class<X> ofType, final Consumer<? super X> effect) {
    this.error
      .filter(ofType::isInstance)
      .map(ofType::cast)
      .ifPresent(effect);

    return this;
  }

  /**
   * Run an effect if the error is present. The error is passed in the argument
   * of the {@code effect} consumer.
   *
   * @param effect a consumer that recieves the caught error
   * @return the same handler to continue chainning operations
   */
  public EffectHandler<E> doOnError(final Consumer<Throwable> effect) {
    this.error.ifPresent(effect);

    return this;
  }

  /**
   * Catch the error if present and if it's instance of the provided type.
   * The caught error is passed to the argument of the handler consumer. If the
   * error is caught and handled, the operation returns an empty
   * {@link EffectHandler}. Otherwise, the same instance is returned.
   *
   * @param <X> the type of the error to catch
   * @param ofType thetype of the error to catch
   * @param handler a consumer that receives the caught error
   * @return an empty handler if an error instance of the provided type was
   *         caught. The same handler instance otherwise
   */
  public <X extends Throwable> EffectHandler<E> catchError(final Class<X> ofType, final Consumer<? super X> handler) {
    return this.error
      .filter(ofType::isInstance)
      .map(ofType::cast)
      .map(caught -> {
        handler.accept(caught);
        return EffectHandler.<E>empty();
      })
      .orElse(this);
  }

  /**
   * Catch the error if is present. The caught error is passed in the argument
   * of the {@code handler} consumer. Since the error was caught and handled,
   * the operations returns an empty {@link EffectHandler}. Otherwise, the same
   * instance is returned.
   *
   * @param handler a consumer that recieves the caught error
   * @return an empty handler if the error is present. The same handler
   *         instance otherwise
   */
  public EffectHandler<E> catchError(final Consumer<Throwable> handler) {
    return this.error
      .map(caught -> {
        handler.accept(caught);
        return EffectHandler.<E>empty();
      })
      .orElse(this);
  }

  /**
   * Chain another effect covering both cases of success or error of the
   * previous effect in two different callbacks.
   *
   * @param <X> the type of the error the new effect may throw
   * @param onSuccess a throwing runnable to run in case of succeess
   * @param onError a throwing runnable to run in case of error
   * @return a new {@link EffectHandler} representing the result of one of the
   *         invoked callback
   */
  public <X extends Throwable> EffectHandler<X> effect(
    final ThrowingRunnable<? extends X> onSuccess,
    final ThrowingConsumer<Throwable, ? extends X> onError
  ) {
    return this.error
      .map(e -> {
        try {
          onError.accept(e);
          return EffectHandler.failure(Commons.<X>cast(e));
        } catch (Throwable x) { // NOSONAR
          return EffectHandler.failure(Commons.<X>cast(x));
        }
      })
      .orElseGet(() -> Maybe.from(onSuccess));
  }

  /**
   * Chain another effect if the previous completed with no error. Otherwise,
   * ignores the current error and return a new {@link EffectHandler} that is
   * either empty or has a different error cause by the next effect.
   *
   * @param <X> the type of the error the new effect may throw
   * @param effect a throwing runnable to run in case of succeess
   * @return a new {@link EffectHandler} that is either empty or with the
   *         thrown error
   */
  public <X extends Throwable> EffectHandler<X> effect(final ThrowingRunnable<? extends X> effect) {
    return this.effect(effect, err -> { });
  }

  /**
   * Chain a solver covering both cases of success or error of the
   * previous effect in two different callbacks.
   *
   * <p>The second callback receives the caught error. Both callbacks should
   * solve a value of the same type {@code T}, but only one of the callbacks is
   * invoked. It depends on whether the previous effect(s) threw an error or not.
   *
   * @param <T> the type of the value to be solved
   * @param <X> the type of exception the callbacks may throw
   * @param onSuccess a throwing supplier that solves a value
   * @param onError a throwing function that receives the error and solves a value
   * @return a {@link SolveHandler} with either the solved value or the error
   */
  public <T, X extends Throwable> SolveHandler<T, X> solve(
    final ThrowingSupplier<T, X> onSuccess,
    final ThrowingFunction<Throwable, T, X> onError
  ) {
    return this.error
      .map(Maybe.partial(onError))
      .orElseGet(() -> Maybe.from(onSuccess));
  }

  /**
   * Terminal operation to supply a {@code T} value. If no error is present the
   * {@code onSuccess} callback is used. Otherwise, the {@code onError}
   * callback which receives the caught error.
   *
   * <p>Both callbacks should return a value of the same type {@code T}, but
   * only one of the callbacks is invoked. It depends on whether the previous
   * effect(s) threw an error or not.
   *
   * @param <T> the type of the value to supplied
   * @param onSuccess a supplier that provides a value
   * @param onError a function that receives the error and returns a value
   * @return either the success or the error mapped value
   */
  public <T> T thenGet(final Supplier<? extends T> onSuccess, final Function<Throwable, ? extends T> onError) {
    return this.error
      .map(onError)
      .orElseGet(() -> Commons.cast(onSuccess.get()));
  }

  /**
   * Terminal operation to return a {@code T} value. If no error is present the
   * {@code value} param is returned, {@code fallback} otherwise.
   *
   * <p>Both values should be of the same type {@code T}, but only one is
   * returned. It depends on whether the previous effect(s) threw an error
   * or not.
   *
   * @param <T> the type of the value to return
   * @param value the value to return on success
   * @param fallback the value to return on error
   * @return either the success or the error value
   */
  public <T> T thenReturn(final T value, final T fallback) {
    return this.error.isEmpty()
      ? value
      : fallback;
  }

  /**
   * Terminal operation to handle the error if present. The error is passed in
   * the argument of the {@code effect} consumer.
   *
   * @param effect a consumer that receives the caught error
   */
  public void orElse(final Consumer<Throwable> effect) {
    this.error.ifPresent(effect);
  }

  /**
   * Throws the error if present. Does nothing otherwise.
   *
   * @throws E the error thrown by the {@code effect} operation
   */
  public void orThrow() throws E {
    if (this.error.isPresent()) {
      throw this.error.get();
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
  public <X extends Throwable> void orThrow(final Function<Throwable, ? extends X> mapper) throws X {
    if (this.error.isPresent()) {
      throw mapper.apply(this.error.get());
    }
  }
}

package io.github.joselion.maybe;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;

import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingRunnable;

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
  static <E extends Throwable> EffectHandler<E> ofError(final E error) {
    return new EffectHandler<>(error);
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
   * @param effect a runnable function
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
   * @param effect a consumer function that recieves the caught error
   * @return the same handler to continue chainning operations
   */
  public <X extends Throwable> EffectHandler<E> doOnError(final Class<X> ofType, final Consumer<X> effect) {
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
   * @param effect a consumer function that recieves the caught error
   * @return the same handler to continue chainning operations
   */
  public EffectHandler<E> doOnError(final Consumer<E> effect) {
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
   * @param handler a consumer function that receives the caught error
   * @return an empty handler if an error instance of the provided type was
   *         caught. The same handler instance otherwise
   */
  public <X extends E> EffectHandler<E> catchError(final Class<X> ofType, final Consumer<X> handler) {
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
   * @param handler a consumer function that recieves the caught error
   * @return an empty handler if the error is present. The same handler
   *         instance otherwise
   */
  public EffectHandler<E> catchError(final Consumer<E> handler) {
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
   * @param onSuccess a runnable checked function to run in case of succeess
   * @param onError a runnable checked function to run in case of error
   * @return a new {@link EffectHandler} representing the result of one of the
   *         invoked callback
   */
  public <X extends Throwable> EffectHandler<X> runEffect(
    final ThrowingRunnable<X> onSuccess,
    final ThrowingConsumer<E, X> onError
  ) {
    return this.error
      .map(Maybe.partialEffect(onError))
      .orElseGet(() -> Maybe.fromEffect(onSuccess));
  }

  /**
   * Chain another effect if the previous completed with no error. Otherwise,
   * ignores the current error and return a new {@link EffectHandler} that is
   * either empty or has a different error cause by the next effect.
   *
   * @param <X> the type of the error the new effect may throw
   * @param effect a runnable checked function to run in case of succeess
   * @return a new {@link EffectHandler} that is either empty or with the
   *         thrown error
   */
  public <X extends Throwable> EffectHandler<X> runEffect(final ThrowingRunnable<X> effect) {
    return this.runEffect(effect, err -> { });
  }

  /**
   * Terminal operation to handle the error if present. The error is passed in
   * the argument of the {@code effect} consumer.
   *
   * @param effect a consumer function that receives the caught error
   */
  public void orElse(final Consumer<E> effect) {
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
  public <X extends Throwable> void orThrow(final Function<E, X> mapper) throws X {
    if (this.error.isPresent()) {
      throw mapper.apply(this.error.get());
    }
  }
}

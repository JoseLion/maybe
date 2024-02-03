package io.github.joselion.maybe;

import static java.util.Objects.isNull;

import java.util.Optional;

import io.github.joselion.maybe.helpers.Commons;
import io.github.joselion.maybe.util.Either;
import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingFunction;

/**
 * CloseableHandler is an API that allows to solve or run an effect using an
 * {@link AutoCloseable} resource. This resource will be automatically closed
 * after the {@code solve} or the {@code effect} operation is finished.
 *
 * @param <T> The autoclosable type
 * @param <E> The throwable type
 * @author Jose Luis Leon
 * @since v1.3.0
 */
public final class CloseableHandler<T extends AutoCloseable, E extends Throwable> {

  private final Either<E, T> value;

  private CloseableHandler(final Either<E, T> value) {
    this.value = value;
  }

  /**
   * Internal use method to instatiate a CloseableHandler from a given resource.
   *
   * @param <T> the type of the resource
   * @param <E> the type of the error
   * @param resource the resource to instantiate the CloseableHandler with
   * @return a new instance of CloseableHandler with the given resource
   */
  static <T extends AutoCloseable, E extends Throwable> CloseableHandler<T, E> from(final T resource) {
    final var nullException = new NullPointerException("The \"Maybe<T>\" resource solved to null");
    final var either = isNull(resource) // NOSONAR
      ? Either.<E, T>ofLeft(Commons.cast(nullException))
      : Either.<E, T>ofRight(resource);

    return new CloseableHandler<>(either);
  }

  /**
   * Internal use method to instatiate a failed CloseableHandler from an exception.
   *
   * @param <T> the type of the resource
   * @param <E> the type of the error
   * @param error the error to instantiate the failed CloseableHandler
   * @return a new instance of the failed CloseableHandler with the error
   */
  static <T extends AutoCloseable, E extends Throwable> CloseableHandler<T, E> failure(final E error) {
    final var nullException = new NullPointerException("The \"Maybe<T>\" error was null");
    final var either = isNull(error) // NOSONAR
      ? Either.<E, T>ofLeft(Commons.cast(nullException))
      : Either.<E, T>ofLeft(error);

    return new CloseableHandler<>(either);
  }

  /**
   * Internal use only.
   *
   * @return the possible stored resource
   */
  Optional<T> resource() {
    return this.value.rightToOptional();
  }

  /**
   * Internal use only.
   *
   * @return the possible propagated error
   */
  Optional<E> error() {
    return this.value.leftToOptional();
  }

  /**
   * If the resource is present, solves the value of a throwing operation
   * using a {@link ThrowingFunction} expression which has the previously
   * prepared resource in the argument. The resource is automatically closed
   * after the operation finishes, just like a common try-with-resources
   * statement.
   *
   * <p>Returs a {@link SolveHandler} which allows to handle the possible error
   * and return a safe value. The returned handler is {@code empty} if neither
   * the resource nor the error is present.
   *
   * @param <S> the type of the value returned by the {@code solver}
   * @param <X> the type of exception the {@code solver} may throw
   * @param solver the checked function operation to solve
   * @return a {@link SolveHandler} with either the value solved or the thrown
   *         exception to be handled
   */
  public <S, X extends Throwable> SolveHandler<S, X> solve(
    final ThrowingFunction<? super T, ? extends S, ? extends X> solver
  ) {
    return this.value
      .mapLeft(Commons::<X>cast)
      .unwrap(
        SolveHandler::failure,
        resource -> {
          try (var res = resource) {
            return SolveHandler.from(solver.apply(res));
          } catch (final Throwable e) { //NOSONAR
            final var error = Commons.<X>cast(e);
            return SolveHandler.failure(error);
          }
        }
      );
  }

  /**
   * If the resource is present, runs an effect that may throw an exception
   * using a {@link ThrowingConsumer} expression which has the previously
   * prepared resource in the argument. The resource is automatically closed
   * after the operation finishes, just like a common try-with-resources
   * statement.
   *
   * <p>Returning then an {@link EffectHandler} which allows to handle the
   * possible error. The returned handler is {@code empty} if neither the
   * resource nor the error is present.
   *
   * @param <X> the type of exception the {@code effect} may throw
   * @param effect the checked consumer operation to execute
   * @return an {@link EffectHandler} with either the thrown exception to be
   *         handled or empty
   */
  public <X extends Throwable> EffectHandler<X> effect(
    final ThrowingConsumer<? super T, ? extends X> effect
  ) {
    return this.value
      .mapLeft(Commons::<X>cast)
      .unwrap(
        EffectHandler::failure,
        resource -> {
          try (var res = resource) {
            effect.accept(res);
            return EffectHandler.empty();
          } catch (final Throwable e) { // NOSONAR
            final var error = Commons.<X>cast(e);
            return EffectHandler.failure(error);
          }
        }
      );
  }
}

package io.github.joselion.maybe;

import java.util.Optional;

import io.github.joselion.maybe.util.Either;
import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingFunction;

/**
 * ResourceHolder is a "middle step" API that allows to resolve or run an effect
 * using a previously passed {@link AutoCloseable} resource. This resource will
 * be automatically closed after the {@code resolve} or the {@code effect}
 * operation is finished.
 *
 * @param <R> The autoclosable type
 * @param <E> The throwable type
 *
 * @author Jose Luis Leon
 * @since v1.3.0
 */
public class ResourceHolder<R extends AutoCloseable, E extends Throwable> {

  private final Either<E, R> value;

  private ResourceHolder(final R resource) {
    this.value = Either.ofRight(resource);
  }

  private ResourceHolder(final E error) {
    this.value = Either.ofLeft(error);
  }

  /**
   * Internal use method to instatiate a ResourceHolder from a given resource.
   * 
   * @param <R> the type of the resource
   * @param <E> the type of the error
   * @param resource the resource to instantiate the ResourceHolder with
   * @return a new instance of ResourceHolder with the given resource
   */
  static <R extends AutoCloseable, E extends Throwable> ResourceHolder<R, E> from(final R resource) {
    return new ResourceHolder<>(resource);
  }

  /**
   * Internal use method to instatiate a failed ResourceHolder from an exception.
   *
   * @param <R> the type of the resource
   * @param <E> the type of the error
   * @param error the error to instantiate the failed ResourceHolder
   * @return a new instance of the failed ResourceHolder with the error
   */
  static <R extends AutoCloseable, E extends Throwable> ResourceHolder<R, E> failure(final E error) {
    return new ResourceHolder<>(error);
  }

  /**
   * Internal use only.
   *
   * @return the possible stored resource
   */
  Optional<R> resource() {
    return value.rightToOptional();
  }

  /**
   * Internal use only.
   *
   * @return the possible propagated error
   */
  Optional<E> error() {
    return value.leftToOptional();
  }

  /**
   * If the resource is present, resolves the value of a throwing operation
   * using a {@link ThrowingFunction} expression which has the previously
   * prepared resource in the argument. The resource is automatically closed
   * after the operation finishes, just like a common try-with-resources
   * statement.
   * <p>
   * Returs a {@link ResolveHandler} which allows to handle the possible error
   * and return a safe value. The returned handler has {@code nothing} if
   * neither the resource nor the error is present.
   * 
   * @param <T> the type of the value returned by the {@code resolver}
   * @param <X> the type of exception the {@code resolver} may throw
   * @param resolver the checked function operation to resolve
   * @return a {@link ResolveHandler} with either the value resolved or the thrown
   *         exception to be handled
   */
  @SuppressWarnings("unchecked")
  public <T, X extends Throwable> ResolveHandler<T, X> resolveClosing(final ThrowingFunction<R, T, X> resolver) {
    return value.unwrap(
      error -> ResolveHandler.ofError((X) error),
      resource -> {
        try (var res = resource) {
          return ResolveHandler.ofSuccess(resolver.apply(res));
        } catch (final Throwable e) { //NOSONAR
          final var error = (X) e;
          return ResolveHandler.ofError(error);
        }
      });
  }

  /**
   * If the resource is present, runs an effect that may throw an exception
   * using a {@link ThrowingConsumer} expression which has the previously
   * prepared resource in the argument. The resource is automatically closed
   * after the operation finishes, just like a common try-with-resources
   * statement.
   * <p>
   * Returning then an {@link EffectHandler} which allows to handle the
   * possible error. The returned handler has {@code nothing} if neither the
   * resource nor the error is present.
   * 
   * @param <X> the type of exception the {@code effect} may throw
   * @param effect the checked consumer operation to execute
   * @return an {@link EffectHandler} with either the thrown exception to be
   *         handled or nothing
   */
  @SuppressWarnings("unchecked")
  public <X extends Throwable> EffectHandler<X> runEffectClosing(final ThrowingConsumer<R, X> effect) {
    return value.unwrap(
      error -> EffectHandler.ofError((X) error),
      resource -> {
        try (var res = resource) {
          effect.accept(res);
          return EffectHandler.empty();
        } catch (final Throwable e) { // NOSONAR
          final var error = (X) e;
          return EffectHandler.ofError(error);
        }
      }
    );
  }
}

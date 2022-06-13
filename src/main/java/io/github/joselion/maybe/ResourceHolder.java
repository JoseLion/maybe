package io.github.joselion.maybe;

import java.util.Optional;

import org.eclipse.jdt.annotation.Nullable;

import io.github.joselion.maybe.util.ConsumerChecked;
import io.github.joselion.maybe.util.FunctionChecked;

/**
 * ResourceHolder is a "middle step" API that allows to resolve or run an effect
 * using a previously passed {@link AutoCloseable} resource. This resource will
 * be automatically closed after the {@code resolve} or the {@code effect}
 * operation is finished.
 * 
 * @author Jose Luis Leon
 * @since v1.3.0
 */
public class ResourceHolder<R extends AutoCloseable, E extends Exception> {

  private final Optional<R> resource;

  private final Optional<E> error;

  private ResourceHolder(final @Nullable R resource) {
    this.resource = Optional.ofNullable(resource);
    this.error = Optional.empty();
  }

  private ResourceHolder(final E error) {
    this.resource = Optional.empty();
    this.error = Optional.of(error);
  }

  /**
   * Internal use method to instatiate a ResourceHolder from a given resource.
   * 
   * @param <R> the type of the resource
   * @param <E> the type of the error
   * @param resource the resource to instantiate the ResourceHolder with
   * @return a new instance of ResourceHolder with the given resource
   */
  static <R extends AutoCloseable, E extends Exception> ResourceHolder<R, E> from(final @Nullable R resource) {
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
  static <R extends AutoCloseable, E extends Exception> ResourceHolder<R, E> failure(final E error) {
    return new ResourceHolder<>(error);
  }

  /**
   * Internal use only.
   *
   * @return the possible stored resource
   */
  Optional<R> resource() {
    return this.resource;
  }

  /**
   * Internal use only.
   *
   * @return the possible propagated error
   */
  Optional<E> error() {
    return this.error;
  }

  /**
   * If the resource is present, resolves the value of a throwing operation
   * using a {@link FunctionChecked} expression which has the previously
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
  public <T, X extends Exception> ResolveHandler<T, X> resolveClosing(final FunctionChecked<R, T, X> resolver) {
    if (this.resource.isPresent()) {
      try (R resArg = this.resource.get()) {
        return ResolveHandler.withSuccess(resolver.apply(resArg));
      } catch (Exception e) {
        final X newError = (X) e;

        return ResolveHandler.withError(newError);
      }
    }

    if (this.error.isPresent()) {
      return ResolveHandler.withError((X) this.error.get());
    }


    return ResolveHandler.withNothing();
  }

  /**
   * If the resource is present, runs an effect that may throw an exception
   * using a {@link ConsumerChecked} expression which has the previously
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
  public <X extends Exception> EffectHandler<X> runEffectClosing(final ConsumerChecked<R, X> effect) {
    if (this.resource.isPresent()) {
      try (R resArg = this.resource.get()) {
        effect.accept(resArg);

        return EffectHandler.withNothing();
      } catch (Exception e) {
        final X newError = (X) e;

        return EffectHandler.withError(newError);
      }
    }

    if (this.error.isPresent()) {
      return EffectHandler.withError((X) this.error.get());
    }

    return EffectHandler.withNothing();
  }
}

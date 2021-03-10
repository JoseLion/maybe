package com.github.joselion.maybe;

import com.github.joselion.maybe.util.ConsumerChecked;
import com.github.joselion.maybe.util.FunctionChecked;

/**
 * ResourceSpec is a "middle step" API that allows to resolve or run an effect
 * usinga previously passed {@link AutoCloseable} resource. This resource will
 * be automatically closed after the {@code resolve} or the {@code runEffect}
 * operation is finished.
 * 
 * @author Jose Luis Leon
 * @since v1.3.0
 */
public class ResourceHolder<R extends AutoCloseable> {

  private final R resource;

  private ResourceHolder(final R resource) {
    this.resource = resource;
  }

  /**
   * Internal use method to instatiate a ResourceSpec froma given resource.
   * 
   * @param <R> the type of the resource
   * @param resource the resource to instantiate the ResourceSpec with
   * @return a new instance of ResourceSpec with the give resource
   */
  protected static <R extends AutoCloseable> ResourceHolder<R> from(final R resource) {
    return new ResourceHolder<>(resource);
  }

  /**
   * Resolves the value of a throwing operation using a {@link FunctionChecked}
   * expression which has the previously prepared resource in the argument. The
   * resource is automatically closed after the operation finishes, just like a
   * common try-with-resources statement.
   * <p>
   * Returs a {@link ResolveHandler} which allows to handle the possible error
   * and return a safe value.
   * 
   * @param <T> the type of the value returned by the {@code resolver}
   * @param <E> the type of exception the {@code resolver} may throw
   * @param resolver the checked function operation to resolve
   * @return a {@link ResolveHandler} with either the value resolved or the thrown
   *         exception to be handled
   */
  public <T, E extends Exception> ResolveHandler<T, E> resolve(final FunctionChecked<R, T, E> resolver) {
    try (R resArg = this.resource) {
      return ResolveHandler.withSuccess(resolver.applyChecked(resArg));
    } catch (Exception e) {
      @SuppressWarnings("unchecked")
      final E error = (E) e;

      return ResolveHandler.withError(error);
    }
  }

  /**
   * Runs an effect that may throw an exception using a {@link ConsumerChecked}
   * expression which has the previously prepared resource in the argument. The
   * resource is automatically closed after the operation finishes, just like a
   * common try-with-resources statement.
   * <p>
   * Returning then an {@link EffectHandler} which allows to handle the possible
   * error.
   * 
   * @param <E> the type of exception the {@code effect} may throw
   * @param effect the checked consumer operation to execute
   * @return an {@link EffectHandler} with either the thrown exception to be
   *         handled or nothing
   */
  public <E extends Exception> EffectHandler<E> runEffect(final ConsumerChecked<R, E> effect) {
    try (R resArg = this.resource) {
      effect.acceptChecked(resArg);
      return EffectHandler.withNothing();
    } catch (Exception e) {
      @SuppressWarnings("unchecked")
      final E error = (E) e;

      return EffectHandler.withError(error);
    }
  }
}

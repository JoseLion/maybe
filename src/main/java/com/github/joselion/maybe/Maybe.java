package com.github.joselion.maybe;

import java.util.Optional;
import java.util.function.Function;

import com.github.joselion.maybe.util.ConsumerChecked;
import com.github.joselion.maybe.util.FunctionChecked;
import com.github.joselion.maybe.util.RunnableChecked;
import com.github.joselion.maybe.util.SupplierChecked;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Maybe is a container object (monad) that may contain a value. Its API allows
 * to process throwing operations in a functional way leveraging
 * {@link java.util.Optional Optional} to unbox the possible contained value.
 * 
 * @param <T> the type of the wrapped value
 * 
 * @author Jose Luis Leon
 * @since v0.1.0
 */
public final class Maybe<T> {

  private final Optional<T> value;

  private Maybe(final @Nullable T value) {
    this.value = Optional.ofNullable(value);
  }

  /**
   * Returns a {@code Maybe} monad wrapping the given value. If the value is
   * {@code null}, it returns a {@link #nothing()}.
   * 
   * @param <T>   the type of the value
   * @param value the value to wrap on the monad
   * @return a {@code Maybe} wrapping the value if it's non-{@code null},
   *         {@link #nothing()} otherwise
   */
  public static <T> Maybe<T> just(final T value) {
    return new Maybe<>(value);
  }

  /**
   * Returns a {@code Maybe} monad with nothing on it. This means the monad does
   * not contains a value because an exception may have occurred.
   * 
   * @param <T> the type of the value
   * @return a {@code Maybe} with nothing
   */
  public static <T> Maybe<T> nothing() {
    return new Maybe<>(null);
  }

  /**
   * Resolves the value of a throwing operation using a {@link SupplierChecked}
   * expression. Returning then a {@link ResolveHandler} which allows to handle
   * the possible error and return a safe value.
   * 
   * @param <T> the type of the value returned by the {@code resolver}
   * @param <E> the type of exception the {@code resolver} may throw
   * @param resolver the checked supplier operation to resolve
   * @return a {@link ResolveHandler} with either the value resolved or the thrown
   *         exception to be handled
   */
  public static <T, E extends Exception> ResolveHandler<T, E> resolve(final SupplierChecked<T, E> resolver) {
    try {
      return ResolveHandler.withSuccess(resolver.getChecked());
    } catch (Exception e) {
      @SuppressWarnings("unchecked")
      final E error = (E) e;

      return ResolveHandler.withError(error);
    }
  }

  /**
   * Runs an effect that may throw an exception using a {@link RunnableChecked}
   * expression. Returning then an {@link EffectHandler} which allows to handle
   * the possible error.
   * 
   * @param <E> the type of exception the {@code effect} may throw
   * @param effect the checked runnable operation to execute
   * @return an {@link EffectHandler} with either the thrown exception to be
   *         handled or nothing
   */
  public static <E extends Exception> EffectHandler<E> runEffect(final RunnableChecked<E> effect) {
    try {
      effect.runChecked();
      return EffectHandler.withNothing();
    } catch (Exception e) {
      @SuppressWarnings("unchecked")
      final E error = (E) e;

      return EffectHandler.withError(error);
    }
  }

  /**
   * Prepare an {@link AutoCloseable} resource to use in a resolver or effect.
   * The resource will be automatically closed after the operation is finished,
   * just like a common try-with-resources statement.
   * 
   * @param <R> the type of the resource. Extends from {@link AutoCloseable}
   * @param resource the {@link AutoCloseable} resource to prepare
   * @return a {@link ResourceHolder} which let's you choose to resolve a value
   *         or run an effect using the prepared resource
   */
  public static <R extends AutoCloseable> ResourceHolder<R> withResource(final R resource) {
    return ResourceHolder.from(resource);
  }

  /**
   * Maps the value of the monad, if present, to another value using the
   * provided {@link Function} mapper. Otherwise, ignores the mapper and
   * returns {@link #nothing()}.
   * 
   * @param <U>    the type the value will be mapped to
   * @param mapper the mapper function
   * @return a {@code Maybe} with the mapped value if present,
   *         {@link #nothing()} otherwise
   */
  public <U> Maybe<U> map(final Function<T, U> mapper) {
    if (value.isPresent()) {
      return Maybe.just(mapper.apply(value.get()));
    }

    return nothing();
  }

  /**
   * Maps the value of the monad, if present, to another value using the
   * provided {@link Function} mapper. Otherwise, ignores the mapper and
   * returns {@link #nothing()}.
   * 
   * This method is similar to {@link #map(Function)}, but the mapping function is
   * one whose result is already a {@code Maybe}, and if invoked, flatMap does not
   * wrap it within an additional {@code Maybe}.
   * 
   * @param <U>    the type the value will be mapped to
   * @param mapper the mapper function
   * @return a {@code Maybe} with the mapped value if present,
   *         {@link #nothing()} otherwise
   */
  public <U> Maybe<U> flatMap(final Function<T, Maybe<U>> mapper) {
    if (value.isPresent()) {
      return mapper.apply(value.get());
    }

    return nothing();
  }

  /**
   * Chain the {@code Maybe} with another resolver, if and only if the previous
   * operation was handled with no errors. The value of the previous operation
   * is passed as argument of the {@link FunctionChecked}.
   * 
   * @param <U>  the type of value returned by the next operation
   * @param <E>  the type of exception the new resolver may throw
   * @param resolver the checked supplier operation to resolve
   * @return a {@link ResolveHandler} with either the value resolved or the thrown
   *         exception to be handled
   */
  public <U, E extends Exception> ResolveHandler<U, E> thenResolve(final FunctionChecked<T, U, E> resolver) {
    if (value.isPresent()) {
      return Maybe.resolve(() -> resolver.applyChecked(value.get()));
    }

    return ResolveHandler.withNothing();
  }

  /**
   * Chain the {@code Maybe} with another effect, if and only if the previous
   * operation was handled with no errors.
   * 
   * @param <E> the type of exception the new effect may throw
   * @param effect the checked runnable operation to execute next
   * @return an {@link EffectHandler} with either the thrown exception to be
   *         handled or nothing
   */
  public <E extends Exception> EffectHandler<E> thenRunEffect(final ConsumerChecked<T, E> effect) {
    if (value.isPresent()) {
      return Maybe.runEffect(() -> effect.acceptChecked(value.get()));
    }

    return EffectHandler.withNothing();
  }

  /**
   * If the value is present in the monad, casts the value to another type. In
   * case of any exception during the cast, a Maybe with {@code nothing} is
   * returned.
   * 
   * @param <U>  the type that the value will be cast to
   * @param type the class instance of the type to cast
   * @return a new {@code Maybe} with the cast value if it can be cast,
   *         {@link #nothing()} otherwise
   */
  public <U> Maybe<U> cast(final Class<U> type) {
    try {
      final T finalValue = this.value.orElseThrow();
      final U newValue = type.cast(finalValue);

      return Maybe.just(newValue);
    } catch (RuntimeException e) {
      return nothing();
    }
  }

  /**
   * Checks if the {@code Maybe} has a value.
   * 
   * @return true if the value is present, false otherwise
   */
  public boolean hasValue() {
    return value.isPresent();
  }

  /**
   * Checks if the {@code Maybe} has nothing. That is, when no value is present.
   * 
   * @return true if the value is NOT present, false otherwise
   */
  public boolean hasNothing() {
    return value.isEmpty();
  }

  /**
   * Safely unbox the value of the monad as an {@link java.util.Optional Optional}
   * which may or may not contain a value.
   * 
   * @return an {@code Optional} with the value of the monad, if preset.
   */
  public Optional<T> toOptional() {
    return value;
  }

  /**
   * Checks if some other object is equal to this {@code Maybe}. For two objects
   * to be equal they both must:
   * <ul>
   *   <li>Be an instance of {@code Maybe}</li>
   *   <li>Contain a values equal to via {@code equals()} comparation</li>
   * </ul>
   * 
   * @param obj an object to be tested for equality
   * @return {@code true} if the other object is "equal to" this object,
   *         {@code false} otherwise
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof Maybe) {
      final Maybe<?> other = (Maybe<?>) obj;
      return other.toOptional().equals(value);
    }

    return false;
  }

  /**
   * Returns the hash code of the value, if present, otherwise {@code 0} (zero)
   * if no value is present.
   * 
   * @return hash code value of the present value or {@code 0} if no value is present
   */
  @Override
  public int hashCode() {
    return value.hashCode();
  }

  /**
   * Returns a non-empty string representation of this {@code Maybe} suitable
   * for debugging. The exact presentation format is unspecified and may vary
   * between implementations and versions.
   * 
   * @return the string representation of this instance
   */
  @Override
  public String toString() {
    return value.isPresent()
      ? String.format("Maybe[%s]", value.get())
      : "Maybe.nothing";
  }
}

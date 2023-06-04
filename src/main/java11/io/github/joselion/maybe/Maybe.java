package io.github.joselion.maybe;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;

import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingFunction;
import io.github.joselion.maybe.util.function.ThrowingRunnable;
import io.github.joselion.maybe.util.function.ThrowingSupplier;

/**
 * Maybe is a monadic wrapper that may contain a value. Its rich API allows to
 * process throwing operations in a functional way leveraging {@link Optional}
 * to unwrap the possible contained value.
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
   * Internal use only.
   *
   * @return the possible wrapped value
   */
  Optional<T> value() {
    return value;
  }

  /**
   * Creates a {@link Maybe} wrapper of the given value. If the value is
   * {@code null}, it returns a {@link #nothing()}.
   * 
   * @param <T> the type of the value
   * @param value the value be wrapped
   * @return a {@code Maybe} wrapping the value if it's non-{@code null},
   *         {@link #nothing()} otherwise
   */
  public static <T> Maybe<T> just(final T value) {
    return new Maybe<>(value);
  }

  /**
   * Creates a {@link Maybe} wrapper with nothing on it. This means the wrapper
   * does not contains a value because an exception may have occurred.
   * 
   * @param <T> the type of the value
   * @return a {@code Maybe} with nothing
   */
  public static <T> Maybe<T> nothing() {
    return new Maybe<>(null);
  }

  /**
   * Creates a {@link Maybe} wrapper of the given value if the optional is not
   * empty. Returns a {@link #nothing()} otherwise.
   * <p>
   * This is a convenience creator that would be equivalent to:
   * <pre>
   *  Maybe.just(opt)
   *    .resolve(Optional::get)
   *    .toMaybe();
   * </pre>
   *
   * @param <T> the type of the value
   * @param value an optional value to create the wrapper from
   * @return a {@code Maybe} wrapping the value if it's not empty.
   *         {@link #nothing()} otherwise
   */
  public static <T> Maybe<T> fromOptional(final Optional<T> value) {
    return new Maybe<>(value.orElse(null));
  }

  /**
   * Resolves the value of a throwing operation using a {@link ThrowingSupplier}
   * expression. Returning then a {@link ResolveHandler} which allows to handle
   * the possible error and return a safe value.
   * 
   * @param <T> the type of the value returned by the {@code resolver}
   * @param <E> the type of exception the {@code resolver} may throw
   * @param resolver the checked supplier operation to resolve
   * @return a {@link ResolveHandler} with either the value resolved or the thrown
   *         exception to be handled
   */
  public static <T, E extends Throwable> ResolveHandler<T, E> fromResolver(final ThrowingSupplier<T, E> resolver) {
    try {
      return ResolveHandler.ofSuccess(resolver.get());
    } catch (Throwable e) { // NOSONAR
      @SuppressWarnings("unchecked")
      final var error = (E) e;
      return ResolveHandler.ofError(error);
    }
  }

  /**
   * Runs an effect that may throw an exception using a {@link ThrowingRunnable}
   * expression. Returning then an {@link EffectHandler} which allows to handle
   * the possible error.
   * 
   * @param <E> the type of exception the {@code effect} may throw
   * @param effect the checked runnable operation to execute
   * @return an {@link EffectHandler} with either the thrown exception to be
   *         handled or nothing
   */
  public static <E extends Throwable> EffectHandler<E> fromEffect(final ThrowingRunnable<E> effect) {
    try {
      effect.run();
      return EffectHandler.empty();
    } catch (Throwable e) { // NOSONAR
      @SuppressWarnings("unchecked")
      final var error = (E) e;
      return EffectHandler.ofError(error);
    }
  }

  /**
   * Convenience partial application of a {@code resolver}. This method creates
   * a function that receives an {@code S} value which can be used to produce a
   * {@link ResolveHandler} once applied. This is specially useful when we want
   * to create a {@link Maybe} from a callback argument, like on a
   * {@link Optional#map(Function)} for instance.
   * <p>
   * In other words, the following code
   * <pre>
   *  Optional.of(value)
   *    .map(str -> Maybe.fromResolver(() -> decode(str)));
   * </pre>
   * Is equivalent to
   * <pre>
   *  Optional.of(value)
   *    .map(Maybe.partialResolver(this::decode));
   * </pre>
   *
   * @param <S> the type of the value the returned function receives
   * @param <T> the type of the value to be resolved
   * @param <E> the type of the error the resolver may throw
   * @param resolver a checked function that receives an {@code S} value and
   *                 returns a {@code T} value
   * @return a partially applied {@link ResolveHandler}. This means, a function
   *         that receives an {@code S} value, and produces a {@code ResolveHandler<T, E>}
   */
  public static <S, T, E extends Throwable> Function<S, ResolveHandler<T, E>> partialResolver(
    final ThrowingFunction<S, T, E> resolver
  ) {
    return value -> Maybe.fromResolver(() -> resolver.apply(value));
  }

  /**
   * Convenience partial application of an {@code effect}. This method creates
   * a function that receives an {@code S} value which can be used to produce
   * an {@link EffectHandler} once applied. This is specially useful when we
   * want to create a {@link Maybe} from a callback argument, like on a
   * {@link Optional#map(Function)} for instance.
   * <p>
   * In other words, the following code
   * <pre>
   *  Optional.of(value)
   *    .map(msg -> Maybe.fromEffect(() -> sendMessage(msg)));
   * </pre>
   * Is equivalent to
   * <pre>
   *  Optional.of(value)
   *    .map(Maybe.partialEffect(this::sendMessage));
   * </pre>
   *
   * @param <S> the type of the value the returned function receives
   * @param <E> the type of the error the resolver may throw
   * @param effect a checked consumer that receives an {@code S} value
   * @return a partially applied {@link EffectHandler}. This means, a function
   *         that receives an {@code S} value, and produces an {@code EffectHandler<E>}
   */
  public static <S, E extends Throwable> Function<S, EffectHandler<E>> partialEffect(
    final ThrowingConsumer<S, E> effect
  ) {
    return value -> Maybe.fromEffect(() -> effect.accept(value));
  }

  /**
   * Prepare an {@link AutoCloseable} resource to use in a resolver or effect.
   * The resource will be automatically closed after the operation is finished,
   * just like a common try-with-resources statement.
   * 
   * @param <R> the type of the resource. Extends from {@link AutoCloseable}
   * @param <E> the type of error the holder may have
   * @param resource the {@link AutoCloseable} resource to prepare
   * @return a {@link ResourceHolder} which let's you choose to resolve a value
   *         or run an effect using the prepared resource
   */
  public static <R extends AutoCloseable, E extends Throwable> ResourceHolder<R, E> withResource(final R resource) {
    return ResourceHolder.from(resource);
  }

  /**
   * If present, maps the value to another using the provided mapper function.
   * Otherwise, ignores the mapper and returns {@link #nothing()}.
   * 
   * @param <U> the type the value will be mapped to
   * @param mapper the mapper function
   * @return a {@code Maybe} with the mapped value if present,
   *         {@link #nothing()} otherwise
   */
  public <U> Maybe<U> map(final Function<T, U> mapper) {
    return value.map(mapper)
      .map(Maybe::just)
      .orElseGet(Maybe::nothing);
  }

  /**
   * If present, maps the value to another using the provided mapper function.
   * Otherwise, ignores the mapper and returns {@link #nothing()}.
   * 
   * This method is similar to {@link #map(Function)}, but the mapping function is
   * one whose result is already a {@code Maybe}, and if invoked, flatMap does not
   * wrap it within an additional {@code Maybe}.
   * 
   * @param <U> the type the value will be mapped to
   * @param mapper the mapper function
   * @return a {@code Maybe} with the mapped value if present,
   *         {@link #nothing()} otherwise
   */
  public <U> Maybe<U> flatMap(final Function<T, Maybe<U>> mapper) {
    return value.map(mapper)
      .orElseGet(Maybe::nothing);
  }

  /**
   * Chain the {@code Maybe} with another resolver, if and only if the previous
   * operation was handled with no errors. The value of the previous operation
   * is passed as argument of the {@link ThrowingFunction}.
   * 
   * @param <U> the type of value returned by the next operation
   * @param <E> the type of exception the new resolver may throw
   * @param resolver a checked function that receives the current value and
   *                 resolves another
   * @return a {@link ResolveHandler} with either the resolved value, or the
   *         thrown exception to be handled
   */
  @SuppressWarnings("unchecked")
  public <U, E extends Throwable> ResolveHandler<U, E> resolve(final ThrowingFunction<T, U, E> resolver) {
    try {
      return value
        .map(Maybe.partialResolver(resolver))
        .orElseThrow();
    } catch (final NoSuchElementException error) {
      return ResolveHandler.ofError((E) error);
    }
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
  @SuppressWarnings("unchecked")
  public <E extends Throwable> EffectHandler<E> runEffect(final ThrowingConsumer<T, E> effect) {
    try {
      return value
        .map(Maybe.partialEffect(effect))
        .orElseThrow();
    } catch (final NoSuchElementException error) {
      return EffectHandler.ofError((E) error);
    }
  }

  /**
   * If the value is present, cast the value to another type. In case of an
   * exception during the cast, a Maybe with {@link #nothing()} is returned.
   * 
   * @param <U> the type that the value will be cast to
   * @param type the class instance of the type to cast
   * @return a new {@code Maybe} with the cast value if it can be cast,
   *         {@link #nothing()} otherwise
   */
  public <U> Maybe<U> cast(final Class<U> type) {
    try {
      final var newValue = type.cast(value.orElseThrow());
      return Maybe.just(newValue);
    } catch (final ClassCastException error) {
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
   * Safely unbox the value as an {@link Optional} which may or may not contain
   * a value.
   * 
   * @return an optional with the value, if preset. An empty optional otherwise
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
      final var other = Maybe.class.cast(obj);
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
    return value
      .map(Object::toString)
      .map(x -> String.format(x, "Maybe[%s]"))
      .orElse("Maybe.nothing");
  }
}

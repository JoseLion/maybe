package io.github.joselion.maybe;

import java.io.Closeable;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;

import io.github.joselion.maybe.helpers.Commons;
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
    return this.value;
  }

  /**
   * Creates a {@link Maybe} wrapper of the given value. If the value is
   * {@code null}, it returns a {@link #empty()}.
   *
   * @param <T> the type of the value
   * @param value the value be wrapped
   * @return a {@code Maybe} wrapping the value if it's non-{@code null},
   *         {@link #empty()} otherwise
   */
  public static <T> Maybe<T> of(final @Nullable T value) {
    return new Maybe<>(value);
  }

  /**
   * Creates a {@link Maybe} wrapper of the given optional if not empty.
   * Returns {@link #empty()} if the optional is empty or {@code null}.
   *
   * @param <T> the type of the value
   * @param value an optional value to create the wrapper from
   * @return a {@code Maybe} wrapping the value if it's not empty.
   *         {@link #empty()} otherwise
   * @apiNote It is not convenient to create a {@code Maybe} wrapping an
   *          {@code Optional}. It'll be hard to use the value later on, and it
   *          defies the purpose of using {@code Maybe} in the first place
   *          (Maybe is like Optional, but for handling exceptions). But if you
   *          really want to do that for some reason, here's a workaround where
   *          {@code value} can be an {@code Optional<T>} or not:
   *          <pre>{@code Maybe.of(value).map(Optional::of);}</pre>
   */
  public static <T> Maybe<T> of(final @Nullable Optional<T> value) { // NOSONAR
    if (value != null) { // NOSONAR
      return value
        .map(Maybe::new)
        .orElseGet(Maybe::empty);
    }

    return new Maybe<>(null);
  }

  /**
   * Creates an empty {@link Maybe} instance.
   *
   * @param <T> the type of the value
   * @return an empty {@code Maybe}
   */
  public static <T> Maybe<T> empty() {
    return Maybe.of(null);
  }

  /**
   * Solves the value of a throwing operation using a {@link ThrowingSupplier}
   * expression. Returning then a {@link SolveHandler} which allows to handle
   * the possible error and return a safe value.
   *
   * @param <T> the type of the value returned by the {@code solver}
   * @param <E> the type of exception the {@code solver} may throw
   * @param solver the checked supplier operation to solve
   * @return a {@link SolveHandler} with either the value solved or the thrown
   *         exception to be handled
   */
  public static <T, E extends Throwable> SolveHandler<T, E> from(
    final ThrowingSupplier<? extends T, ? extends E> solver
  ) {
    try {
      return SolveHandler.from(solver.get());
    } catch (Throwable e) { // NOSONAR
      final var error = Commons.<E>cast(e);
      return SolveHandler.failure(error);
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
   *         handled or empty
   */
  public static <E extends Throwable> EffectHandler<E> from(final ThrowingRunnable<? extends E> effect) {
    try {
      effect.run();
      return EffectHandler.empty();
    } catch (Throwable e) { // NOSONAR
      final var error = Commons.<E>cast(e);
      return EffectHandler.failure(error);
    }
  }

  /**
   * Convenience partial application of a {@code solver}. This method creates
   * a function that receives an {@code S} value which can be used to produce a
   * {@link SolveHandler} once applied. This is specially useful when we want
   * to create a {@link Maybe} from a callback argument, like on a
   * {@link Optional#map(Function)} for instance.
   *
   * <p>In other words, the following code:
   * <pre>{@code
   * Optional
   *  .of(value)
   *  .map(str -> Maybe.from(() -> this.decode(str)));
   * }</pre>
   *
   * <p>Is equivalent to:
   * <pre>{@code
   *  Optional
   *   .of(value)
   *   .map(Maybe.partial(this::decode));
   * }</pre>
   *
   * @param <S> the type of the value the returned function receives
   * @param <T> the type of the value to be solved
   * @param <E> the type of the error the solver may throw
   * @param solver a checked function that receives an {@code S} value and
   *               returns a {@code T} value
   * @return a partially applied {@link SolveHandler}. This means, a function
   *         that receives an {@code S} value, and produces a {@code SolveHandler<T, E>}
   */
  public static <S, T, E extends Throwable> Function<S, SolveHandler<T, E>> partial(
    final ThrowingFunction<? super S, ? extends T, ? extends E> solver
  ) {
    return value -> Maybe.from(() -> solver.apply(value));
  }

  /**
   * Convenience partial application of an {@code effect}. This method creates
   * a function that receives an {@code S} value which can be used to produce
   * an {@link EffectHandler} once applied. This is specially useful when we
   * want to create a {@link Maybe} from a callback argument, like on a
   * {@link Optional#map(Function)} for instance.
   *
   * <p>In other words, the following code:
   * <pre>{@code
   * Optional
   *  .of(value)
   *  .map(msg -> Maybe.from(() -> this.sendMessage(msg)));
   * }</pre>
   *
   * <p>Is equivalent to:
   * <pre>{@code
   * Optional
   *  .of(value)
   *  .map(Maybe.partial(this::sendMessage));
   * }</pre>
   *
   * @param <S> the type of the value the returned function receives
   * @param <E> the type of the error the effect may throw
   * @param effect a checked consumer that receives an {@code S} value
   * @return a partially applied {@link EffectHandler}. This means, a function
   *         that receives an {@code S} value, and produces an {@code EffectHandler<E>}
   */
  public static <S, E extends Throwable> Function<S, EffectHandler<E>> partial(
    final ThrowingConsumer<? super S, ? extends E> effect
  ) {
    return value -> Maybe.from(() -> effect.accept(value));
  }

  /**
   * Prepare an {@link AutoCloseable} resource to use in a solver or effect.
   * The resource will be automatically closed after the operation is finished,
   * just like a common try-with-resources statement.
   *
   * @param <R> the type of the resource. Extends from {@link AutoCloseable}
   * @param <E> the type of error the holder may have
   * @param resource the {@link AutoCloseable} resource to prepare
   * @return a {@link CloseableHandler} which let's you choose to solve a value
   *         or run an effect using the prepared resource
   */
  public static <R extends AutoCloseable, E extends Throwable> CloseableHandler<R, E> withResource(final R resource) {
    return CloseableHandler.from(resource);
  }

  /**
   * Prepare an {@link AutoCloseable} resource to use in a solver or effect,
   * using a {@link ThrowingSupplier}. Any exception thrown by the supplier
   * will be propageted to the {@link CloseableHandler}. The resource will be
   * automatically closed after the operation is finished, just like a common
   * try-with-resources statement.
   *
   * @param <R> the type of the resource. Extends from {@link AutoCloseable}
   * @param <E> the type of error the holder may have
   * @param supplier the throwing supplier o the {@link AutoCloseable} resource
   * @return a {@link CloseableHandler} which let's you choose to solve a value
   *         or run an effect using the prepared resource
   */
  public static <R extends Closeable, E extends Throwable> CloseableHandler<R, E> solveResource(
    final ThrowingSupplier<? extends R, ? extends E> supplier
  ) {
    return Maybe
      .from(supplier)
      .map(CloseableHandler::<R, E>from)
      .orElse(x -> CloseableHandler.failure(Commons.cast(x)));
  }

  /**
   * If present, maps the value to another using the provided mapper function.
   * Otherwise, ignores the mapper and returns {@link #empty()}.
   *
   * @param <U> the type the value will be mapped to
   * @param mapper the mapper function
   * @return a {@code Maybe} with the mapped value if present,
   *         {@link #empty()} otherwise
   */
  public <U> Maybe<U> map(final Function<? super T, ? extends U> mapper) {
    return this.value
      .map(mapper)
      .map(Maybe::<U>of)
      .orElseGet(Maybe::empty);
  }

  /**
   * If present, maps the value to another using the provided mapper function.
   * Otherwise, ignores the mapper and returns {@link #empty()}.
   * 
   * <p>This method is similar to {@link #map(Function)}, but the mapping
   * function is one whose result is already a {@code Maybe}, and if invoked,
   * flatMap does not wrap it within an additional {@code Maybe}.
   *
   * @param <U> the type the value will be mapped to
   * @param mapper the mapper function
   * @return a {@code Maybe} with the mapped value if present,
   *         {@link #empty()} otherwise
   */
  public <U> Maybe<U> flatMap(final Function<? super T, Maybe<? extends U>> mapper) {
    return this.value
      .map(mapper)
      .map(Commons::<Maybe<U>>cast)
      .orElseGet(Maybe::empty);
  }

  /**
   * Chain the {@code Maybe} with another solver, if and only if the previous
   * operation was handled with no errors. The value of the previous operation
   * is passed as argument of the {@link ThrowingFunction}.
   *
   * @param <U> the type of value returned by the next operation
   * @param <E> the type of exception the new solver may throw
   * @param solver a checked function that receives the current value and
   *                 solves another
   * @return a {@link SolveHandler} with either the solved value, or the
   *         thrown exception to be handled
   */
  public <U, E extends Throwable> SolveHandler<U, E> solve(
    final ThrowingFunction<? super T, ? extends U, ? extends E> solver
  ) {
    try {
      return this.value
        .map(Maybe.<T, U, E>partial(solver))
        .orElseThrow();
    } catch (final NoSuchElementException e) {
      final var error = Commons.<E>cast(e);
      return SolveHandler.failure(error);
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
  public <E extends Throwable> EffectHandler<E> effect(final ThrowingConsumer<? super T, ? extends E> effect) {
    try {
      return this.value
        .map(Maybe.<T, E>partial(effect))
        .orElseThrow();
    } catch (final NoSuchElementException e) {
      final var error = Commons.<E>cast(e);
      return EffectHandler.failure(error);
    }
  }

  /**
   * If the value is present, casts the value to the provided {@code type}
   * class. If the value is not assignable to {@code type}, returns a handler
   * with a {@link ClassCastException} error.
   *
   * @param <U> the type of the cast value
   * @param type the class instance of the type to cast
   * @return a handler with either the cast value or a ClassCastException error
   */
  public <U> SolveHandler<U, ClassCastException> cast(final Class<U> type) {
    return this.solve(type::cast);
  }

  /**
   * If the value is present, casts the value to the provided {@code type}
   * class. If the value is not assignable to {@code type}, maps the error with
   * the provided {@code onError} function, which receives the produced
   * {@link ClassCastException} on its argument.
   *
   * @param <U> the type of the cast value
   * @param <X> the type of the mapped exception
   * @param type the class instance of the type to cast
   * @param onError a function to map the error in case of failure
   * @return a handler with either the cast value or the mapped error
   */
  public <U, X extends Throwable> SolveHandler<U, X> cast(
    final Class<U> type,
    final Function<Throwable, ? extends X> onError
  ) {
    return this
      .solve(type::cast)
      .mapError(onError);
  }

  /**
   * Checks if the {@code Maybe} has a value.
   *
   * @return true if the value is present, false otherwise
   */
  public boolean hasValue() {
    return this.value.isPresent();
  }

  /**
   * Checks if the {@code Maybe} is empty. That is, when no value is present.
   *
   * @return true if the value is not present, false otherwise
   */
  public boolean isEmpty() {
    return this.value.isEmpty();
  }

  /**
   * Safely unbox the value as an {@link Optional} which may or may not contain
   * a value.
   *
   * @return an optional with the value, if preset. An empty optional otherwise
   */
  public Optional<T> toOptional() {
    return this.value;
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

    if (obj instanceof Maybe<?>) {
      final var other = (Maybe<?>) obj;
      return other.toOptional().equals(this.value);
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
    return this.value.hashCode();
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
    return this.value
      .map(Object::toString)
      .map(x -> String.format("Maybe[%s]", x))
      .orElse("Maybe.empty");
  }
}

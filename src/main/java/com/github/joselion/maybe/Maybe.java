package com.github.joselion.maybe;

import java.util.Optional;
import java.util.function.Function;

import com.github.joselion.maybe.util.FunctionChecked;
import com.github.joselion.maybe.util.SupplierChecked;

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
public class Maybe<T> {

  private final Optional<T> success;

  private Maybe(final T success) {
    this.success = Optional.ofNullable(success);
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
   * not contain a success value because an exception may have occurred.
   * 
   * @param <T> the type of the value
   * @return a {@code Maybe} with nothing
   */
  public static <T> Maybe<T> nothing() {
    return new Maybe<>(null);
  }

  /**
   * Resolves the value of a throwing operation using a {@link SupplierChecked}
   * expression. Returning then a {@link ResolveHandler} whic allows to handle the
   * possible error and return a safe value.
   * 
   * @param <T>       the type of the value returned by the {@code operation}
   * @param <E>       the typew of exception the {@code operation} may throw
   * @param operation the checked supplier operation to resolve
   * @return a {@link ResolveHandler} with either the value resolved or the thrown
   *         exception to be handled
   */
  public static <T, E extends Exception> ResolveHandler<T, E> resolve(final SupplierChecked<T, E> operation) {
    try {
      return ResolveHandler.withSuccess(operation.getChecked());
    } catch (Exception e) {
      @SuppressWarnings("unchecked")
      final E error = (E) e;

      return ResolveHandler.withError(error);
    }
  }

  /**
   * Maps the current success value of the monad to another value using the
   * provided {@link Function} mapper.
   * 
   * @param <U>    the type the success value will be mapped to
   * @param mapper the mapper function
   * @return a {@code Maybe} with the mapped value if success is present,
   *         {@link #nothing()} otherwise
   */
  public <U> Maybe<U> map(final Function<T, U> mapper) {
    if (success.isPresent()) {
      return Maybe.just(mapper.apply(success.get()));
    }

    return nothing();
  }

  /**
   * Maps the current success value of the monad to another value using the
   * provided {@link Function} mapper.
   * 
   * This method is similar to {@link #map(Function)}, but the mapping function is
   * one whose result is already a {@code Maybe}, and if invoked, flatMap does not
   * wrap it within an additional {@code Maybe}.
   * 
   * @param <U>    the type the success value will be mapped to
   * @param mapper the mapper function
   * @return a {@code Maybe} with the mapped value if success is present,
   *         {@link #nothing()} otherwise
   */
  public <U> Maybe<U> flatMap(final Function<T, Maybe<U>> mapper) {
    if (success.isPresent()) {
      return mapper.apply(success.get());
    }

    return nothing();
  }

  /**
   * Chains the current Maybe to another operation which is executed only if a
   * value is present in the Monad. The value is passed to a
   * {@link FunctionChecked} to be used in the next operation. If there's no value
   * in the monad, a Maybe with {@code nothing} is returned.
   * 
   * @param <U>  the type of the value returned by the next operation
   * @param <X>  the type of the exception the new operation may throw
   * @param next the {@link FunctionChecked} operation applied next
   * @return a {@link ResolveHandler} with either the value resolved or the thrown
   *         exception to be handled
   */
  public <U, X extends Exception> ResolveHandler<U, X> thenResolve(final FunctionChecked<T, U, X> next) {
    if (success.isPresent()) {
      return Maybe.resolve(() -> next.applyChecked(success.get()));
    }

    return ResolveHandler.withNothing();
  }

  /**
   * If the value is present in the monad, casts the value to another type. In
   * case of any exception during the cast, a Maybe with {@code nothing} is
   * returned.
   * 
   * @param <U>  the type that the value will be cast to
   * @param type a class instance of the type to cast
   * @return a new {@code Maybe} with the cast value if it can be cast,
   *         {@link #nothing()} otherwise
   */
  public <U> Maybe<U> cast(Class<U> type) {
    try {
      final T value = success.orElseThrow();
      final U newValue = type.cast(value);

      return Maybe.just(newValue);
    } catch (RuntimeException e) {
      return nothing();
    }
  }

  /**
   * Checks if the {@code Maybe} has a success value.
   * 
   * @return true if a success value is present, false otherwise
   */
  public boolean hasSuccess() {
    return success.isPresent();
  }

  /**
   * Checks if the {@code Maybe} has nothing. That is, when no success value and
   * failure exception are present.
   * 
   * @return true if both success value and failure exception are not present,
   *         false otherwise
   */
  public boolean hasNothing() {
    return success.isEmpty();
  }

  /**
   * Safely unbox the value of the monad as an {@link java.util.Optional Optional}
   * which may or may not contain a value.
   * 
   * @return an {@code Optional} with the value of the monad, if preset.
   */
  public Optional<T> toOptional() {
    return success;
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
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof Maybe) {
      final Maybe<?> other = (Maybe<?>) obj;
      return other.toOptional().equals(success);
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
    return success.hashCode();
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
    return success.isPresent()
      ? String.format("Maybe[%s]", success.get())
      : "Maybe.nothing";
  }
}

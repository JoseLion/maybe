package com.github.joselion.maybe;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.joselion.maybe.exceptions.MaybeFailedException;
import com.github.joselion.maybe.util.ConsumerChecked;
import com.github.joselion.maybe.util.FunctionChecked;
import com.github.joselion.maybe.util.RunnableChecked;
import com.github.joselion.maybe.util.SupplierChecked;

/**
 * Maybe is a container object (monad) that may contain a value, a
 * {@link Throwable}, or neither. Its API allow us to process throwing operation
 * in a functional way with the posibility of safely or unsafely unboxing the
 * result of the operations.
 * 
 * @param <T> the type of the wrapped value
 * 
 * @author Jose Luis Leon
 * @since v0.1.0
 */
public class Maybe<T> {

  private Optional<T> success;

  private <E extends Exception> Maybe(final T success) {
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
   * expression. Returning then a {@code Maybe} monad with either the success
   * value or {@link #nothing()}.
   * 
   * @param <T> the type of the value returned by the passed {@code operation}
   * @param operation the checked supplier operation to resolve
   * @return a {@code Maybe} with either the success resolved value or {@link #nothing()}
   */
  public static <T> Maybe<T> resolve(final SupplierChecked<T, ? extends Exception> operation) {
    try {
      return Maybe.just(operation.getChecked());
    } catch (Exception e) {

      return Maybe.nothing();
    }
  }

  /**
   * Executes a throwing operation using a {@link RunnableChecked} expression.
   * Returning then a {@code Maybe} monad with {@link #nothing()}
   * 
   * @param operation the throwing operation runnable
   * @return a {@code Maybe} with {@link #nothing()}
   */
  public static Maybe<Void> execute(final RunnableChecked<? extends Exception> operation) {
    try {
      operation.runChecked();
      return Maybe.nothing();
    } catch (Exception e) {
      return Maybe.nothing();
    }
  }

  /**
   * Unsafely unbox the value of the {@code Maybe} monad. Returns the value if
   * present. Throws an unchecked exception otherwise.
   * 
   * @return the success value of the {@code Maybe} monad if present
   * @throws MaybeFailedException if the success value is not present
   */
  public T getUnsafe() {
    return success.orElseThrow(MaybeFailedException::new);
  }

  /**
   * Returns the success value if present, the {@code other} value otherwise.
   * 
   * @param other the value to return if the {@code Maybe} operation fails.
   * @return the success value, if present, {@code other} otherwise
   */
  public T orElse(final T other) {
    return success.orElse(other);
  }

  /**
   * Returns the success value if present. Otherwise, the result resolved by the
   * {@code otherFn} supplier.
   * 
   * @param otherFn the supplier to get the {@code other} value.
   * @return the success value, if present, the result of applying the
   *         {@code otherFn} otherwise
   */
  public T orElse(final Supplier<T> otherFn) {
    return success.orElseGet(otherFn);
  }

  /**
   * Returns the success value if present. Otherwise, 
   * throws a new unchecked exception.
   * 
   * @param newException an unchecked exception to throw if the
   *                     success value is not present.
   * @return the success value if present
   */
  public T orThrow(final RuntimeException newException) {
    return success.orElseThrow(() -> newException);
  }

  /**
   * Returns the success value if present. Otherwise, throws a new
   * unchecked resolved by the {@code exceptionSupplier} function.
   * 
   * @param exceptionSupplier an unchecked exception supplier to throw
   *                          if the success value is not present.
   * @return the success value if present
   */
  public T orThrow(final Supplier<? extends RuntimeException> exceptionSupplier) {
    return success.orElseThrow(exceptionSupplier);
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

    return Maybe.nothing();
  }

  /**
   * Maps the current success value of the monad to another value using the
   * provided {@link Function} mapper.
   * 
   * This method is similar to {@link #map(Function)}, but the mapping function is
   * one whose result is already a {@code Maybe}, and if invoked, flatMap does not
   * wrap it within an additional {@code Maybe}.
   * 
   * @param <U> the type the success value will be mapped to
   * @param mapper the mapper function
   * @return a {@code Maybe} with the mapped value if success is present,
   *         {@link #nothing()} otherwise
   */
  public <U> Maybe<U> flatMap(final Function<T, Maybe<U>> mapper) {
    if (success.isPresent()) {
      return mapper.apply(success.get());
    }

    return Maybe.nothing();
  }

  /**
   * Resolves the value of another throwing operation. The value of the previous
   * {@code maybe} is passed to the checked function.
   * 
   * @param <U> the type of the value returned by the passes operation
   * @param thenOperation the {@link FunctionChecked} throwing operation
   * @return another {@code Maybe} with other success value if resolved
   */
  public <U> Maybe<U> thenResolve(final FunctionChecked<T, U, ? extends Exception> thenOperation) {
    if (success.isPresent()) {
      return Maybe.resolve(() -> thenOperation.applyChecked(success.get()));
    }

    return Maybe.nothing();
  }

  /**
   * Executes another throwing operation. The value of the previous {@code maybe} is
   * passed to the checked consumer.
   * 
   * @param thenOperation the {@link ConsumerChecked} throwing operation
   * @return another {@code Maybe} with {@link #nothing()}
   */
  public Maybe<Void> thenExecute(final ConsumerChecked<T, ? extends Exception> thenOperation) {
    if (success.isPresent()) {
      return Maybe.execute(() -> thenOperation.acceptChecked(success.get()));
    }

    return Maybe.nothing();
  }

  /**
   * Cast the success value of the monad to the passed {@code Class} type, if present.
   * This operation may throw an unchecked @{link java.lang.ClassCastException ClassCastException}
   * if the object is not null and is not assignable to the type {@code U}.
   * 
   * @param <U> the type the success value will be cast to
   * @param type the class instance of the type to cast
   * @return a new {@code Maybe} with the cast success value
   *         if present, {@link #nothing()} otherwise
   */
  public <U> Maybe<U> cast(Class<U> type) {
    if (success.isPresent()) {
      return Maybe.just(type.cast(success.get()));
    }

    return Maybe.nothing();
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
   * Checks if the {@code Maybe} has nothing. That is, when no success
   * value and failure exception are present.
   * 
   * @return true if both success value and failure exception are not
   *         present, false otherwise
   */
  public boolean hasNothing() {
    return success.isEmpty();
  }
}

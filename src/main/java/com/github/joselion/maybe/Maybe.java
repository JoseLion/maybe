package com.github.joselion.maybe;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import com.github.joselion.maybe.exceptions.MaybeFailureException;
import com.github.joselion.maybe.util.BiConsumerChecked;
import com.github.joselion.maybe.util.BiFunctionChecked;
import com.github.joselion.maybe.util.RunnableChecked;
import com.github.joselion.maybe.util.SupplierChecked;

/**
 * Maybe is a container object (monad) that may contain a value, a
 * {@link Throwable}, or neither. Its API allow us to process throwing operation
 * in a functional way with the posibility of safely or unsafely unboxing the
 * result of the operations.
 * 
 * @param <T> the type of the wrapped value
 * @param <E> the type of the wrapped exception
 * 
 * @author Jose Luis Leon
 * @since v0.1.0
 */
public class Maybe<T, E extends Exception> {

  private Optional<T> success;

  private Optional<E> failure;

  private Maybe(final T success, final E failure) {
    this.success = Optional.ofNullable(success);
    this.failure = Optional.ofNullable(failure);
  }

  /**
   * Returns a {@code Maybe} monad wrapping the given value. If the value is
   * {@code null}, it returns {@link #nothing()}.
   * 
   * @param <T>   the type of the value
   * @param <E>   the type of the exception
   * @param value the value to wrap on the monad
   * @return a {@code Maybe} wrapping the value if it's non-{@code null},
   *         {@link #nothing()} otherwise
   */
  public static <T, E extends Exception> Maybe<T, E> just(final T value) {
    return new Maybe<>(value, null);
  }

  /**
   * Returns a {@code Maybe} monad with the given failure exception. If the
   * exception is {@code null}, it returns {@link #nothing()}.
   * 
   * @param <T>       the type of the value
   * @param <E>       the type of exception
   * @param exception the expection the monad will fail with
   * @return a {@code Maybe} with the given failure exception if it's
   *         non-{@code null}, {@link #nothing()} otherwise
   */
  public static <T, E extends Exception> Maybe<T, E> fail(final E exception) {
    return new Maybe<>(null, exception);
  }

  /**
   * Returns a {@code Maybe} monad with nothing on it. This means the monad does
   * not contain a success value, neither a failure exception.
   * 
   * @param <T> the type of the value
   * @param <E> the type of the exception
   * @return a {@code Maybe} with nothing
   */
  public static <T, E extends Exception> Maybe<T, E> nothing() {
    return new Maybe<>(null, null);
  }

  /**
   * Resolves the value of a throwing operation using a {@link SupplierChecked}
   * expression. Returning then a {@code Maybe} monad with either the success
   * value, or the failure exception {@code E}.
   * 
   * @param <T>       the type of the value returned by the
   *                  {@link SupplierChecked} expression
   * @param <E>       the type of the exception that the {@link SupplierChecked}
   *                  may throw
   * @param operation the throwing operation supplier
   * @return a {@code Maybe} with either the success value, or the failure
   *         exception
   */
  public static <T, E extends Exception> Maybe<T, E> resolve(final SupplierChecked<T, E> operation) {
    try {
      return Maybe.just(operation.getChecked());
    } catch (final Exception e) {
      @SuppressWarnings("unchecked")
      final E exception = (E) e;

      return Maybe.fail(exception);
    }
  }

  /**
   * Executes a throwing operation using a {@link RunnableChecked} expression.
   * Returning then a {@code Maybe} monad with either {@link #nothing()}, or the
   * failure exception {@code E}.
   * 
   * @param <E>       the type of the exception that the {@link RunnableChecked}
   *                  may throw
   * @param operation the throwing operation runnable
   * @return a {@code Maybe} with either {@link #nothing()}, or the failure
   *         exception
   */
  public static <E extends Exception> Maybe<Void, E> execute(final RunnableChecked<E> operation) {
    try {
      operation.runChecked();

      return Maybe.nothing();
    } catch (Exception e) {
      @SuppressWarnings("unchecked")
      final E exception = (E) e;

      return Maybe.fail(exception);
    }
  }

  /**
   * Unsafely unbox the value of the {@code Maybe} monad. Returns the value if
   * present. Throws an unchecked exception otherwise.
   * 
   * @return the success value of the {@code Maybe} monad if present
   * @throws MaybeFailureException if the success value is not present and a
   *                               failure exception exists.
   *                               {@link NoSuchElementException} exception
   *                               otherwise
   */
  public T getUnsafe() {
    return success.orElseThrow(() -> new MaybeFailureException(failure.orElseThrow()));
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
   * Returns the success value if present. Otherwise, the result returned by the
   * {@code otherFn} function, which makes the exception available in the
   * argument, if present.
   * 
   * @param otherFn the function to get the {@code other} value. The exception is
   *                injected as {@code Optional} to the function argument.
   * @return the success value, if present, the result of applying the
   *         {@code otherFn} otherwise
   */
  public T orElse(final Function<Optional<E>, T> otherFn) {
    return success.orElse(otherFn.apply(failure));
  }

  /**
   * Returns the success value if present. Otherwise, maps the
   * current failure exception with the {@code exceptionMapper}
   * provided and throws a new unchecked exception.
   * 
   * @param exceptionMapper a mapper function to provide the new exception
   * @return                the success value if present
   */
  public T orThrow(final Function<E, ? extends RuntimeException> exceptionMapper) {
    return success.orElseThrow(() -> exceptionMapper.apply(failure.get()));
  }

  /**
   * Maps the current success value of the monad to another value using the
   * provided {@link Function} mapper. If the monda has a failure instead, the
   * monad with the same exception is returned.
   * 
   * @param <U>    the type the success value will be mapped to
   * @param mapper the mapper function
   * @return a {@code Maybe} with the mapped value if success is present, with the
   *         same exception if failed, {@link #nothing()} otherwise
   */
  public <U> Maybe<U, E> map(final Function<T, U> mapper) {
    if (success.isPresent()) {
      return Maybe.just(mapper.apply(success.get()));
    }

    if (failure.isPresent()) {
      return Maybe.fail(failure.get());
    }

    return Maybe.nothing();
  }

  /**
   * Maps the current success value of the monad to another value using the
   * provided {@link Function} mapper. If the monda has a failure instead, the
   * monad with the same exception is returned.
   * 
   * This method is similar to {@link #map(Function)}, but the mapping function is
   * one whose result is already a {@code Maybe}, and if invoked, flatMap does not
   * wrap it within an additional {@code Maybe}.
   * 
   * @param <U>    the type the success value will be mapped to
   * @param mapper the mapper function
   * @return a {@code Maybe} with the mapped value if success is present, with the
   *         same exception if failed, {@link #nothing()} otherwise
   */
  public <U> Maybe<U, E> flatMap(final Function<T, Maybe<U, E>> mapper) {
    if (success.isPresent()) {
      return mapper.apply(success.get());
    }

    if (failure.isPresent()) {
      return Maybe.fail(failure.get());
    }

    return Maybe.nothing();
  }

  /**
   * Resolves the value of another throwing operation. The possible values of the
   * success or failure exception of the previous {@code maybe} are passed as
   * optional to the checked function.
   * 
   * @param <U>           the type of the value returned by the passes operation
   * @param <X>           the type of the exception thrown by the passed operation
   * @param thenOperation the {@link BiFunctionChecked} throwing operation
   * @return another {@code Maybe} with other success value, or other failure
   *         exception
   */
  public <U, X extends Exception> Maybe<U, X> thenResolve(
    final BiFunctionChecked<Optional<T>, Optional<E>, U, X> thenOperation
  ) {
    return Maybe.resolve(() -> thenOperation.applyChecked(success, failure));
  }

  /**
   * Executes another throwing operation. The possible values of the
   * success or failure exception of the previous {@code maybe} are passed as
   * optional to the checked function.
   * 
   * @param <X>           the type of the exception thrown by the passed operation
   * @param thenOperation the {@link BiFunctionChecked} throwing operation
   * @return another {@code Maybe} with either {@link #nothing()}, or the failure
   *         exception
   */
  public <X extends Exception> Maybe<Void, X> thenExecute(
    final BiConsumerChecked<Optional<T>, Optional<E>, X> thenOperation
  ) {
    return Maybe.execute(() -> thenOperation.acceptChecked(success, failure));
  }

  /**
   * Cast the success value of the monad to the passed {@code Class} type, if present.
   * This operation may throw an unchecked @{link java.lang.ClassCastException ClassCastException}
   * if the object is not null and is not assignable to the type {@code U}.
   * 
   * @param <U> the type the success value will be cast to
   * @param type the class instance of the type to cast
   * @return a new {@code Maybe} with the cast success value, if present, the
   *         same failure exception otherwhise. If none is present, {@link #nothing()}\
   *         is returned instead.
   */
  public <U> Maybe<U, E> cast(Class<U> type) {
    if (success.isPresent()) {
      return Maybe.just(type.cast(success.get()));
    }

    if (failure.isPresent()) {
      return Maybe.fail(failure.get());
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
   * Checks if the {@code Maybe} has a failure exception.
   * 
   * @return true if a failure exception is present, false otherwise
   */
  public boolean hasFailure() {
    return failure.isPresent();
  }

  /**
   * Checks if the {@code Maybe} has nothing. That is, when no success
   * value and failure exception are present.
   * 
   * @return true if both success value and failure exception are not
   *         present, false otherwise
   */
  public boolean hasNothing() {
    return success.isEmpty() && failure.isEmpty();
  }
}

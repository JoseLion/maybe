package io.github.joselion.maybe;

import static java.util.Objects.isNull;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.Nullable;

import io.github.joselion.maybe.helpers.Commons;
import io.github.joselion.maybe.util.Either;
import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingFunction;

/**
 * SolveHandler is an API to handle the possible error of a {@link Maybe}'s
 * solve operation. It can return back to maybe to continue linking operations,
 * or use terminal methods to return a safe value.
 *
 * @param <T> the type of the value passed through the {@code Maybe}
 * @param <E> the type of exception that the solve operation may throw
 * @author Jose Luis Leon
 * @since v0.3.2
 */
public final class SolveHandler<T, E extends Throwable> {

  private final Either<E, T> value;

  private SolveHandler(final Either<E, T> value) {
    this.value = value;
  }

  /**
   * Internal use method to instantiate a SolveHandler of a success value.
   *
   * @param <T> the type of the success value
   * @param <E> the type of the possible exception
   * @param success the success value to instantiate the SolveHandler
   * @return a SolveHandler instance with a success value
   */
  static <T, E extends Throwable> SolveHandler<T, E> from(final T success) {
    final var nullException = new NullPointerException("The \"Maybe<T>\" value solved to null");
    final var either = isNull(success) // NOSONAR
      ? Either.<E, T>ofLeft(Commons.cast(nullException))
      : Either.<E, T>ofRight(success);

    return new SolveHandler<>(either);
  }

  /**
   * Internal use method to instantiate a SolveHandler of an error value.
   *
   * @param <T> the type of the success value
   * @param <E> the type of the possible exception
   * @param error the error to instantiate the SolveHandler
   * @return a SolveHandler instance with an error value
   */
  static <T, E extends Throwable> SolveHandler<T, E> failure(final E error) {
    final var nullException = new NullPointerException("The \"Maybe<T>\" error was null");
    final var either = isNull(error) // NOSONAR
      ? Either.<E, T>ofLeft(Commons.cast(nullException))
      : Either.<E, T>ofLeft(error);

    return new SolveHandler<>(either);
  }

  /**
   * Internal use only.
   *
   * @return the possible success value
   */
  Optional<T> success() {
    return this.value.rightToOptional();
  }

  /**
   * Internal use only.
   *
   * @return the possible thrown exception
   */
  Optional<E> error() {
    return this.value.leftToOptional();
  }

  /**
   * Run an effect if the operation solved successfully. The solved value
   * is passed in the argument of the {@code effect} consumer.
   *
   * @param effect a consumer that receives the solved value
   * @return the same handler to continue chainning operations
   */
  public SolveHandler<T, E> doOnSuccess(final Consumer<? super T> effect) {
    this.value.doOnRight(effect);

    return this;
  }

  /**
   * Run an effect if the error is present and is an instance of the provided
   * type. The error is passed in the argument of the {@code effect}
   * consumer.
   *
   * @param <X> the type of the error to match
   * @param ofType a class instance of the error type to match
   * @param effect a consumer that receives the caught error
   * @return the same handler to continue chainning operations
   */
  public <X extends Throwable> SolveHandler<T, E> doOnError(final Class<X> ofType, final Consumer<? super X> effect) {
    this.value
      .leftToOptional()
      .filter(ofType::isInstance)
      .map(ofType::cast)
      .ifPresent(effect);

    return this;
  }

  /**
   * Run an effect if the error is present. The error is passed in the argument
   * of the {@code effect} consumer.
   *
   * @param effect a consumer that receives the caught error
   * @return the same handler to continue chainning operations
   */
  public SolveHandler<T, E> doOnError(final Consumer<Throwable> effect) {
    this.value.doOnLeft(effect);

    return this;
  }

  /**
   * Catch the error if is present and is an instance of the provided type.
   * Then handle the error and return a new value. The caught error is passed
   * in the argument of the {@code handler} function.
   *
   * @param <X> the type of the error to catch
   * @param ofType a class instance of the error type to catch
   * @param handler a function that receives the caught error and produces
   *                another value
   * @return a handler containing a new value if an error instance of the
   *         provided type was caught. The same handler instance otherwise
   */
  public <X extends Throwable> SolveHandler<T, E> catchError(
    final Class<X> ofType,
    final Function<? super X, ? extends T> handler
  ) {
    return this.value
      .leftToOptional()
      .filter(ofType::isInstance)
      .map(ofType::cast)
      .map(handler)
      .map(SolveHandler::<T, E>from)
      .orElse(this);
  }

  /**
   * Catch the error if is present and handle it to return a new value. The
   * caught error is passed in the argument of the {@code handler} function.
   *
   * @param handler a function that receives the caught error and produces
   *                another value
   * @return a handler containing a new value if an error was caught. The same
   *         handler instance otherwise
   */
  public SolveHandler<T, E> catchError(final Function<Throwable, ? extends T> handler) {
    return this.value
      .mapLeft(handler)
      .mapLeft(SolveHandler::<T, E>from)
      .leftOrElse(this);
  }

  /**
   * If the value was not previously solved, chains another solver function.
   * 
   * <p>This is helpful to try to solve values in different ways, like when
   * nesting a try-catch in another catch block, but more functional.
   *
   * <p>The caught error {@code E} is passed in the argument of the
   * {@code solver} function.
   *
   * @param <X> the type of the error the new solver may throw
   * @param solver a throwing function that receives the previous error and
   *               solves another value
   * @return a new handler with either the solved value or the error
   */
  public <X extends Throwable> SolveHandler<T, X> onErrorSolve(
    final ThrowingFunction<Throwable, ? extends T, ? extends X> solver
  ) {
    return this.value
      .unwrap(
        Maybe.partial(solver),
        SolveHandler::from
      );
  }

  /**
   * If the value was not previously solved and the type matches the error
   * thrown, chains another solver function.
   *
   * <p>This is helpful to try to solve values in different ways, like when
   * nesting a try-catch in another catch block, but more functional.
   *
   * @param <C> the type of the error to catch
   * @param <X> the type of the error the new solver may throw
   * @param ofType a class instance of the error type to catch
   * @param solver a throwing function that receives the previous error and
   *               solves another value
   * @return a new handler with either the solved value or the error
   */
  public <C extends Throwable, X extends Throwable> SolveHandler<T, X> onErrorSolve(
    final Class<C> ofType,
    final ThrowingFunction<? super C, ? extends T, ? extends X> solver
  ) {
    return this.value
      .unwrap(
        prev -> ofType.isInstance(prev)
          ? Maybe.of(prev).map(Commons::<C>cast).solve(solver)
          : SolveHandler.failure(Commons.cast(prev)),
        SolveHandler::from
      );
  }

  /**
   * Chain another solver covering both cases of success or error of the
   * previous solver in two different callbacks.
   *
   * <p>The first callback receives the solved value, the second callback the
   * caught error. Both should solve another value of the same type {@code S},
   * but only one of the callbacks is invoked. It depends on whether the
   * previous value was solved or not.
   *
   * @param <S> the type of value returned by the next operation
   * @param <X> the type of exception the new solver may throw
   * @param onSuccess a throwing function that receives the current value
   *                  and solves another
   * @param onError a throwing function that receives the error and solves
   *                another value
   * @return a new handler with either the solved value or the error
   */
  public <S, X extends Throwable> SolveHandler<S, X> solve(
    final ThrowingFunction<? super T, ? extends S, ? extends X> onSuccess,
    final ThrowingFunction<Throwable, ? extends S, ? extends X> onError
  ) {
    return this.value.unwrap(
      Maybe.partial(onError),
      Maybe.partial(onSuccess)
    );
  }

  /**
   * Chain another solver function if the value was solved. Otherwise,
   * returns a handler containing the error so it can be propagated downstream.
   *
   * @param <S> the type of value returned by the next operation
   * @param <X> the type of exception the new solver may throw
   * @param solver a throwing function that receives the current value and
   *               solves another
   * @return a new handler with either the solved value or an error
   */
  public <S, X extends Throwable> SolveHandler<S, X> solve(
    final ThrowingFunction<? super T, ? extends S, ? extends X> solver
  ) {
    return this.value
      .mapLeft(Commons::<X>cast)
      .unwrap(
        SolveHandler::failure,
        Maybe.partial(solver)
      );
  }

  /**
   * Chain the previous operation to an effect covering both the success or
   * error cases in two different callbacks.
   *
   * @param <X> the type of the error the effect may throw
   * @param onSuccess a throwing consumer to run in case of succeess
   * @param onError a throwing consumer to run in case of error
   * @return an {@link EffectHandler} representing the result of one of the
   *         invoked callback
   */
  public <X extends Throwable> EffectHandler<X> effect(
    final ThrowingConsumer<? super T, ? extends X> onSuccess,
    final ThrowingConsumer<Throwable, ? extends X> onError
  ) {
    return this.value.unwrap(
      Maybe.partial(onError),
      Maybe.partial(onSuccess)
    );
  }

  /**
   * Chain the previous operation to an effect if the value was solved.
   * Otherwise, returns a handler containing the error so it can be propagated
   * downstream.
   *
   * @param <X> the type of the error the effect may throw
   * @param effect a throwing consume to run in case of succeess
   * @return a new {@link EffectHandler} representing the result of the success
   *         callback or containg the error
   */
  public <X extends Throwable> EffectHandler<X> effect(final ThrowingConsumer<? super T, ? extends X> effect) {
    return this.value
      .mapLeft(Commons::<X>cast)
      .unwrap(
        EffectHandler::failure,
        Maybe.partial(effect)
      );
  }

  /**
   * If the value is present, map it to another value using the {@code mapper}
   * function. If an error is present, the {@code mapper} function is never
   * applied and the next handler will just contain the error.
   *
   * @param <U> the type the mapped value
   * @param mapper a function which takes the value as argument and returns
   *               another value
   * @return a new handler with either the mapped value or an error
   */
  public <U> SolveHandler<U, E> map(final Function<? super T, ? extends U> mapper) {
    return this.value
      .mapRight(mapper)
      .unwrap(
        SolveHandler::failure,
        SolveHandler::from
      );
  }

  /**
   * If an error is present and matches the specified {@code ofType} class, map
   * it to another throwable using the {@code mapper} function which receives
   * the matching error as input. If the error is not present or it does not
   * match the specified type, the {@code mapper} is never applied and the next
   * handler will just contain the solved value.
   *
   * @param <C> the type of error to match
   * @param <X> the type of the mapped error
   * @param ofType a class instance of the error type to match
   * @param mapper a function which takes the error as argument and returns
   *               another error
   * @return a new handler with either the mapped error or the value
   */
  public <C extends Throwable, X extends Throwable> SolveHandler<T, X> mapError(
    final Class<C> ofType,
    final Function<? super C, ? extends X> mapper
  ) {
    return this.value.unwrap(
      error -> {
        final var nextError = ofType.isInstance(error)
          ? mapper.apply(Commons.cast(error))
          : Commons.<X>cast(error);

        return SolveHandler.failure(nextError);
      },
      SolveHandler::from
    );
  }

  /**
   * If an error is present, map it to another throwable using the {@code mapper}
   * function which receives the previous error as input. If the error is not
   * present, the {@code mapper} is never applied and the next handler will
   * just contain the solved value.
   *
   * @param <X> the type of the mapped error
   * @param mapper a function which takes the error as argument and returns
   *               another error
   * @return a new handler with either the mapped error or the value
   */
  public <X extends Throwable> SolveHandler<T, X> mapError(final Function<Throwable, ? extends X> mapper) {
    return this.value
      .mapLeft(mapper)
      .unwrap(
        SolveHandler::failure,
        SolveHandler::from
      );
  }

  /**
   * If the value is present, map it to another value using the {@code mapper}
   * function. If an error is present, the {@code mapper} function is never
   * applied and the next handler will still contain the error.
   *
   * <p>This method is similar to {@link #map(Function)}, but the mapping
   * function is one whose result is a {@code Maybe}, and if invoked, flatMap
   * does not wrap it within an additional {@code Maybe}.
   *
   * @param <U> the type the value is mapped to
   * @param mapper a function which takes the value as argument and returns a
   *               {@code Maybe<U>} with another value
   * @return a new handler with either the mapped value or an error
   */
  public <U> SolveHandler<U, E> flatMap(final Function<? super T, Maybe<? extends U>> mapper) {
    return this.value
      .mapRight(mapper)
      .unwrap(
        SolveHandler::failure,
        maybe -> maybe.solve(ThrowingFunction.identity())
      );
  }

  /**
   * If the value is present and the value matches the given predicate, returns
   * a handler with the same value. Otherwise, returns a handler with an error
   * mapped by the {@code onFalse} function, which receives the value as input.
   * If an error is present, returns a handler containing the same error so it
   * can be propagated downstream.
   *
   * <p>In simpler terms, this operation is a shorcut for the following:
   * <pre>{@code
   * Maybe.of(value)
   *  .solve(MyService::processOrThrow)
   *  .solve(x -> {
   *    if (someCondition(x)) { // `predicate` param
   *      return x;
   *    }
   *
   *    throw new RuntimeException("Invalid value: " + x); // `onFalse` param
   *  });
   * }</pre>
   *
   * @param <X> the type of the supplied error
   * @param predicate a predicate to apply to the value, if present
   * @param onFalse a function that receives the value as input an returns an
   *                error in case te predicate evaluates to false
   * @return a handler with either the same value or the mapped error
   */
  public <X extends Throwable> SolveHandler<T, X> filter(
    final Predicate<T> predicate,
    final Function<T, X> onFalse
  ) {
    return this.value
      .mapLeft(Commons::<X>cast)
      .unwrap(
        SolveHandler::failure,
        prev -> predicate.test(prev)
          ? SolveHandler.from(prev)
          : SolveHandler.failure(onFalse.apply(prev))
      );
  }

  /**
   * If the value is present, casts the value to the provided {@code type}
   * class. If the error is present or the value not assignable to {@code type},
   * returns a handler with a {@link ClassCastException} error.
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
   * the provided {@code onError} bi-function, which receives the value and the
   * produced {@link ClassCastException} as inputs. If the error is present,
   * returns a handler with the same error so it can be propagated downstream.
   *
   * @param <U> the type of the cast value
   * @param <X> the type of the mapped exception
   * @param type the class instance of the type to cast
   * @param onError a bi-function to map the error in case of failure
   * @return a handler with either the cast value or the mapped error
   */
  public <U, X extends Throwable> SolveHandler<U, X> cast(
    final Class<U> type,
    final BiFunction<? super T, ClassCastException, ? extends X> onError
  ) {
    return this.solve(prev -> {
      try {
        return type.cast(prev);
      } catch (ClassCastException e) {
        throw onError.apply(prev, e);
      }
    });
  }

  /**
   * Returns the solved value if present. Another value otherwise.
   *
   * @param fallback the value to return if the operation failed to solve
   * @return the solved value if present. Another value otherwise
   */
  public T orElse(final T fallback) {
    return this.value.rightOrElse(fallback);
  }

  /**
   * Returns the solved value if present. Otherwise, the result produced by
   * the mapping function, which has the error on its argument, and returns
   * another value.
   *
   * @param mapper a function that receives the caught error and produces
   *               another value
   * @return the solved value if present. Another value otherwise
   */
  public T orElse(final Function<Throwable, ? extends T> mapper) {
    return this.value.unwrap(mapper, Function.identity());
  }

  /**
   * Returns the solved value if present. Otherwise, another value produced by
   * the {@code supplier}.
   *
   * @apiNote Use this method instead of {@link #orElse(Object)} to do lazy
   *          evaluation of the produced value. That means that the "else"
   *          value won't be evaluated if the error is not present.
   * @param supplier the suplier that produces another value if the operation
   *                 failed to solve
   * @return the solved value if present. Another value otherwise
   */
  public T orElseGet(final Supplier<? extends T> supplier) {
    return this.value
      .rightToOptional()
      .orElseGet(supplier);
  }

  /**
   * Returns the solved value if present. Just {@code null} otherwise.
   *
   * <p>It's strongly encouraged to use {@link #toOptional()} instead to better
   * handle nullability, but if you really need to return {@code null} in case
   * of error, you should only use this method.
   *
   * <p>Using {@code .orElse(null)} will result in ambiguity between
   * {@link #orElse(Function)} and {@link #orElse(Object)}.
   *
   * @return the solved value if present. Just {@code null} otherwise.
   */
  public @Nullable T orNull() {
    return this.value.rightOrNull();
  }

  /**
   * Returns the solved value if present. Throws the error otherwise.
   *
   * @return the solved/handled value if present
   * @throws E the error thrown by the {@code solve} operation
   */
  public T orThrow() throws E {
    return this.value
      .rightToOptional()
      .orElseThrow(this.value::leftOrNull);
  }

  /**
   * Returns the solved value if present. Throws another error otherwise.
   *
   * @param <X> the new error type
   * @param mapper a function that receives the caught error and produces
   *               another exception
   * @return the solved/handled value if present
   * @throws X a mapped exception
   */
  public <X extends Throwable> T orThrow(final Function<Throwable, ? extends X> mapper) throws X {
    return this.value
      .rightToOptional()
      .orElseThrow(() -> mapper.apply(this.value.leftOrNull()));
  }

  /**
   * Transforms the handler to a {@link Maybe} that contains either the
   * solved value or the error.
   *
   * @return the solved value wrapped in a {@link Maybe} or holding the error
   */
  public Maybe<T> toMaybe() {
    return this.value
      .rightToOptional()
      .map(Maybe::of)
      .orElseGet(Maybe::empty);
  }

  /**
   * Transforms the handler to an {@link Optional}. If the value was solved,
   * the {@link Optional} will contain it. Returs an {@code empty} optional
   * otherwise.
   *
   * @return the solved value wrapped in an {@link Optional} if present. An
   *         {@code empty} optional otherwise.
   */
  public Optional<T> toOptional() {
    return this.value.rightToOptional();
  }

  /**
   * Transforms the handler to an {@link Either}, in which the left side might
   * contain the error and the right side might contain the solved value.
   *
   * <p>The benefit of transforming to {@code Either} is that its implementation
   * ensures that only one of the two possible values is present at the same
   * time, never both nor none.
   *
   * @return an {@code Either} with the solved value on the right side or the
   *         error on the left
   */
  public Either<E, T> toEither() {
    return this.value;
  }

  /**
   * Map the value to an {@link AutoCloseable} resource to be use in either a
   * {@code solve} or {@code effect} operation. These operations will close the
   * resource upon completation. If the value was not solved, the error is
   * propagated to the {@link CloseableHandler}.
   *
   * @param <R> the type of the {@link AutoCloseable} resource
   * @param mapper a function that receives the solved value and produces an
   *               autoclosable resource
   * @return a {@link CloseableHandler} with the mapped resource if the value is
   *         present or the error otherwise.
   * @see CloseableHandler#solve(ThrowingFunction)
   * @see CloseableHandler#effect(ThrowingConsumer)
   */
  public <R extends AutoCloseable> CloseableHandler<R, E> mapToResource(final Function<T, R> mapper) {
    return this.value
      .mapRight(mapper)
      .unwrap(
        CloseableHandler::failure,
        CloseableHandler::from
      );
  }

  /**
   * Solve a function that may create an {@link AutoCloseable} resource using
   * the value in the handle, (if any). If the function is solved it returns
   * a {@link CloseableHandler} that will close the resource after used. If the
   * function does not solves or the value is not present, the error is
   * propagated to the {@link CloseableHandler}.
   *
   * @param <R> the type of the {@link AutoCloseable} resource
   * @param <X> the error type the solver function may throw
   * @param solver a function that returns either a resource or throws an exception
   * @return a {@link CloseableHandler} with the solved resource if the value is
   *         present or the error otherwise.
   */
  public <R extends AutoCloseable, X extends Throwable> CloseableHandler<R, X> solveResource(
    final ThrowingFunction<? super T, ? extends R, ? extends X> solver
  ) {
    return this.value
      .mapLeft(Commons::<X>cast)
      .unwrap(
        CloseableHandler::failure,
        prev ->
          Maybe
            .of(prev)
            .solve(solver)
            .map(CloseableHandler::<R, X>from)
            .orElse(error -> CloseableHandler.failure(Commons.cast(error)))
      );
  }
}

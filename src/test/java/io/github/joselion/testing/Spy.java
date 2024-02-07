package io.github.joselion.testing;

import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.mock;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.joselion.maybe.helpers.Commons;
import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingFunction;
import io.github.joselion.maybe.util.function.ThrowingRunnable;
import io.github.joselion.maybe.util.function.ThrowingSupplier;

public final class Spy {

  private Spy() {
    throw new UnsupportedOperationException("Spy is a helper class");
  }

  /**
   * Creates a spy of a generic {@code T} lambda expression.
   *
   * @param <T> the type of the lambda expression
   * @param lambda the lambda expression to spy on
   * @return a spy of the provided lambda expression
   */
  public static <T> T lambda(final T lambda) {
    final var interfaces = lambda.getClass().getInterfaces();
    final var toMock = Commons.<Class<T>>cast(interfaces[0]);

    return mock(toMock, delegatesTo(lambda));
  }

  /**
   * Creates a spy of a generic {@link Function} interface.
   *
   * @param <T> the type of the input to the function
   * @param <U> the type of the result of the function
   * @param function the function to spy on
   * @return a spy of the provided function
   */
  public static <T, U> Function<T, U> function(final Function<T, U> function) {
    return lambda(function);
  }

  /**
   * Creates a spy of a generic {@link Consumer} interface.
   *
   * @param <T> the type of the input to the operation
   * @param consumer the consumer to spy on
   * @return a spy of the provided consumer
   */
  public static <T> Consumer<T> consumer(final Consumer<T> consumer) {
    return lambda(consumer);
  }

  /**
   * Creates a spy of a generic {@link Supplier} interface.
   *
   * @param <T> the type of results supplied by this supplier
   * @param supplier the supplier to spy on
   * @return a spy of the provided supplier
   */
  public static <T> Supplier<T> supplier(final Supplier<T> supplier) {
    return lambda(supplier);
  }

  /**
   * Creates a spy of a generic {@link Runnable} interface.
   *
   * @param runnable the runneble to spy on
   * @return a spy of the provided runnable
   */
  public static Runnable runnable(final Runnable runnable) {
    return lambda(runnable);
  }

  /**
   * Creates a spy of a generic {@link ThrowingFunction} interface.
   *
   * @param <T> the type of the input to the function
   * @param <R> the type of the result of the function
   * @param <E> the type of exception that the function throws
   * @param throwingFunction a throwing function to spy
   * @return a spy of the provided throwing function
   */
  public static <T, R, E extends Throwable> ThrowingFunction<T, R, E> throwingFunction(
    final ThrowingFunction<T, R, E> throwingFunction
  ) {
    return lambda(throwingFunction);
  }

  /**
   * Creates a spy of a generic {@link ThrowingConsumer} interface.
   *
   * @param <T> the type of argument passed to the function
   * @param <E> the type of exception that the function throws
   * @param throwingConsumer a throwing consumer to spy
   * @return a spy of the provided throwing consumer
   */
  public static <T, E extends Throwable> ThrowingConsumer<T, E> throwingConsumer(
    final ThrowingConsumer<T, E> throwingConsumer
  ) {
    return lambda(throwingConsumer);
  }

  /**
   * Creates a spy of a generic {@link ThrowingSupplier} interface.
   *
   * @param <T> the type of results supplied by the supplier
   * @param <E> the type of checked exception that the supplier throws
   * @param throwingSupplier a throwing supplier to spy
   * @return a spy of the provided throwing supplier
   */
  public static <T, E extends Throwable> ThrowingSupplier<T, E> throwingSupplier(
    final ThrowingSupplier<T, E> throwingSupplier
  ) {
    return lambda(throwingSupplier);
  }

  /**
   * Creates a spy of a generic {@link ThrowingRunnable} interface.
   *
   * @param <E> the type of checked exception that the runnable throws
   * @param throwingRunnable a throwing runnable to spy
   * @return a spy of the provided throwing runnable
   */
  public static <E extends Throwable> ThrowingRunnable<E> throwingRunnable(
    final ThrowingRunnable<E> throwingRunnable
  ) {
    return lambda(throwingRunnable);
  }
}

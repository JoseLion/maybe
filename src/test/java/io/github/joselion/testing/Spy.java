package io.github.joselion.testing;

import static org.mockito.AdditionalAnswers.delegatesTo;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.mockito.Mockito;

import io.github.joselion.maybe.helpers.Common;

public class Spy {

  public static <T> T lambda(final T lambda) {
    final var interfaces = lambda.getClass().getInterfaces();
    final var toMock = Common.<Class<T>>cast(interfaces[0]);

    return Mockito.mock(toMock, delegatesTo(lambda));
  }

  public static <T, U> Function<T, U> function(final Function<T, U> function) {
    return lambda(function);
  }

  public static <T> Consumer<T> consumer(final Consumer<T> consumer) {
    return lambda(consumer);
  }

  public static <T> Supplier<T> supplier(final Supplier<T> supplier) {
    return lambda(supplier);
  }

  public static Runnable runnable(final Runnable runnable) {
    return lambda(runnable);
  }
}

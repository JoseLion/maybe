package io.github.joselion.testing;

import static org.mockito.AdditionalAnswers.delegatesTo;

import org.mockito.Mockito;

import io.github.joselion.maybe.helpers.Common;

public class Spy {

  public static <T> T lambda(final T lambda) {
    final var interfaces = lambda.getClass().getInterfaces();
    final var toMock = Common.<Class<T>>cast(interfaces[0]);

    return Mockito.mock(toMock, delegatesTo(lambda));
  }
}

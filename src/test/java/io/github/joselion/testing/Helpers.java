package io.github.joselion.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;

import org.mockito.Mockito;

public class Helpers {

  @SuppressWarnings("unchecked")
  public static <T> T spyLambda(final T lambda) {
    Class<?>[] interfaces = lambda.getClass().getInterfaces();
    assertThat(interfaces).hasSize(1);

    return Mockito.mock((Class<T>) interfaces[0], delegatesTo(lambda));
  }
}

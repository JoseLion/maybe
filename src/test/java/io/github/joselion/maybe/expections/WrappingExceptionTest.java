package io.github.joselion.maybe.expections;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.maybe.exceptions.WrappingException;
import io.github.joselion.testing.UnitTest;

@UnitTest
public class WrappingExceptionTest {

  @Nested class wrapped {
    @Test void returns_the_same_exception_as_getCause() {
      final IOException exception = new IOException("FAIL");
      final WrappingException wrapping = WrappingException.of(exception);

      assertThat(wrapping.wrapped()).isSameAs(wrapping.getCause());
    }
  }
}

package io.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingFunction;
import io.github.joselion.testing.Spy;
import io.github.joselion.testing.UnitTest;

@UnitTest class ResourceHolderTest {

  private static final String FILE_PATH = "./src/test/resources/readTest.txt";

  private static final IOException FAIL_EXCEPTION = new IOException("FAIL");

  private final ThrowingFunction<FileInputStream, String, RuntimeException> okResolver = res -> "OK";

  private final ThrowingFunction<FileInputStream, Object, IOException> errorResolver = res -> {
    throw FAIL_EXCEPTION;
  };

  private final ThrowingConsumer<FileInputStream, RuntimeException> noOpEffect = res -> { };

  private final ThrowingConsumer<FileInputStream, IOException> errorEffect = res -> {
    throw FAIL_EXCEPTION;
  };

  @Nested class resolveClosing {
    @Nested class when_the_resource_is_present {
      @Nested class when_the_operation_succeeds {
        @Test void returns_a_handler_with_the_value() {
          final var fis = getFIS();
          final var resolverSpy = Spy.lambda(okResolver);
          final var handler = Maybe.withResource(fis)
            .resolveClosing(resolverSpy);

          assertThat(handler.success()).contains("OK");
          assertThat(handler.error()).isEmpty();
          assertThatThrownBy(fis::read)
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("Stream Closed");

          verify(resolverSpy).apply(fis);
        }
      }

      @Nested class when_the_operation_fails {
        @Test void returns_a_handler_with_the_error() throws IOException {
          final var fis = getFIS();
          final var resolverSpy = Spy.lambda(errorResolver);
          final var handler = Maybe.withResource(fis)
            .resolveClosing(resolverSpy);

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error()).contains(FAIL_EXCEPTION);
          assertThatThrownBy(fis::read)
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("Stream Closed");

          verify(resolverSpy).apply(fis);
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_handler_with_the_propagated_error() throws Throwable {
        final var error = new IOException("Something went wrong...");
        final var resolverSpy = Spy.<ThrowingFunction<AutoCloseable, ?, ?>>lambda(fis -> "");
        final var handler = ResourceHolder.failure(error)
          .resolveClosing(resolverSpy);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(IOException.class)
          .hasMessage(error.getMessage());

        verify(resolverSpy, never()).apply(any());
      }
    }
  }

  @Nested class runEffectClosing {
    @Nested class when_the_resource_is_present {
      @Nested class when_the_operation_succeeds {
        @Test void returns_a_handler_with_nothing() {
          final var fis = getFIS();
          final var effectSpy = Spy.lambda(noOpEffect);
          final var handler = Maybe.withResource(fis)
            .runEffectClosing(effectSpy);

          assertThat(handler.error()).isEmpty();
          assertThatThrownBy(fis::read)
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("Stream Closed");

          verify(effectSpy).accept(fis);
        }
      }

      @Nested class when_the_operation_fails {
        @Test void returns_a_handler_with_the_error() throws IOException {
          final var fis = getFIS();
          final var effectSpy = Spy.lambda(errorEffect);
          final var handler = Maybe.withResource(fis)
            .runEffectClosing(effectSpy);

          assertThat(handler.error()).contains(FAIL_EXCEPTION);
          assertThatThrownBy(fis::read)
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("Stream Closed");

          verify(effectSpy).accept(fis);
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_handler_with_the_propagated_error() throws Throwable {
        final var error = new IOException("Something went wrong...");
        final var effectSpy = Spy.<ThrowingConsumer<AutoCloseable, ?>>lambda(res -> { });
        final var handler = ResourceHolder.failure(error)
          .runEffectClosing(effectSpy);

        assertThat(handler.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(IOException.class)
          .hasMessage(error.getMessage());

        verify(effectSpy, never()).accept(any());
      }
    }
  }

  private FileInputStream getFIS() {
    return Maybe.just(FILE_PATH)
      .resolve(FileInputStream::new)
      .orThrow(Error::new);
  }
}

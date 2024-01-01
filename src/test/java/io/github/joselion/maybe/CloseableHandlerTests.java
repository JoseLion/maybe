package io.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.FileInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingFunction;
import io.github.joselion.testing.Spy;
import io.github.joselion.testing.UnitTest;

@UnitTest class CloseableHandlerTests {

  private static final String FILE_PATH = "./src/test/resources/readTest.txt";

  private static final IOException FAIL_EXCEPTION = new IOException("FAIL");

  private final ThrowingFunction<FileInputStream, String, RuntimeException> okSolver = res -> "OK";

  private final ThrowingFunction<FileInputStream, Object, IOException> errorSolver = res -> {
    throw FAIL_EXCEPTION;
  };

  private final ThrowingConsumer<FileInputStream, RuntimeException> noOpEffect = res -> { };

  private final ThrowingConsumer<FileInputStream, IOException> errorEffect = res -> {
    throw FAIL_EXCEPTION;
  };

  @Nested class from {
    @Nested class when_the_resource_is_not_null {
      @Test void returns_a_handler_with_the_value() {
        final var fis = getFIS();
        final var handler = CloseableHandler.from(fis);

        assertThat(handler.resource()).containsSame(fis);
        assertThat(handler.error()).isEmpty();
      }
    }

    @Nested class when_the_resource_is_null {
      @Test void returns_a_handler_with_a_NullPointerException_error() {
        final var handler = CloseableHandler.from(null);

        assertThat(handler.resource()).isEmpty();
        assertThat(handler.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(NullPointerException.class)
          .hasMessage("The \"Maybe<T>\" resource solved to null");
      }
    }
  }

  @Nested class failure {
    @Nested class when_the_error_is_not_null {
      @Test void returns_a_handler_with_the_error() {
        final var handler = CloseableHandler.failure(FAIL_EXCEPTION);

        assertThat(handler.resource()).isEmpty();
        assertThat(handler.error()).containsSame(FAIL_EXCEPTION);
      }
    }

    @Nested class when_the_error_is_null {
      @Test void returns_a_handler_with_a_NullPointerException_error() {
        final var handler = CloseableHandler.failure(null);

        assertThat(handler.resource()).isEmpty();
        assertThat(handler.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(NullPointerException.class)
          .hasMessage("The \"Maybe<T>\" error was null");
      }
    }
  }

  @Nested class solve {
    @Nested class when_the_resource_is_present {
      @Nested class when_the_operation_succeeds {
        @Test void returns_a_handler_with_the_value() {
          final var fis = getFIS();
          final var solverSpy = Spy.lambda(okSolver);
          final var handler = Maybe.withResource(fis)
            .solve(solverSpy);

          assertThat(handler.success()).contains("OK");
          assertThat(handler.error()).isEmpty();
          assertThatCode(fis::read)
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("Stream Closed");

          verify(solverSpy).apply(fis);
        }
      }

      @Nested class when_the_operation_fails {
        @Test void returns_a_handler_with_the_error() throws IOException {
          final var fis = getFIS();
          final var solverSpy = Spy.lambda(errorSolver);
          final var handler = Maybe.withResource(fis)
            .solve(solverSpy);

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error()).contains(FAIL_EXCEPTION);
          assertThatCode(fis::read)
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("Stream Closed");

          verify(solverSpy).apply(fis);
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_handler_with_the_propagated_error() throws Throwable {
        final var error = new IOException("Something went wrong...");
        final var solverSpy = Spy.<ThrowingFunction<AutoCloseable, ?, ?>>lambda(fis -> "");
        final var handler = CloseableHandler.failure(error)
          .solve(solverSpy);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(IOException.class)
          .hasMessage(error.getMessage());

        verify(solverSpy, never()).apply(any());
      }
    }
  }

  @Nested class resolveClosing {
    @Test void calls_solve() {
      final var identity = ThrowingFunction.identity();
      final var error = new IOException("Something went wrong...");
      final var handler = spy(CloseableHandler.failure(error));
      handler.resolveClosing(identity);

      verify(handler).solve(identity);
    }
  }

  @Nested class effect {
    @Nested class when_the_resource_is_present {
      @Nested class when_the_operation_succeeds {
        @Test void returns_an_empty_handler() {
          final var fis = getFIS();
          final var effectSpy = Spy.lambda(noOpEffect);
          final var handler = Maybe.withResource(fis)
            .effect(effectSpy);

          assertThat(handler.error()).isEmpty();
          assertThatCode(fis::read)
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
            .effect(effectSpy);

          assertThat(handler.error()).contains(FAIL_EXCEPTION);
          assertThatCode(fis::read)
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
        final var handler = CloseableHandler.failure(error)
          .effect(effectSpy);

        assertThat(handler.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(IOException.class)
          .hasMessage(error.getMessage());

        verify(effectSpy, never()).accept(any());
      }
    }
  }

  @Nested class runEffectClosing {
    @Test void calls_effect() {
      final var handler = spy(CloseableHandler.<FileInputStream, IOException>failure(FAIL_EXCEPTION));
      handler.runEffectClosing(noOpEffect);

      verify(handler).effect(noOpEffect);
    }
  }

  private FileInputStream getFIS() {
    return Maybe.of(FILE_PATH)
      .solve(FileInputStream::new)
      .orThrow(Error::new);
  }
}

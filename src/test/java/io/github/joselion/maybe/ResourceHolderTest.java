package io.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.testing.UnitTest;

@UnitTest class ResourceHolderTest {

  private static final String FILE_PATH = "./src/test/resources/readTest.txt";

  @Nested class resolveClosing {
    @Nested class when_the_resource_is_present {
      @Nested class when_the_operation_succeeds {
        @Test void returns_a_handler_with_the_value() {
          final var fis = getFIS();
          final var handler = Maybe.withResource(fis)
            .resolveClosing(res -> {
              assertThat(res)
                .isEqualTo(fis)
                .hasContent("foo");
              return "OK";
            });

          assertThat(handler.success()).contains("OK");
          assertThat(handler.error()).isEmpty();

          assertThatThrownBy(fis::read)
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("Stream Closed");
        }
      }

      @Nested class when_the_operation_fails {
        @Test void returns_a_handler_with_the_error() {
          final var fis = getFIS();
          final var exception = new IOException("FAIL");
          final var handler = Maybe.withResource(fis)
            .resolveClosing(res -> {
              assertThat(res)
                .isEqualTo(fis)
                .hasContent("foo");
              throw exception;
            });

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error()).contains(exception);
          assertThatThrownBy(fis::read)
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("Stream Closed");
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_handler_with_the_propagated_error() {
        final var error = new IOException("Something went wrong...");
        final var handler = ResourceHolder.failure(error)
          .resolveClosing(fis -> {
            throw new AssertionError("The handler should not be executed!");
          });

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error())
          .containsInstanceOf(IOException.class)
          .get(InstanceOfAssertFactories.THROWABLE)
          .hasMessage(error.getMessage());
      }
    }

    @Nested class when_neither_the_resource_nor_the_error_is_present {
      @Test void returns_a_handler_with_nothing() {
        final var handler = ResourceHolder.from(null)
          .resolveClosing(res -> {
            throw new AssertionError("The handler should not be executed!");
          });

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).isEmpty();
      }
    }
  }

  @Nested class runEffectClosing {
    @Nested class when_the_resource_is_present {
      @Nested class when_the_operation_succeeds {
        @Test void returns_a_handler_with_nothing() {
          final var counter = new ArrayList<>();
          final var fis = getFIS();
          final var handler = Maybe.withResource(fis)
            .runEffectClosing(res -> {
              assertThat(res)
                .isEqualTo(fis)
                .hasContent("foo");
              counter.add(1);
            });

          assertThat(counter).containsExactly(1);

          assertThat(handler.error()).isEmpty();

          assertThatThrownBy(fis::read)
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("Stream Closed");
        }
      }

      @Nested class when_the_operation_fails {
        @Test void returns_a_handler_with_the_error() {
          final var fis = getFIS();
          final var exception = new IOException("FAIL");
          final var handler = Maybe.withResource(fis)
            .runEffectClosing(res -> {
              assertThat(res)
                .isEqualTo(fis)
                .hasContent("foo");
              throw exception;
            });

          assertThat(handler.error()).contains(exception);

          assertThatThrownBy(fis::read)
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("Stream Closed");
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_handler_with_the_propagated_error() {
        final var error = new IOException("Something went wrong...");
        final var handler = ResourceHolder.failure(error)
          .runEffectClosing(res -> {
            throw new AssertionError("The handler should not be executed!");
          });

        assertThat(handler.error())
          .containsInstanceOf(IOException.class)
          .get(InstanceOfAssertFactories.THROWABLE)
          .hasMessage(error.getMessage());
      }
    }

    @Nested class when_neither_the_resource_nor_the_error_is_present {
      @Test void returns_a_handler_with_nothing() {
        final var handler = ResourceHolder.from(null)
          .runEffectClosing(res -> {
            throw new AssertionError("The handler should not be executed!");
          });

        assertThat(handler.error()).isEmpty();
      }
    }
  }

  private FileInputStream getFIS() {
    return Maybe.just(FILE_PATH)
      .resolve(FileInputStream::new)
      .orThrow(Error::new);
  }
}

package io.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.testing.UnitTest;

@UnitTest class ResourceHolderTest {

  private static final String FILE_PATH = "./src/test/resources/readTest.txt";

  @Nested class resolveClosing {
    @Nested class when_the_resource_is_present {
      @Nested class when_the_operation_succeeds {
        @Test void returns_a_handler_with_the_value() {
          final FileInputStream fis = getFIS();
          final ResolveHandler<String, ?> handler = Maybe.withResource(fis)
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
          final FileInputStream fis = getFIS();
          final IOException exception = new IOException("FAIL");
          final ResolveHandler<?, IOException> handler = Maybe.withResource(fis)
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
        final IOException error = new IOException("Something went wrong...");
        final ResolveHandler<FileInputStream, IOException> handler = ResourceHolder.failure(error)
          .resolveClosing(fis -> {
            throw new AssertionError("The handler should not be executed!");
          });

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).contains(error);
      }
    }

    @Nested class when_neither_the_resource_nor_the_error_is_present {
      @Test void returns_a_handler_with_nothing() {
        final ResolveHandler<AutoCloseable, Exception> handler = ResourceHolder.from(null)
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
          final List<Integer> counter = new ArrayList<>();
          final FileInputStream fis = getFIS();
          final EffectHandler<?> handler = Maybe.withResource(fis)
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
          final FileInputStream fis = getFIS();
          final IOException exception = new IOException("FAIL");
          final EffectHandler<IOException> handler = Maybe.withResource(fis)
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
        final IOException error = new IOException("Something went wrong...");
        final EffectHandler<IOException> handler = ResourceHolder.failure(error)
          .runEffectClosing(res -> {
            throw new AssertionError("The handler should not be executed!");
          });

        assertThat(handler.error()).contains(error);
      }
    }

    @Nested class when_neither_the_resource_nor_the_error_is_present {
      @Test void returns_a_handler_with_nothing() {
        final EffectHandler<Exception> handler = ResourceHolder.from(null)
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

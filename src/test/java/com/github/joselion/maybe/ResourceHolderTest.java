package com.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.joselion.maybe.helpers.UnitTest;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest class ResourceHolderTest {

  private static final String SUCCESS = "success";

  private static final String ERROR = "error";

  private static final String FILE_PATH = "./src/test/resources/readTest.txt";

  @Nested class resolveClosing {
    @Nested class when_the_resource_is_NOT_present {
      @Test void returns_a_handler_with_nothing() {
        final ResolveHandler<AutoCloseable, Exception> handler = ResourceHolder.from(null)
          .resolveClosing(res -> {
            throw new AssertionError("The handler should not be executed!");
          });

        assertThat(handler)
          .extracting(SUCCESS, optional(AutoCloseable.class))
          .isEmpty();

        assertThat(handler)
          .extracting(ERROR, optional(Exception.class))
          .isEmpty();
      }
    }

    @Nested class when_the_resource_is_present {
      @Nested class when_the_operation_success {
        @Test void returns_a_handler_with_the_value() {
          final FileInputStream fis = getFIS();
          final ResolveHandler<String, ?> handler = Maybe.withResource(fis)
            .resolveClosing(res -> {
              assertThat(res)
                .isEqualTo(fis)
                .hasContent("foo");
              return "OK";
            });

          assertThat(handler)
            .extracting(SUCCESS, optional(String.class))
            .contains("OK");

          assertThat(handler)
            .extracting(ERROR, optional(RuntimeException.class))
            .isEmpty();

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

          assertThat(handler)
            .extracting(SUCCESS, optional(Object.class))
            .isEmpty();

          assertThat(handler)
            .extracting(ERROR, optional(IOException.class))
            .containsInstanceOf(IOException.class)
            .contains(exception);

          assertThatThrownBy(fis::read)
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("Stream Closed");
        }
      }
    }
  }

  @Nested class runEffectClosing {
    @Nested class when_the_resource_is_NOT_present {
      @Test void returns_a_handler_with_nothing() {
        final EffectHandler<Exception> handler = ResourceHolder.from(null)
          .runEffectClosing(res -> {
            throw new AssertionError("The handler should not be executed!");
          });

        assertThat(handler)
          .extracting(ERROR, optional(Exception.class))
          .isEmpty();
      }
    }

    @Nested class when_the_resource_is_present {
      @Nested class when_the_operation_success {
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

          assertThat(handler)
            .extracting(ERROR, optional(RuntimeException.class))
            .isEmpty();

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

          assertThat(handler)
            .extracting(ERROR, optional(IOException.class))
            .containsInstanceOf(IOException.class)
            .contains(exception);

          assertThatThrownBy(fis::read)
            .isExactlyInstanceOf(IOException.class)
            .hasMessage("Stream Closed");
        }
      }
    }
  }

  private FileInputStream getFIS() {
    return Maybe.just(FILE_PATH)
      .resolve(FileInputStream::new)
      .orThrow(Error::new);
  }
}

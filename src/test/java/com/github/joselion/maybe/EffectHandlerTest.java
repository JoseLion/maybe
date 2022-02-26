package com.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.EOFException;
import java.io.IOException;

import com.github.joselion.maybe.helpers.UnitTest;
import com.github.joselion.maybe.util.RunnableChecked;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest class EffectHandlerTest {

  private static final IOException FAIL_EXCEPTION = new IOException("FAIL");

  private final RunnableChecked<IOException> throwingOp = () -> {
    throw FAIL_EXCEPTION;
  };

  private final RunnableChecked<RuntimeException> noOp = () -> { };

  @Nested class onError {
    @Nested class when_the_error_is_present {
      @Nested class and_the_error_is_instance_of_the_checked_exception {
        @Test void the_handler_is_applied() {
          assertThat(
            Maybe.fromRunnable(throwingOp)
              .doOnError(error -> {
                assertThat(error)
                  .isInstanceOf(IOException.class)
                  .hasMessageContaining("FAIL");
              })
              .error()
          )
          .isEmpty();
        }
      }

      @Nested class and_the_error_is_NOT_instance_of_the_checked_exception {
        @Test void the_handler_is_applied() {
          final RunnableChecked<IOException> failingOp = () -> {
            throw new UnsupportedOperationException("ERROR");
          };

          assertThat(
            Maybe.fromRunnable(failingOp)
              .doOnError(error -> {
                assertThat(error)
                  .isInstanceOf(UnsupportedOperationException.class)
                  .hasMessageContaining("ERROR");
              })
              .error()
          )
          .isEmpty();
        }
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void the_handler_is_NOT_applied() {
        assertThat(
          Maybe.fromRunnable(noOp)
            .doOnError(error -> {
              throw new AssertionError("The handler should not be executed");
            })
            .error()
        )
        .isEmpty();
      }
    }
  }

  @Nested class catchError {
    @Nested class when_the_error_is_present {
      @Nested class and_is_instance_of_the_errorType_argument {
        @Test void catches_the_error_and_the_handler_is_applied() {
          assertThat(
            Maybe.fromRunnable(throwingOp)
              .catchError(IOException.class, error -> {
                assertThat(error)
                  .isInstanceOf(IOException.class)
                  .hasMessage("FAIL");
              })
              .error()
          )
          .isEmpty();
        }
      }

      @Nested class and_is_NOT_instance_of_the_errorType_argument {
        @Test void the_error_is_NOT_catched_and_the_handler_is_not_applied() {
          assertThat(
            Maybe.fromRunnable(throwingOp)
              .catchError(EOFException.class, error -> {
                throw new AssertionError("The handler should not be executed");
              })
              .error()
          )
          .contains(FAIL_EXCEPTION);
        }
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void the_handler_is_NOT_applied() {
        assertThat(
          Maybe.fromRunnable(noOp)
            .catchError(RuntimeException.class, error -> {
              throw new AssertionError("The handler should not be executed");
            })
            .error()
        )
        .isEmpty();
      }
    }
  }

  @Nested class onErrorThrow {
    @Nested class when_the_error_is_present {
      @Test void throws_an_exception() {
        final EffectHandler<IOException> handler = Maybe.fromRunnable(throwingOp);

        assertThat(
          assertThrows(IOException.class, handler::onErrorThrow)
        )
        .isInstanceOf(IOException.class)
        .hasMessage("FAIL");

        assertThat(
          assertThrows(
            EOFException.class,
            () -> handler.onErrorThrow(error -> new EOFException(error.getMessage() + " - OTHER ERROR"))
          )
        )
        .isExactlyInstanceOf(EOFException.class)
        .hasMessage("FAIL - OTHER ERROR");
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void no_exception_is_thrown() {
        final EffectHandler<RuntimeException> handler = Maybe.fromRunnable(noOp);

        assertThatCode(handler::onErrorThrow).doesNotThrowAnyException();

        assertThatCode(() -> {
          handler.onErrorThrow(error -> new EOFException(error.getMessage() + " - OTHER ERROR"));
        })
        .doesNotThrowAnyException();
      }
    }
  }

  @Nested class toMaybe {
    @Test void returns_a_maybe_with_nothing() {
      assertThat(Maybe.fromRunnable(throwingOp).toMaybe().value())
        .isEmpty();
    }
  }
}

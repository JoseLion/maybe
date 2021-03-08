package com.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.EOFException;
import java.io.IOException;

import com.github.joselion.maybe.helpers.UnitTest;
import com.github.joselion.maybe.util.RunnableChecked;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class EffectHandlerTest {

  private static final String ERROR = "error";

  private static final String SUCCESS = "success";

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
            Maybe.runEffect(throwingOp)
              .onError(error -> {
                assertThat(error)
                  .isInstanceOf(IOException.class)
                  .hasMessageContaining("FAIL");
              })
          )
          .extracting(ERROR, optional(IOException.class))
          .isEmpty();
        }
      }

      @Nested class and_the_error_is_NOT_instance_of_the_checked_exception {
        @Test void the_handler_is_applied() {
          final RunnableChecked<IOException> failingOp = () -> {
            throw new UnsupportedOperationException("ERROR");
          };
          assertThat(
            Maybe.runEffect(failingOp)
              .onError(error -> {
                assertThat(error)
                  .isInstanceOf(UnsupportedOperationException.class)
                  .hasMessageContaining("ERROR");
              })
          )
          .extracting(ERROR, optional(IOException.class))
          .isEmpty();
        }
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void the_handler_is_NOT_applied() {
        assertThat(
          Maybe.runEffect(noOp)
            .onError(error -> {
              throw new AssertionError("The handler should not be executed");
            })
        )
        .extracting(ERROR, optional(RuntimeException.class))
        .isEmpty();
      }
    }
  }

  @Nested class catchError {
    @Nested class when_the_error_is_present {
      @Nested class and_is_instance_of_the_errorType_argument {
        @Test void catches_the_error_and_the_handler_is_applied() {
          assertThat(
            Maybe.runEffect(throwingOp)
              .catchError(IOException.class, error -> {
                assertThat(error)
                  .isInstanceOf(IOException.class)
                  .hasMessage("FAIL");
              })
          )
          .extracting(ERROR, optional(IOException.class))
          .isEmpty();
        }
      }

      @Nested class and_is_NOT_instance_of_the_errorType_argument {
        @Test void the_error_is_NOT_catched_and_the_handler_is_not_applied() {
          assertThat(
            Maybe.runEffect(throwingOp)
              .catchError(EOFException.class, error -> {
                throw new AssertionError("The handler should not be executed");
              })
          )
          .extracting(ERROR, optional(IOException.class))
          .contains(FAIL_EXCEPTION);
        }
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void the_handler_is_NOT_applied() {
        assertThat(
          Maybe.runEffect(noOp)
            .catchError(RuntimeException.class, error -> {
              throw new AssertionError("The handler should not be executed");
            })
        )
        .extracting(ERROR, optional(RuntimeException.class))
        .isEmpty();
      }
    }
  }

  @Nested class and {
    @Test void returns_a_maybe_with_nothing() {
      assertThat(
        Maybe.runEffect(throwingOp).and()
      )
      .extracting(SUCCESS, optional(Void.class))
      .isEmpty();
    }
  }

  @Nested class onErrorThrow {
    @Nested class when_the_error_is_present {
      @Test void throws_an_exception() {
        final EffectHandler<IOException> handler = Maybe.runEffect(throwingOp);

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
        final EffectHandler<RuntimeException> handler = Maybe.runEffect(noOp);

        assertThatCode(handler::onErrorThrow).doesNotThrowAnyException();

        assertThatCode(() -> {
          handler.onErrorThrow(error -> new EOFException(error.getMessage() + " - OTHER ERROR"));
        })
        .doesNotThrowAnyException();
      }
    }
  }
}

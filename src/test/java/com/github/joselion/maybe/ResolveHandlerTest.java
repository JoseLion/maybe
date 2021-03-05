package com.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.EOFException;
import java.io.IOException;

import com.github.joselion.maybe.helpers.UnitTest;
import com.github.joselion.maybe.util.SupplierChecked;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class ResolveHandlerTest {

  private final String SUCCESS = "success";

  private final String ERROR = "error";

  private final static IOException FAIL_EXCEPTION = new IOException("FAIL");

  private final SupplierChecked<String, IOException> throwingOp = () -> {
    throw FAIL_EXCEPTION;
  };

  private final SupplierChecked<String, RuntimeException> okOp = () -> "OK";

  @Nested class onError {
    @Nested class when_the_error_is_present {
      @Nested class and_the_error_is_instance_of_the_checked_exception {
        @Test void applies_the_handler_function() {
          final ResolveHandler<String, IOException> handler = Maybe.resolve(throwingOp)
            .onError(error -> {
              assertThat(error)
                .isInstanceOf(IOException.class)
                .hasMessage("FAIL");

              return "OK";
            });

          assertThat(handler)
            .extracting(SUCCESS, optional(String.class))
            .contains("OK");

          assertThat(handler)
            .extracting(ERROR, optional(IOException.class))
            .isEmpty();
        }
      }

      @Nested class and_the_error_is_NOT_instance_of_the_checked_exception {
        @Test void applies_the_handler_function() {
          final SupplierChecked<String, IOException> failingOp = () -> {
            throw new UnsupportedOperationException("ERROR");
          };
          final ResolveHandler<String, IOException> handler = Maybe.resolve(failingOp)
            .onError(error -> {
              assertThat(error)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("ERROR");

              return "OK";
            });

          assertThat(handler)
            .extracting(SUCCESS, optional(String.class))
            .contains("OK");

          assertThat(handler)
            .extracting(ERROR, optional(IOException.class))
            .isEmpty();
        }
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void the_error_handler_is_not_executed() {
        final ResolveHandler<String, RuntimeException> handler = Maybe.resolve(okOp)
          .onError(error -> {
            throw new AssertionError("The handler should not be executed");
          });

        assertThat(handler)
          .extracting(SUCCESS, optional(String.class))
          .contains("OK");

        assertThat(handler)
          .extracting(ERROR, optional(IOException.class))
          .isEmpty();
      }
    }
  }

  @Nested class catchError {
    @Nested class when_the_error_is_present {
      @Nested class and_is_instance_of_the_errorType_argument {
        @Test void catches_the_error_and_the_handler_is_applied() {
          final ResolveHandler<String, IOException> handler = Maybe.resolve(throwingOp)
            .catchError(IOException.class, error -> {
              assertThat(error)
                .isInstanceOf(IOException.class)
                .hasMessage("FAIL");

              return "OK";
            });

          assertThat(handler)
            .extracting(SUCCESS, optional(String.class))
            .contains("OK");

          assertThat(handler)
            .extracting(ERROR, optional(IOException.class))
            .isEmpty();
        }
      }

      @Nested class and_is_NOT_instance_of_the_errorType_argument {
        @Test void the_error_is_NOT_catched_and_the_handler_is_not_applied() {
          final ResolveHandler<String, IOException> handler = Maybe.resolve(throwingOp)
            .catchError(EOFException.class, error -> {
              throw new AssertionError("The handler should not be executed");
            });

          assertThat(handler)
            .extracting(SUCCESS, optional(String.class))
            .isEmpty();

          assertThat(handler)
            .extracting(ERROR, optional(IOException.class))
            .contains(FAIL_EXCEPTION);
        }
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void the_error_handler_is_not_executed() {
        final ResolveHandler<String, RuntimeException> handler = Maybe.resolve(okOp)
          .catchError(RuntimeException.class, error -> {
            throw new AssertionError("The handler should not be executed");
          });

        assertThat(handler)
          .extracting(SUCCESS, optional(String.class))
          .contains("OK");

        assertThat(handler)
          .extracting(ERROR, optional(IOException.class))
          .isEmpty();
      }
    }
  }

  @Nested class and {
    @Nested class when_the_value_is_present {
      @Test void returns_a_maybe_with_the_value() {
        assertThat(
          Maybe.resolve(okOp).and()
        )
        .extracting(SUCCESS, optional(String.class))
        .contains("OK");
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_a_maybe_with_nothing() {
        assertThat(
          Maybe.resolve(throwingOp).and()
        )
        .extracting(SUCCESS, optional(String.class))
        .isEmpty();
      }
    }
  }

  @Nested class orDefault {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() {
        assertThat(
          Maybe.resolve(okOp)
            .orDefault("OTHER")
        )
        .isEqualTo("OK");
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_the_default_value() {
        assertThat(
          Maybe.resolve(throwingOp)
            .orDefault("OTHER")
        )
        .isEqualTo("OTHER");
      }
    }
  }

  @Nested class orThrow {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() throws EOFException {
        assertThat(
          Maybe.resolve(okOp).orThrow()
        )
        .isEqualTo("OK");

        assertThat(
          Maybe.resolve(okOp).orThrow(error -> new EOFException(error.getMessage()))
        )
        .isEqualTo("OK");
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void throws_the_error() {
        final ResolveHandler<?, IOException> handler = Maybe.resolve(throwingOp);

        assertThat(
          assertThrows(IOException.class, handler::orThrow)
        )
        .isExactlyInstanceOf(IOException.class)
        .hasMessage("FAIL");

        assertThat(
          assertThrows(
            EOFException.class,
            () -> handler.orThrow(error -> new EOFException(error.getMessage() + " - OTHER ERROR"))
          )
        )
        .isExactlyInstanceOf(EOFException.class)
        .hasMessage("FAIL - OTHER ERROR");
      }
    }
  }

  @Nested class toOptional {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value_wrapped_in_an_optinal() {
        assertThat(Maybe.resolve(okOp).toOptional()).contains("OK");
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_an_empty_optional() {
        assertThat(Maybe.resolve(throwingOp).toOptional()).isEmpty();
      }
    }
  }
}

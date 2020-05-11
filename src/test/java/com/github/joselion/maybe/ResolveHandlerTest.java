package com.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.EOFException;
import java.io.IOException;

import com.github.joselion.maybe.helpers.UnitTest;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class ResolveHandlerTest {

  private final String SUCCESS = "success";

  private final String ERROR = "error";
  
  @Nested
  class onError {
    
    @Nested
    class when_the_error_is_present {
      
      @Test
      void applies_the_handler_function() {
        final ResolveHandler<String, IOException> handler = Maybe.resolve(() -> throwingOp())
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

    @Nested
    class when_the_error_is_NOT_present {

      @Test
      void the_error_handler_is_not_executed() {
        final ResolveHandler<String, RuntimeException> handler = Maybe.resolve(() -> "OK")
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

  @Nested
  class catchError {

    @Nested
    class when_the_error_is_present {

      @Nested
      class and_is_instance_of_the_errorType_argument {

        @Test
        void catches_the_error_and_the_handler_is_applied() {
          final ResolveHandler<String, IOException> handler = Maybe.resolve(() -> throwingOp())
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

      @Nested
      class and_is_NOT_instance_of_the_errorType_argument {

        @Test
        void the_error_is_NOT_catched_and_the_handler_is_not_applied() {
          final ResolveHandler<String, IOException> handler = Maybe.resolve(() -> throwingOp())
            .catchError(EOFException.class, error -> {
              throw new AssertionError("The handler should not be executed");
            });

          assertThat(handler)
            .extracting(SUCCESS, optional(String.class))
            .isEmpty();
  
          assertThat(handler)
            .extracting(ERROR, optional(IOException.class))
            .containsInstanceOf(IOException.class)
            .withFailMessage("FAIL");
        }
      }
    }

    @Nested
    class when_the_error_is_NOT_present {

      @Test
      void the_error_handler_is_not_executed() {
        final ResolveHandler<String, RuntimeException> handler = Maybe.resolve(() -> "OK")
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

  @Nested
  class and {
    
    @Nested
    class when_the_value_is_present {

      @Test
      void returns_a_maybe_with_the_value() {
        assertThat(
          Maybe.resolve(() -> "OK").and()
        )
        .extracting(SUCCESS, optional(String.class))
        .contains("OK");
      }
    }

    @Nested
    class when_the_value_is_NOT_present {
      @Test
      void returns_a_maybe_with_nothing() {
        assertThat(
          Maybe.resolve(() -> throwingOp()).and()
        )
        .extracting(SUCCESS, optional(String.class))
        .isEmpty();
      }
    }
  }

  @Nested
  class orDefault {
    
    @Nested
    class when_the_value_is_present {

      @Test
      void returns_the_value() {
        assertThat(
          Maybe.resolve(() -> "OK")
            .orDefault("OTHER")
        )
        .isEqualTo("OK");
      }
    }

    @Nested
    class when_the_value_is_NOT_present {

      @Test
      void returns_the_default_value() {
        assertThat(
          Maybe.resolve(() -> throwingOp())
            .orDefault("OTHER")
        )
        .isEqualTo("OTHER");
      }
    }
  }

  @Nested
  class orThrow {
    @Nested
    class when_the_value_is_present {
  
      @Test
      void returns_the_value() throws EOFException {
        assertThat(
          Maybe.resolve(() -> "OK").orThrow()
        )
        .isEqualTo("OK");

        assertThat(
          Maybe.resolve(() -> "OK").orThrow(error -> new EOFException(error.getMessage()))
        )
        .isEqualTo("OK");
      }
    }

    @Nested
    class when_the_value_is_NOT_present {

      @Test
      void throws_the_error() {
        final ResolveHandler<?, IOException> handler = Maybe.resolve(() -> throwingOp());

        assertThat(
          assertThrows(IOException.class, handler::orThrow)
        )
        .isExactlyInstanceOf(IOException.class)
        .hasMessage("FAIL");

        assertThat(
          assertThrows(
            IOException.class,
            () -> handler.orThrow(error -> new EOFException(error.getMessage() + " - OTHER ERROR"))
          )
        )
        .isExactlyInstanceOf(EOFException.class)
        .hasMessage("FAIL - OTHER ERROR");
      }
    }
  }


  private String throwingOp() throws IOException {
    throw new IOException("FAIL");
  }
}

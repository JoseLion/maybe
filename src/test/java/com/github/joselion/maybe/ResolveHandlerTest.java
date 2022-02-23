package com.github.joselion.maybe;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INPUT_STREAM;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.github.joselion.maybe.exceptions.WrappingException;
import com.github.joselion.maybe.helpers.UnitTest;
import com.github.joselion.maybe.util.SupplierChecked;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest class ResolveHandlerTest {

  private static final String SUCCESS = "success";

  private static final String ERROR = "error";

  private static final String VALUE = "value";

  private static final String RESOURCE = "resource";

  private static final IOException FAIL_EXCEPTION = new IOException("FAIL");

  private static final UnsupportedOperationException ERROR_EXCEPTION = new UnsupportedOperationException("ERROR");

  private final SupplierChecked<String, IOException> throwingOp = () -> {
    throw FAIL_EXCEPTION;
  };

  private final SupplierChecked<String, IOException> errorOp = () -> {
    throw ERROR_EXCEPTION;
  };

  private final SupplierChecked<String, RuntimeException> okOp = () -> "OK";

  @Nested class doOnError {
    @Nested class when_the_error_is_present {
      @Nested class and_the_error_is_instance_of_the_checked_exception {
        @Test void runs_the_effect() {
          final List<Integer> counter = new ArrayList<>();
          final ResolveHandler<String, IOException> handler = Maybe.fromSupplier(throwingOp)
            .doOnError(error -> {
              assertThat(error)
                .isInstanceOf(IOException.class)
                .hasMessage("FAIL");

              counter.add(1);
            });

          assertThat(counter).containsExactly(1);

          assertThat(handler)
            .extracting(SUCCESS, optional(String.class))
            .isEmpty();

          assertThat(handler)
            .extracting(ERROR, optional(IOException.class))
            .contains(FAIL_EXCEPTION);
        }
      }

      @Nested class and_the_error_is_NOT_instance_of_the_checked_exception {
        @Test void runs_the_effect() {
          final List<Integer> counter = new ArrayList<>();
          final ResolveHandler<String, IOException> handler = Maybe.fromSupplier(errorOp)
            .doOnError(error -> {
              assertThat(error)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("ERROR");

              counter.add(1);
            });

          assertThat(counter).containsExactly(1);

          assertThat(handler)
            .extracting(SUCCESS, optional(String.class))
            .isEmpty();

          assertThat(handler)
            .extracting(ERROR, optional(UnsupportedOperationException.class))
            .contains(ERROR_EXCEPTION);
        }
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void does_NOT_run_the_effect() {
        final ResolveHandler<String, RuntimeException> handler = Maybe.fromSupplier(okOp)
          .doOnError(error -> {
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

  @Nested class onError {
    @Nested class when_the_error_is_present {
      @Nested class and_the_error_is_instance_of_the_checked_exception {
        @Test void applies_the_handler_function() {
          final ResolveHandler<String, IOException> handler = Maybe.fromSupplier(throwingOp)
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
          final ResolveHandler<String, IOException> handler = Maybe.fromSupplier(failingOp)
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
        final ResolveHandler<String, RuntimeException> handler = Maybe.fromSupplier(okOp)
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
          final ResolveHandler<String, IOException> handler = Maybe.fromSupplier(throwingOp)
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
          final ResolveHandler<String, IOException> handler = Maybe.fromSupplier(throwingOp)
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
        final ResolveHandler<String, RuntimeException> handler = Maybe.fromSupplier(okOp)
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

  @Nested class map {
    @Nested class when_the_value_is_present {
      @Test void returns_a_new_handler_applying_the_mapper_function() {
        final ResolveHandler<Integer, ?> handler = ResolveHandler.withSuccess("Hello world!")
          .map(String::length);

        assertThat(handler)
          .extracting(SUCCESS, optional(Integer.class))
          .contains(12);

        assertThat(handler)
          .extracting(ERROR, optional(Exception.class))
          .isEmpty();
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_new_handler_with_the_previous_error() {
        final ResolveHandler<?, IOException> handler = ResolveHandler.withError(FAIL_EXCEPTION)
          .map(Object::toString);

        assertThat(handler)
          .extracting(SUCCESS, optional(String.class))
          .isEmpty();

        assertThat(handler)
          .extracting(ERROR, optional(IOException.class))
          .contains(FAIL_EXCEPTION);
      }
    }

    @Nested class when_neither_the_value_nor_the_error_is_present {
      @Test void returns_an_empty_handler() {
        final ResolveHandler<?, ?> handler = ResolveHandler.withNothing()
          .map(Object::toString);

        assertThat(handler)
          .extracting(SUCCESS, optional(String.class))
          .isEmpty();

        assertThat(handler)
          .extracting(ERROR, optional(Exception.class))
          .isEmpty();
      }
    }
  }

  @Nested class filter {
    @Nested class when_the_value_is_present {
      @Nested class and_the_predicate_matches {
        @Test void returns_a_new_handler_with_the_value() {
          final ResolveHandler<String, ?> handler = ResolveHandler.withSuccess("Hello world!")
            .filter(it -> it.contains("world"));

          assertThat(handler)
            .extracting(SUCCESS, optional(String.class))
            .contains("Hello world!");

          assertThat(handler)
            .extracting(ERROR, optional(Exception.class))
            .isEmpty();
        }
      }

      @Nested class and_the_predicate_does_NOT_match {
        @Test void returns_an_empty_handler() {
          final ResolveHandler<String, ?> handler = ResolveHandler.withSuccess("Hello world!")
            .filter(it -> it.contains("planet"));

          assertThat(handler)
            .extracting(SUCCESS, optional(String.class))
            .isEmpty();

          assertThat(handler)
            .extracting(ERROR, optional(Exception.class))
            .isEmpty();
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_new_handler_with_the_previous_error() {
        final ResolveHandler<?, IOException> handler = ResolveHandler.withError(FAIL_EXCEPTION)
          .filter(Objects::isNull);

        assertThat(handler)
          .extracting(SUCCESS, optional(String.class))
          .isEmpty();

        assertThat(handler)
          .extracting(ERROR, optional(IOException.class))
          .contains(FAIL_EXCEPTION);
      }
    }

    @Nested class when_neither_the_value_nor_the_error_is_present {
      @Test void returns_an_empty_handler() {
        final ResolveHandler<?, ?> handler = ResolveHandler.withNothing()
          .filter(Objects::isNull);

        assertThat(handler)
          .extracting(SUCCESS, optional(String.class))
          .isEmpty();

        assertThat(handler)
          .extracting(ERROR, optional(Exception.class))
          .isEmpty();
      }
    }
  }

  @Nested class cast {
    @Nested class when_the_value_is_present {
      @Nested class and_the_object_can_be_cast {
        @Test void returns_a_new_handler_with_the_cast_value() {
          final Object anyValue = "Hello";
          final ResolveHandler<String, WrappingException> handler = ResolveHandler.withSuccess(anyValue)
            .cast(String.class);

          assertThat(handler)
            .extracting(SUCCESS, optional(String.class))
            .containsInstanceOf(String.class)
            .contains("Hello");

          assertThat(handler)
            .extracting(ERROR, optional(WrappingException.class))
            .isEmpty();
        }
      }

      @Nested class and_the_object_can_NOT_be_cast {
        @Test void returns_a_new_handler_with_the_cast_exception() {
          final Object anyValue = 3;
          final ResolveHandler<String, WrappingException> handler = ResolveHandler.withSuccess(anyValue)
            .cast(String.class);

          assertThat(handler)
            .extracting(SUCCESS, optional(String.class))
            .isEmpty();

          assertThat(handler)
            .extracting(ERROR, optional(WrappingException.class))
            .map(WrappingException::getCause)
            .containsInstanceOf(ClassCastException.class)
            .isNotEmpty();
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_new_handler_with_a_cast_exception() {
        final ResolveHandler<String, WrappingException> handler = ResolveHandler.withError(FAIL_EXCEPTION)
            .cast(String.class);

        assertThat(handler)
          .extracting(SUCCESS, optional(String.class))
          .isEmpty();

        assertThat(handler)
          .extracting(ERROR, optional(WrappingException.class))
          .map(WrappingException::getCause)
          .containsInstanceOf(IOException.class)
          .isNotEmpty();
      }
    }

    @Nested class when_neither_the_value_nor_the_error_is_present {
      @Test void returns_an_empty_handler() {
        final ResolveHandler<?, ?> handler = ResolveHandler.withNothing()
          .cast(String.class);

        assertThat(handler)
          .extracting(SUCCESS, optional(String.class))
          .isEmpty();

        assertThat(handler)
          .extracting(ERROR, optional(Exception.class))
          .isEmpty();
      }
    }
  }

  @Nested class orDefault {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() {
        assertThat(
          Maybe.fromSupplier(okOp)
            .orDefault("OTHER")
        )
        .isEqualTo("OK");
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_the_default_value() {
        assertThat(
          Maybe.fromSupplier(throwingOp)
            .orDefault("OTHER")
        )
        .isEqualTo("OTHER");
      }
    }
  }

  @Nested class orSupplyDefault {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() {
        assertThat(
          Maybe.fromSupplier(okOp)
            .orSupplyDefault(() -> "OTHER")
        )
        .isEqualTo("OK");
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_the_default_value() {
        assertThat(
          Maybe.fromSupplier(throwingOp)
            .orSupplyDefault(() -> "OTHER")
        )
        .isEqualTo("OTHER");
      }
    }
  }

  @Nested class orThrow {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() throws EOFException {
        assertThat(
          Maybe.fromSupplier(okOp).orThrow()
        )
        .isEqualTo("OK");

        assertThat(
          Maybe.fromSupplier(okOp).orThrow(error -> new EOFException(error.getMessage()))
        )
        .isEqualTo("OK");
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void throws_the_error() {
        final ResolveHandler<?, IOException> handler = Maybe.fromSupplier(throwingOp);

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

  @Nested class toMaybe {
    @Nested class when_the_value_is_present {
      @Test void returns_a_maybe_with_the_value() {
        assertThat(
          Maybe.fromSupplier(okOp).toMaybe()
        )
        .extracting(VALUE, optional(String.class))
        .contains("OK");
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_a_maybe_with_nothing() {
        assertThat(
          Maybe.fromSupplier(throwingOp).toMaybe()
        )
        .extracting(VALUE, optional(String.class))
        .isEmpty();
      }
    }
  }

  @Nested class toOptional {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value_wrapped_in_an_optinal() {
        assertThat(Maybe.fromSupplier(okOp).toOptional()).contains("OK");
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_an_empty_optional() {
        assertThat(Maybe.fromSupplier(throwingOp).toOptional()).isEmpty();
      }
    }
  }

  @Nested class mapToResource {
    @Nested class when_the_value_is_present {
      @Test void returns_a_resource_holder_with_the_mapped_value() {
        final ResourceHolder<FileInputStream> resHolder = Maybe.just("./src/test/resources/readTest.txt")
          .resolve(FileInputStream::new)
          .mapToResource(Function.identity());

        assertThat(resHolder)
          .extracting(RESOURCE, optional(FileInputStream.class))
          .containsInstanceOf(FileInputStream.class)
          .get(as(INPUT_STREAM))
          .hasContent("foo");
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void returns_an_empty_resource_holder() {
        final ResourceHolder<FileInputStream> resHolder = Maybe.just("invalidFile.txt")
          .resolve(FileInputStream::new)
          .mapToResource(Function.identity());

        assertThat(resHolder)
          .extracting(RESOURCE, optional(FileInputStream.class))
          .isEmpty();
      }
    }
  }
}

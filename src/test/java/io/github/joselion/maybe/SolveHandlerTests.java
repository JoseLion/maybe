package io.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.INPUT_STREAM;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingFunction;
import io.github.joselion.maybe.util.function.ThrowingSupplier;
import io.github.joselion.testing.Spy;
import io.github.joselion.testing.UnitTest;

@UnitTest class SolveHandlerTests {

  private static final String OK = "OK";

  private static final String OTHER = "OTHER";

  private static final FileSystemException FAIL_EXCEPTION = new FileSystemException("FAIL");

  private final ThrowingSupplier<String, FileSystemException> throwingOp = () -> {
    throw FAIL_EXCEPTION;
  };

  private final ThrowingSupplier<String, RuntimeException> okOp = () -> OK;

  @Nested class from {
    @Nested class when_the_value_is_not_null {
      @Test void returns_a_handler_withThe_value() {
        final var handler = SolveHandler.from(OK);

        assertThat(handler.success()).containsSame(OK);
        assertThat(handler.error()).isEmpty();
      }
    }

    @Nested class when_the_value_is_null {
      @Test void returns_a_handler_with_a_NullPointerException_error() {
        final var handler = SolveHandler.from(null);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(NullPointerException.class)
          .hasMessage("The \"Maybe<T>\" value solved to null");
      }
    }
  }

  @Nested class failure {
    @Nested class when_the_error_is_not_null {
      @Test void returns_a_handler_with_the_error() {
        final var handler = SolveHandler.failure(FAIL_EXCEPTION);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).containsSame(FAIL_EXCEPTION);
      }
    }

    @Nested class when_the_error_is_null {
      @Test void returns_a_handler_with_a_NullPointerException_error() {
        final var handler = SolveHandler.failure(null);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(NullPointerException.class)
          .hasMessage("The \"Maybe<T>\" error was null");
      }
    }
  }

  @Nested class doOnSuccess {
    @Nested class when_the_value_is_present {
      @Test void calls_the_effect_callback() {
        final var consumerSpy = Spy.<String>consumer(v -> { });

        Maybe.from(okOp)
          .doOnSuccess(consumerSpy);

        verify(consumerSpy, times(1)).accept(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Test void never_calls_the_effect_callback() {
        final var consumerSpy = Spy.<String>consumer(v -> { });

        Maybe.from(throwingOp)
          .doOnSuccess(consumerSpy);

        verify(consumerSpy, never()).accept(any());
      }
    }
  }

  @Nested class doOnError {
    @Nested class when_the_error_is_present {
      @Nested class and_the_error_type_is_provided {
        @Nested class and_the_error_is_an_instance_of_the_provided_type {
          @Test void calls_the_effect_callback() {
            final var consumerSpy = Spy.<FileSystemException>consumer(error -> { });

            Maybe.from(throwingOp)
              .doOnError(FileSystemException.class, consumerSpy);

            verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
          }
        }

        @Nested class and_the_error_is_not_an_instance_of_the_provided_type {
          @Test void never_calls_the_effect_callback() {
            final var consumerSpy = Spy.<RuntimeException>consumer(error -> { });

            Maybe.from(throwingOp)
              .doOnError(RuntimeException.class, consumerSpy);

            verify(consumerSpy, never()).accept(any());
          }
        }
      }

      @Nested class and_the_error_type_is_not_provided {
        @Test void calls_the_effect_callback() {
          final var consumerSpy = Spy.<FileSystemException>consumer(error -> { });

          Maybe.from(throwingOp)
            .doOnError(consumerSpy);

          verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
        }
      }
    }

    @Nested class when_the_value_is_present {
      @Test void never_calls_the_effect_callback() {
        final var cunsumerSpy = Spy.<RuntimeException>consumer(error -> { });

        Maybe.from(okOp)
          .doOnError(RuntimeException.class, cunsumerSpy)
          .doOnError(cunsumerSpy);

        verify(cunsumerSpy, never()).accept(any());
      }
    }
  }

  @Nested class catchError {
    @Nested class when_the_error_is_present {
      @Nested class and_the_error_type_is_provided {
        @Nested class and_the_error_is_an_instance_of_the_provided_type {
          @Test void calls_the_handler_function() {
            final var functionSpy = Spy.function((FileSystemException e) -> OK);
            final var handler = Maybe.from(throwingOp)
              .catchError(FileSystemException.class, functionSpy);

            assertThat(handler.success()).contains(OK);
            assertThat(handler.error()).isEmpty();

            verify(functionSpy, times(1)).apply(FAIL_EXCEPTION);
          }
        }

        @Nested class and_the_error_is_not_an_instance_of_the_provided_type {
          @Test void never_calls_the_handler_function() {
            final var functionSpy = Spy.function((AccessDeniedException e) -> OK);
            final var handler = Maybe.from(throwingOp)
              .catchError(AccessDeniedException.class, functionSpy);

            assertThat(handler.success()).isEmpty();
            assertThat(handler.error()).contains(FAIL_EXCEPTION);

            verify(functionSpy, never()).apply(any());
          }
        }
      }

      @Nested class and_the_error_type_is_not_provided {
        @Test void calls_the_handler_function() {
          final var handlerSpy = Spy.function((FileSystemException e) -> OK);
          final var solver = Maybe.from(throwingOp)
            .catchError(handlerSpy);

          assertThat(solver.success()).contains(OK);
          assertThat(solver.error()).isEmpty();

          verify(handlerSpy, times(1)).apply(FAIL_EXCEPTION);
        }
      }
    }

    @Nested class when_the_value_is_present {
      @Test void never_calls_the_handler_function() {
        final var functionSpy = Spy.function((RuntimeException e) -> OK);
        final var solvers = List.of(
          Maybe.from(okOp).catchError(RuntimeException.class, functionSpy),
          Maybe.from(okOp).catchError(functionSpy)
        );

        assertThat(solvers).isNotEmpty().allSatisfy(solver -> {
          assertThat(solver.success()).contains(OK);
          assertThat(solver.error()).isEmpty();
        });

        verify(functionSpy, never()).apply(any());
      }
    }
  }

  @Nested class solve {
    @Nested class when_the_value_is_present {
      @Test void calls_the_solver_callback_and_returns_a_handler() {
        final var solverSpy = Spy.<ThrowingFunction<String, Integer, RuntimeException>>lambda(String::length);
        final var successSpy = Spy.<ThrowingFunction<String, Integer, RuntimeException>>lambda(String::length);
        final var errorSpy = Spy.<ThrowingFunction<RuntimeException, Integer, RuntimeException>>lambda(e -> -1);
        final var handlers = List.of(
          Maybe.from(okOp).solve(solverSpy),
          Maybe.from(okOp).solve(successSpy, errorSpy)
        );

        assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
          assertThat(handler.success()).contains(OK.length());
          assertThat(handler.error()).isEmpty();
        });

        verify(solverSpy, times(1)).apply(OK);
        verify(successSpy, times(1)).apply(OK);
        verify(errorSpy, never()).apply(any());
      }
    }

    @Nested class when_the_error_is_present {
      @Nested class and_the_error_solver_is_not_provided {
        @Test void never_calls_the_solver_callback_and_returns_a_handler_with_the_error() {
          final var successSpy = Spy.<ThrowingFunction<String, Integer, RuntimeException>>lambda(String::length);
          final var handler = Maybe.from(throwingOp)
            .solve(successSpy);

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error())
            .get(THROWABLE)
            .isExactlyInstanceOf(FAIL_EXCEPTION.getClass())
            .isEqualTo(FAIL_EXCEPTION);

          verify(successSpy, never()).apply(any());
        }
      }

      @Nested class and_the_error_solver_is_provided {
        @Test void call_only_the_error_callback_and_returns_a_new_effect_handler() {
          final var successSpy = Spy.<ThrowingFunction<String, Integer, RuntimeException>>lambda(String::length);
          final var errorSpy = Spy.<ThrowingFunction<FileSystemException, Integer, RuntimeException>>lambda(e -> -1);
          final var handler = Maybe.from(throwingOp)
            .solve(successSpy, errorSpy);

          assertThat(handler.success()).contains(-1);
          assertThat(handler.error()).isEmpty();

          verify(successSpy, never()).apply(any());
          verify(errorSpy, times(1)).apply(FAIL_EXCEPTION);
        }
      }
    }
  }

  @Nested class resolve {
    @Test void calls_solve() {
      final var identity = ThrowingFunction.identity();
      final var handler = spy(Maybe.from(okOp));
      handler.resolve(identity, identity);
      handler.resolve(identity);

      verify(handler).solve(identity);
      verify(handler).solve(identity, identity);
    }
  }

  @Nested class effect {
    @Nested class when_the_value_is_present {
      @Test void calls_the_solver_callback_and_returns_a_handler() throws FileSystemException {
        final var effectSpy = Spy.<ThrowingConsumer<String, FileSystemException>>lambda(v -> throwingOp.get());
        final var successSpy = Spy.<ThrowingConsumer<String, FileSystemException>>lambda(v -> throwingOp.get());
        final var errorSpy = Spy.<ThrowingConsumer<RuntimeException, FileSystemException>>lambda(err -> { });
        final var handler = Maybe.from(okOp);
        final var newHandlers = List.of(
          handler.effect(effectSpy),
          handler.effect(successSpy, errorSpy)
        );

        assertThat(newHandlers).isNotEmpty().allSatisfy(newHandler -> {
          assertThat(newHandler.error()).contains(FAIL_EXCEPTION);
        });

        verify(effectSpy, times(1)).accept(OK);
        verify(successSpy, times(1)).accept(OK);
        verify(errorSpy, never()).accept(any());
      }
    }

    @Nested class when_the_error_is_present {
      @Nested class and_the_error_callback_is_provided {
        @Test void calls_only_the_error_callback_and_returns_a_handler() throws FileSystemException {
          final var successSpy = Spy.<ThrowingConsumer<String, FileSystemException>>lambda(v -> { });
          final var errorSpy = Spy.<ThrowingConsumer<FileSystemException, FileSystemException>>lambda(
            err -> throwingOp.get()
          );
          final var handler = Maybe.from(throwingOp);
          final var newHandler = handler.effect(successSpy, errorSpy);

          assertThat(newHandler.error()).contains(FAIL_EXCEPTION);

          verify(successSpy, never()).accept(any());
          verify(errorSpy, times(1)).accept(FAIL_EXCEPTION);
        }
      }

      @Nested class and_the_error_callback_is_not_provided {
        @Test void never_calls_the_effect_callback_and_returns_a_handler_with_the_error() throws FileSystemException {
          final var effectSpy = Spy.<ThrowingConsumer<String, FileSystemException>>lambda(v -> throwingOp.get());
          final var handler = Maybe.from(throwingOp);
          final var newHandler = handler.effect(effectSpy);

          assertThat(newHandler.error())
            .get(THROWABLE)
            .isExactlyInstanceOf(FAIL_EXCEPTION.getClass())
            .isEqualTo(FAIL_EXCEPTION);

          verify(effectSpy, never()).accept(any());
        }
      }
    }
  }

  @Nested class runEffect {
    @Test void calls_effect() {
      final var onSuccess = Spy.<ThrowingConsumer<String, RuntimeException>>lambda(x -> { });
      final var onError = Spy.<ThrowingConsumer<RuntimeException, RuntimeException>>lambda(x -> { });
      final var maybe = spy(Maybe.from(okOp));
      maybe.runEffect(onSuccess);
      maybe.runEffect(onSuccess, onError);

      verify(maybe).effect(onSuccess, onError);
      verify(maybe).effect(onSuccess);
    }
  }

  @Nested class map {
    @Nested class when_the_value_is_present {
      @Test void returns_a_handler_applying_the_mapper_function() {
        final var handler = SolveHandler.from("Hello world!")
          .map(String::length);

        assertThat(handler.success()).contains(12);

        assertThat(handler.error()).isEmpty();
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_handler_with_the_previous_error() {
        final var handler = SolveHandler.failure(FAIL_EXCEPTION)
          .map(Object::toString);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).contains(FAIL_EXCEPTION);
      }
    }
  }

  @Nested class cast {
    @Nested class when_the_value_is_present {
      @Nested class and_the_object_can_be_cast {
        @Test void returns_a_handler_with_the_cast_value() {
          final var anyValue = (Object) "Hello";
          final var handler = SolveHandler.from(anyValue)
            .cast(String.class);

          assertThat(handler.success()).contains("Hello");
          assertThat(handler.error()).isEmpty();
        }
      }

      @Nested class and_the_object_can_not_be_cast {
        @Test void returns_a_handler_with_the_cast_exception() {
          final var handler = SolveHandler.from(3)
            .cast(String.class);

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error())
            .get(THROWABLE)
            .isExactlyInstanceOf(ClassCastException.class)
            .hasMessage("Cannot cast java.lang.Integer to java.lang.String");
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_handler_with_a_cast_exception() {
        final var handler = SolveHandler.failure(FAIL_EXCEPTION)
            .cast(String.class);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(ClassCastException.class)
          .hasMessage(FAIL_EXCEPTION.getMessage());
      }
    }
  }

  @Nested class orElse {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() {
        final var handler = Maybe.from(okOp);

        assertThat(handler.orElse(OTHER)).isEqualTo(OK);
        assertThat(handler.orElse(RuntimeException::getMessage)).isEqualTo(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_the_provided_value() {
        final var handler = Maybe.from(throwingOp);

        assertThat(handler.orElse(OTHER)).isEqualTo(OTHER);
        assertThat(handler.orElse(FileSystemException::getMessage)).isEqualTo(FAIL_EXCEPTION.getMessage());
      }
    }
  }

  @Nested class orElseGet {
    @Nested class when_the_value_is_present {
      @Test void never_evaluates_the_supplier_and_returns_the_value() {
        final var supplierSpy = Spy.supplier(() -> OTHER);
        final var handler = Maybe.from(okOp);

        assertThat(handler.orElseGet(supplierSpy)).isEqualTo(OK);

        verify(supplierSpy, never()).get();
      }
    }

    @Nested class when_the_error_is_present {
      @Test void evaluates_the_supplier_and_returns_the_produced_value() {
        final var supplierSpy = Spy.supplier(() -> OTHER);
        final var handler = Maybe.from(throwingOp);

        assertThat(handler.orElseGet(supplierSpy)).isEqualTo(OTHER);

        verify(supplierSpy, times(1)).get();
      }
    }
  }

  @Nested class orNull {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() {
        final var handler = Maybe.from(okOp);

        assertThat(handler.orNull()).isEqualTo(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_null() {
        final var handler = Maybe.from(throwingOp);

        assertThat(handler.orNull()).isNull();
      }
    }
  }

  @Nested class orThrow {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() throws FileSystemException {
        final var functionSpy = Spy.function((RuntimeException error) -> FAIL_EXCEPTION);
        final var handler = Maybe.from(okOp);

        assertThat(handler.orThrow()).isEqualTo(OK);
        assertThat(handler.orThrow(functionSpy)).isEqualTo(OK);

        verify(functionSpy, never()).apply(any());
      }
    }

    @Nested class when_the_error_is_present {
      @Test void throws_the_error() {
        final var anotherError = new RuntimeException(OTHER);
        final var functionSpy = Spy.function((FileSystemException error) -> anotherError);
        final var handler = Maybe.from(throwingOp);

        assertThatThrownBy(handler::orThrow).isEqualTo(FAIL_EXCEPTION);
        assertThatThrownBy(() -> handler.orThrow(functionSpy)).isEqualTo(anotherError);

        verify(functionSpy, times(1)).apply(FAIL_EXCEPTION);
      }
    }
  }

  @Nested class toMaybe {
    @Nested class when_the_value_is_present {
      @Test void returns_a_maybe_with_the_value() {
        assertThat(Maybe.from(okOp).toMaybe().value())
          .contains(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_an_empty_Maybe_() {
        assertThat(Maybe.from(throwingOp).toMaybe().value())
          .isEmpty();
      }
    }
  }

  @Nested class toOptional {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value_wrapped_in_an_optinal() {
        assertThat(Maybe.from(okOp).toOptional()).contains(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_an_empty_optional() {
        assertThat(Maybe.from(throwingOp).toOptional()).isEmpty();
      }
    }
  }

  @Nested class toEither {
    @Nested class when_the_value_is_present {
      @Test void returns_an_Either_with_the_value_on_its_right_side() {
        final var either = Maybe.from(okOp).toEither();

        assertThat(either.isLeft()).isFalse();
        assertThat(either.isRight()).isTrue();
        assertThat(either.leftOrNull()).isNull();
        assertThat(either.rightOrNull()).isEqualTo(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_an_Either_with_the_error_on_its_left_side() {
        final var either = Maybe.from(throwingOp).toEither();

        assertThat(either.isLeft()).isTrue();
        assertThat(either.isRight()).isFalse();
        assertThat(either.leftOrNull()).isEqualTo(FAIL_EXCEPTION);
        assertThat(either.rightOrNull()).isNull();
      }
    }
  }

  @Nested class mapToResource {
    @Nested class when_the_resource_is_present {
      @Test void returns_a_resource_holder_with_the_mapped_value() {
        final var path = "./src/test/resources/readTest.txt";
        final var holder = Maybe.of(path)
          .solve(FileInputStream::new)
          .mapToResource(Function.identity());

        assertThat(holder.resource())
          .get(INPUT_STREAM)
          .hasContent("foo");
        assertThat(holder.error()).isEmpty();
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_resource_holder_with_the_propagated_error() {
        final var holder = Maybe.of("invalidFile.txt")
          .solve(FileInputStream::new)
          .mapToResource(Function.identity());

        assertThat(holder.resource()).isEmpty();
        assertThat(holder.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(FileNotFoundException.class)
          .hasMessageStartingWith("invalidFile.txt");
      }
    }
  }

  @Nested class solveResource {
    @Nested class when_the_value_is_present {
      @Nested class and_the_solver_does_not_throw {
        @Test void returns_a_CloseableHandler_with_the_resource() {
          final var path = "./src/test/resources/readTest.txt";
          final var holder = Maybe
            .of(path)
            .solve(ThrowingFunction.identity())
            .solveResource(FileInputStream::new);

          assertThat(holder.resource())
            .isPresent()
            .containsInstanceOf(FileInputStream.class)
            .get()
            .asInstanceOf(INPUT_STREAM)
            .hasContent("foo");
          assertThat(holder.error()).isEmpty();
        }
      }

      @Nested class and_the_solver_throws {
        @Test void returns_a_CloseableHandler_with_the_thrown_exception() {
          final var holder = Maybe
            .of("invalid.txt")
            .solve(ThrowingFunction.identity())
            .solveResource(FileInputStream::new);

          assertThat(holder.resource()).isEmpty();
          assertThat(holder.error())
            .isPresent()
            .containsInstanceOf(FileNotFoundException.class);
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_CloseableHandler_with_the_propagated_error() {
        final var holder = Maybe
          .from(throwingOp)
          .solveResource(FileInputStream::new);

        assertThat(holder.resource()).isEmpty();
        assertThat(holder.error())
          .isPresent()
          .containsInstanceOf(FileSystemException.class);
      }
    }
  }
}

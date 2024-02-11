package io.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.InstanceOfAssertFactories.INPUT_STREAM;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.maybe.helpers.Commons;
import io.github.joselion.maybe.util.function.ThrowingFunction;
import io.github.joselion.maybe.util.function.ThrowingSupplier;
import io.github.joselion.testing.Spy;
import io.github.joselion.testing.UnitTest;

@UnitTest class SolveHandlerTest {

  private static final String OK = "OK";

  private static final String OTHER = "OTHER";

  private static final FileSystemException FAILURE = new FileSystemException("FAIL");

  private final ThrowingSupplier<String, FileSystemException> throwingOp = () -> {
    throw FAILURE;
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
        final var handler = SolveHandler.failure(FAILURE);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).containsSame(FAILURE);
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

        verify(consumerSpy).accept(OK);
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

            Maybe
              .from(throwingOp)
              .doOnError(FileSystemException.class, consumerSpy);

            verify(consumerSpy).accept(FAILURE);
          }
        }

        @Nested class and_the_error_is_not_an_instance_of_the_provided_type {
          @Test void never_calls_the_effect_callback() {
            final var consumerSpy = Spy.<RuntimeException>consumer(error -> { });

            Maybe
              .from(throwingOp)
              .doOnError(RuntimeException.class, consumerSpy);

            verify(consumerSpy, never()).accept(any());
          }
        }
      }

      @Nested class and_the_error_type_is_not_provided {
        @Nested class and_the_error_matches_the_type_of_the_arg {
          @Test void calls_the_effect_callback() {
            final var consumerSpy = Spy.<Throwable>consumer(error -> { });

            Maybe
              .from(throwingOp)
              .doOnError(consumerSpy);

            verify(consumerSpy).accept(FAILURE);
          }
        }

        @Nested class and_the_error_does_not_match_the_type_of_the_arg {
          @Test void calls_the_effect_callback() {
            final var consumerSpy = Spy.<Throwable>consumer(error -> { });

            Maybe
              .from(throwingOp)
              .solve(Commons::cast)
              .doOnError(consumerSpy);

            verify(consumerSpy).accept(FAILURE);
          }
        }
      }
    }

    @Nested class when_the_value_is_present {
      @Test void never_calls_the_effect_callback() {
        final var runtimeSpy = Spy.<RuntimeException>consumer(error -> { });
        final var throwableSpy = Spy.<Throwable>consumer(error -> { });

        Maybe
          .from(okOp)
          .doOnError(RuntimeException.class, runtimeSpy)
          .doOnError(throwableSpy);

        verify(runtimeSpy, never()).accept(any());
        verify(throwableSpy, never()).accept(any());
      }
    }
  }

  @Nested class catchError {
    @Nested class when_the_error_is_present {
      @Nested class and_the_error_type_is_provided {
        @Nested class and_the_error_is_an_instance_of_the_provided_type {
          @Test void calls_the_handler_function() {
            final var functionSpy = Spy.function((FileSystemException e) -> OK);
            final var handler = Maybe.from(throwingOp).catchError(FileSystemException.class, functionSpy);

            assertThat(handler.success()).contains(OK);
            assertThat(handler.error()).isEmpty();

            verify(functionSpy).apply(FAILURE);
          }
        }

        @Nested class and_the_error_is_not_an_instance_of_the_provided_type {
          @Test void never_calls_the_handler_function() {
            final var functionSpy = Spy.function((AccessDeniedException e) -> OK);
            final var handler = Maybe.from(throwingOp).catchError(AccessDeniedException.class, functionSpy);

            assertThat(handler.success()).isEmpty();
            assertThat(handler.error()).contains(FAILURE);

            verify(functionSpy, never()).apply(any());
          }
        }
      }

      @Nested class and_the_error_type_is_not_provided {
        @Nested class and_the_error_matches_the_type_of_the_arg {
          @Test void calls_the_handler_function() {
            final var handlerSpy = Spy.function((Throwable e) -> OK);
            final var solver = Maybe.from(throwingOp)
              .catchError(handlerSpy);

            assertThat(solver.success()).contains(OK);
            assertThat(solver.error()).isEmpty();

            verify(handlerSpy).apply(FAILURE);
          }
        }

        @Nested class and_the_error_does_not_match_the_type_of_the_arg {
          @Test void calls_the_handler_function() {
            final var handlerSpy = Spy.function((Throwable e) -> OK);
            final var solver = Maybe.from(throwingOp)
              .solve(Commons::cast)
              .catchError(handlerSpy);

            assertThat(solver.success()).contains(OK);
            assertThat(solver.error()).isEmpty();

            verify(handlerSpy).apply(FAILURE);
          }
        }
      }
    }

    @Nested class when_the_value_is_present {
      @Test void never_calls_the_handler_function() {
        final var runtimeSpy = Spy.function((RuntimeException e) -> OK);
        final var throwableSpy = Spy.function((Throwable e) -> OK);
        final var solvers = List.of(
          Maybe.from(okOp).catchError(RuntimeException.class, runtimeSpy),
          Maybe.from(okOp).catchError(throwableSpy)
        );

        assertThat(solvers).isNotEmpty().allSatisfy(solver -> {
          assertThat(solver.success()).contains(OK);
          assertThat(solver.error()).isEmpty();
        });

        verify(runtimeSpy, never()).apply(any());
        verify(throwableSpy, never()).apply(any());
      }
    }
  }

  @Nested class onErrorSolve {
    @Nested class when_the_error_is_present {
      @Nested class and_the_error_type_is_provided {
        @Nested class and_the_error_is_an_instance_of_the_provided_type {
          @Test void calls_the_solver_and_returns_a_handler() {
            final var solverSpy = Spy.throwingFunction(FileSystemException::getMessage);
            final var handler = Maybe.from(throwingOp).onErrorSolve(FileSystemException.class, solverSpy);

            assertThat(handler.success()).contains("FAIL");
            assertThat(handler.error()).isEmpty();

            verify(solverSpy).apply(FAILURE);
          }
        }

        @Nested class and_the_error_is_not_an_instance_of_the_provided_type {
          @Test void never_calls_the_solver_and_returns_a_handler_with_the_error() {
            final var solverSpy = Spy.throwingFunction(FileSystemException::getMessage);
            final var handler = Maybe.from(throwingOp).onErrorSolve(AccessDeniedException.class, solverSpy);

            assertThat(handler.success()).isEmpty();
            assertThat(handler.error()).containsInstanceOf(FileSystemException.class);

            verify(solverSpy, never()).apply(FAILURE);
          }
        }
      }

      @Nested class and_the_error_type_is_not_provided {
        @Nested class and_the_error_matches_the_type_of_the_arg {
          @Test void calls_the_solver_callback_and_returns_a_handler() {
            final var solverSpy = Spy.throwingFunction(Throwable::getMessage);
            final var handler = Maybe.from(throwingOp).onErrorSolve(solverSpy);

            assertThat(handler.success()).contains("FAIL");
            assertThat(handler.error()).isEmpty();

            verify(solverSpy).apply(FAILURE);
          }
        }

        @Nested class and_the_error_does_not_match_the_type_of_the_arg {
          @Test void calls_the_solver_callback_and_returns_a_handler() {
            final var solverSpy = Spy.throwingFunction(Throwable::getMessage);
            final var handler = Maybe.from(throwingOp).solve(Commons::cast).onErrorSolve(solverSpy);

            assertThat(handler.success()).contains("FAIL");
            assertThat(handler.error()).isEmpty();

            verify(solverSpy).apply(FAILURE);
          }
        }
      }
    }

    @Nested class when_the_value_is_present {
      @Test void never_calls_the_solver_and_returns_a_handler_with_the_value() {
        final var runtimeSpy = Spy.throwingFunction(RuntimeException::getMessage);
        final var throwableSpy = Spy.throwingFunction(Throwable::getMessage);
        final var handlers = List.of(
          Maybe.from(okOp).onErrorSolve(throwableSpy),
          Maybe.from(okOp).onErrorSolve(RuntimeException.class, runtimeSpy)
        );

        assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
          assertThat(handler.success()).contains(OK);
          assertThat(handler.error()).isEmpty();
        });

        verify(runtimeSpy, never()).apply(any());
        verify(throwableSpy, never()).apply(any());
      }
    }
  }

  @Nested class solve {
    @Nested class when_the_value_is_present {
      @Test void calls_only_the_success_callback_and_returns_a_handler() {
        final var solverSpy = Spy.throwingFunction(String::length);
        final var successSpy = Spy.throwingFunction(String::length);
        final var errorSpy = Spy.throwingFunction((Throwable e) -> -1);
        final var handlers = List.of(
          Maybe.from(okOp).solve(solverSpy),
          Maybe.from(okOp).solve(successSpy, errorSpy)
        );

        assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
          assertThat(handler.success()).contains(OK.length());
          assertThat(handler.error()).isEmpty();
        });

        verify(solverSpy).apply(OK);
        verify(successSpy).apply(OK);
        verify(errorSpy, never()).apply(any());
      }
    }

    @Nested class when_the_error_is_present {
      @Nested class and_the_error_solver_is_not_provided {
        @Test void never_calls_the_success_callback_and_returns_a_handler_with_the_error() {
          final var successSpy = Spy.throwingFunction(String::length);
          final var handler = Maybe
            .from(throwingOp)
            .solve(successSpy);

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error())
            .get(THROWABLE)
            .isExactlyInstanceOf(FAILURE.getClass())
            .isEqualTo(FAILURE);

          verify(successSpy, never()).apply(any());
        }
      }

      @Nested class and_the_error_solver_is_provided {
        @Nested class and_the_error_matches_the_type_of_the_arg {
          @Test void call_only_the_error_callback_and_returns_a_new_effect_handler() {
            final var successSpy = Spy.throwingFunction(String::length);
            final var errorSpy = Spy.throwingFunction((Throwable e) -> -1);
            final var handler = Maybe
              .from(throwingOp)
              .solve(successSpy, errorSpy);

            assertThat(handler.success()).contains(-1);
            assertThat(handler.error()).isEmpty();

            verify(successSpy, never()).apply(any());
            verify(errorSpy).apply(FAILURE);
          }
        }

        @Nested class and_the_error_does_not_match_the_type_of_the_arg {
          @Test void call_only_the_error_callback_and_returns_a_new_effect_handler() {
            final var successSpy = Spy.throwingFunction(String::length);
            final var errorSpy = Spy.throwingFunction((Throwable e) -> -1);
            final var handler = Maybe
              .from(throwingOp)
              .solve(Commons::<String>cast)
              .solve(successSpy, errorSpy);

            assertThat(handler.success()).contains(-1);
            assertThat(handler.error()).isEmpty();

            verify(successSpy, never()).apply(any());
            verify(errorSpy).apply(FAILURE);
          }
        }
      }
    }
  }

  @Nested class effect {
    @Nested class when_the_value_is_present {
      @Test void calls_the_solver_callback_and_returns_a_handler() throws FileSystemException {
        final var effectSpy = Spy.throwingConsumer((String v) -> throwingOp.get());
        final var successSpy = Spy.throwingConsumer((String v) -> throwingOp.get());
        final var errorSpy = Spy.<Throwable, FileSystemException>throwingConsumer(err -> { });
        final var handler = Maybe.from(okOp);
        final var newHandlers = List.of(
          handler.effect(effectSpy),
          handler.effect(successSpy, errorSpy)
        );

        assertThat(newHandlers).isNotEmpty().allSatisfy(newHandler -> {
          assertThat(newHandler.error()).contains(FAILURE);
        });

        verify(effectSpy).accept(OK);
        verify(successSpy).accept(OK);
        verify(errorSpy, never()).accept(any());
      }
    }

    @Nested class when_the_error_is_present {
      @Nested class and_the_error_callback_is_not_provided {
        @Test void never_calls_the_effect_callback_and_returns_a_handler_with_the_error() throws FileSystemException {
          final var effectSpy = Spy.throwingConsumer((String v) -> throwingOp.get());
          final var handler = Maybe.from(throwingOp).effect(effectSpy);

          assertThat(handler.error())
            .get(THROWABLE)
            .isExactlyInstanceOf(FAILURE.getClass())
            .isEqualTo(FAILURE);

          verify(effectSpy, never()).accept(any());
        }
      }

      @Nested class and_the_error_callback_is_provided {
        @Nested class and_the_error_matches_the_type_of_the_arg {
          @Test void calls_only_the_error_callback_and_returns_a_handler() throws FileSystemException {
            final var successSpy = Spy.throwingConsumer(v -> { });
            final var errorSpy = Spy.throwingConsumer((Throwable err) -> throwingOp.get());
            final var handler = Maybe.from(throwingOp).effect(successSpy, errorSpy);

            assertThat(handler.error()).contains(FAILURE);

            verify(successSpy, never()).accept(any());
            verify(errorSpy).accept(FAILURE);
          }
        }

        @Nested class and_the_error_does_not_match_the_type_of_the_arg {
          @Test void calls_only_the_error_callback_and_returns_a_handler() throws FileSystemException {
            final var successSpy = Spy.throwingConsumer(v -> { });
            final var errorSpy = Spy.throwingConsumer((Throwable err) -> throwingOp.get());
            final var handler = Maybe.from(throwingOp).solve(Commons::cast).effect(successSpy, errorSpy);

            assertThat(handler.error()).contains(FAILURE);

            verify(successSpy, never()).accept(any());
            verify(errorSpy).accept(FAILURE);
          }
        }
      }
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
        final var handler = SolveHandler.failure(FAILURE)
          .map(Object::toString);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).contains(FAILURE);
      }
    }
  }

  @Nested class flatMap {
    @Nested class when_the_value_is_present {
      @Test void returns_a_handler_applying_the_mapper_function() {
        final var handler = SolveHandler.from("Hello world!")
          .flatMap(x -> Maybe.of(x.length()));

        assertThat(handler.success()).contains(12);
        assertThat(handler.error()).isEmpty();
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_handler_with_the_previous_error() {
        final var handler = SolveHandler.failure(FAILURE)
          .flatMap(x -> Maybe.of(x.toString()));

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).contains(FAILURE);
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
          final var handler = SolveHandler.from(3).cast(String.class);

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error())
            .get(THROWABLE)
            .isExactlyInstanceOf(ClassCastException.class)
            .hasMessage("Cannot cast java.lang.Integer to java.lang.String");
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_handler_with_the_error() {
        final var handler = SolveHandler.failure(FAILURE).cast(String.class);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).get().isSameAs(FAILURE);
      }
    }
  }

  @Nested class orElse {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() {
        final var handler = Maybe.from(okOp);

        assertThat(handler.orElse(OTHER)).isEqualTo(OK);
        assertThat(handler.orElse(Throwable::getMessage)).isEqualTo(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Nested class and_the_overload_is_a_fallback {
        @Test void returns_the_fallback_value() {
          final var handler = Maybe.from(throwingOp);

          assertThat(handler.orElse(OTHER)).isEqualTo(OTHER);
        }
      }

      @Nested class and_the_overload_is_a_mapper {
        @Nested class and_the_error_matches_the_type_of_the_arg {
          @Test void returns_the_mapped_value() {
            final var handler = Maybe.from(throwingOp);

            assertThat(handler.orElse(Throwable::getMessage)).isEqualTo(FAILURE.getMessage());
          }
        }

        @Nested class and_the_error_does_not_match_the_type_of_the_arg {
          @Test void returns_the_mapped_value() {
            final var handler = Maybe.from(throwingOp).solve(Commons::cast);

            assertThat(handler.orElse(Throwable::getMessage)).isEqualTo(FAILURE.getMessage());
          }
        }
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

        verify(supplierSpy).get();
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
        final var functionSpy = Spy.function((Throwable error) -> FAILURE);
        final var handler = Maybe.from(okOp);

        assertThat(handler.orThrow()).isEqualTo(OK);
        assertThat(handler.orThrow(functionSpy)).isEqualTo(OK);

        verify(functionSpy, never()).apply(any());
      }
    }

    @Nested class when_the_error_is_present {
      @Nested class and_a_mapper_function_is_not_provided {
        @Test void throws_the_error_present_in_the_handler() {
          final var handler = Maybe.from(throwingOp);

          assertThatCode(handler::orThrow).isEqualTo(FAILURE);
        }
      }

      @Nested class and_a_mapper_function_is_provided {
        @Nested class and_the_error_matches_the_type_of_the_arg {
          @Test void throws_the_error_produced_by_the_function() {
            final var anotherError = new RuntimeException(OTHER);
            final var functionSpy = Spy.function((Throwable error) -> anotherError);
            final var handler = Maybe.from(throwingOp);

            assertThatCode(() -> handler.orThrow(functionSpy)).isEqualTo(anotherError);

            verify(functionSpy).apply(FAILURE);
          }
        }

        @Nested class and_the_error_does_not_match_the_type_of_the_arg {
          @Test void throws_the_error_produced_by_the_function() {
            final var anotherError = new RuntimeException(OTHER);
            final var handler = Maybe.from(throwingOp).solve(Commons::cast);

            assertThat(handler.success()).isEmpty();
            assertThat(handler.error()).isPresent();

            assertThatCode(() -> handler.orThrow(x -> anotherError)).isEqualTo(anotherError);
          }
        }
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
        assertThat(either.leftOrNull()).isEqualTo(FAILURE);
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

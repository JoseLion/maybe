package io.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.INPUT_STREAM;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.maybe.util.Either;
import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingFunction;
import io.github.joselion.maybe.util.function.ThrowingRunnable;
import io.github.joselion.maybe.util.function.ThrowingSupplier;
import io.github.joselion.testing.Spy;
import io.github.joselion.testing.UnitTest;

@UnitTest class MaybeTest {

  private static final String OK = "OK";

  private static final IOException FAILURE = new IOException("FAIL");

  private final ThrowingFunction<String, String, IOException> failFunction = val -> {
    throw FAILURE;
  };

  private final ThrowingSupplier<String, IOException> failSupplier = () -> {
    throw FAILURE;
  };

  private final ThrowingConsumer<String, IOException> failConsumer = it -> {
    throw FAILURE;
  };

  private final ThrowingRunnable<IOException> failRunnable = () -> {
    throw FAILURE;
  };

  @Nested class of {
    @Nested class when_the_value_is_not_an_Optional {
      @Nested class when_a_value_is_passed {
        @Test void returns_a_Maybe_wrapping_the_value() {
          final var maybe = Maybe.of(OK);

          assertThat(maybe.value()).contains(OK);
        }
      }

      @Nested class when_null_is_passed {
        @Test void returns_an_empty_Maybe() {
          final var maybe = Maybe.of(null);

          assertThat(maybe.value()).isEmpty();
        }
      }
    }

    @Nested class when_the_value_is_an_Optional {
      @Nested class when_the_optional_has_a_value {
        @Test void returns_a_Maybe_wrapping_the_value() {
          final var maybe = Maybe.of(Optional.of(OK));

          assertThat(maybe.value()).contains(OK);
        }
      }

      @Nested class when_the_optional_is_empty {
        @Test void returns_an_empty_Maybe() {
          final var maybe = Maybe.of(Optional.empty());

          assertThat(maybe.value()).isEmpty();
        }
      }
    }
  }

  @Nested class empty {
    @Test void returns_an_empty_Maybe() {
      final var maybe = Maybe.empty();

      assertThat(maybe.value()).isEmpty();
    }
  }

  @Nested class from {
    @Nested class when_a_value_is_provided {
      @Nested class and_the_operation_succeeds {
        @Test void returns_a_handler_with_the_value() throws IOException {
          final var supplierSpy = Spy.throwingSupplier(() -> OK);
          final var handler = Maybe.from(supplierSpy);

          assertThat(handler.success()).contains(OK);
          assertThat(handler.error()).isEmpty();

          verify(supplierSpy).get();
        }
      }

      @Nested class and_the_operation_fails {
        @Test void returns_a_handler_with_the_error() throws IOException {
          final var supplierSpy = Spy.lambda(failSupplier);
          final var handler = Maybe.from(supplierSpy);

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error()).contains(FAILURE);

          verify(supplierSpy).get();
        }
      }
    }

    @Nested class when_an_effect_is_passed {
      @Nested class and_the_operation_succeeds {
        @Test void returns_an_empty_handler() {
          final var runnableSpy = Spy.throwingRunnable(() -> { });
          final var handler = Maybe.from(runnableSpy);

          assertThat(handler.error()).isEmpty();

          verify(runnableSpy).run();
        }
      }

      @Nested class and_the_operation_fails {
        @Test void returns_a_handler_with_the_error() throws IOException {
          final var runnableSpy = Spy.lambda(failRunnable);
          final var handler = Maybe.from(runnableSpy);

          assertThat(handler.error()).contains(FAILURE);

          verify(runnableSpy).run();
        }
      }
    }
  }

  @Nested class partial {
    @Nested class when_a_function_is_provided {
      @Test void returns_a_function_that_takes_a_value_and_returns_a_solve_handler() throws IOException {
        final var successSpy = Spy.throwingFunction(String::length);
        final var failureSpy = Spy.lambda(failFunction);

        assertThat(Maybe.partial(successSpy).apply(OK))
          .isInstanceOf(SolveHandler.class)
          .extracting(SolveHandler::success, optional(Integer.class))
          .contains(OK.length());
        assertThat(Maybe.partial(failureSpy).apply(OK))
          .isInstanceOf(SolveHandler.class)
          .extracting(SolveHandler::error, optional(IOException.class))
          .contains(FAILURE);

        verify(successSpy).apply(OK);
        verify(failureSpy).apply(OK);
      }
    }

    @Nested class when_a_consumer_is_provided {
      @Test void returns_a_function_that_takes_a_value_and_returns_an_effect_handler() throws IOException {
        final var successSpy = Spy.throwingConsumer(v -> { });
        final var failureSpy = Spy.lambda(failConsumer);

        assertThat(Maybe.partial(successSpy).apply(OK))
          .isInstanceOf(EffectHandler.class)
          .extracting(EffectHandler::error, optional(RuntimeException.class))
          .isEmpty();

        assertThat(Maybe.partial(failureSpy).apply(OK))
          .isInstanceOf(EffectHandler.class)
          .extracting(EffectHandler::error, optional(IOException.class))
          .contains(FAILURE);

        verify(successSpy).accept(OK);
        verify(failureSpy).accept(OK);
      }
    }
  }

  @Nested class withResource {
    @Test void returns_the_CloseableHandler_with_the_resource() throws FileNotFoundException, IOException {
      try (var fis = new FileInputStream("./src/test/resources/readTest.txt")) {
        final var holder = Maybe.withResource(fis);

        assertThat(holder.resource())
          .isPresent()
          .containsInstanceOf(FileInputStream.class)
          .containsSame(fis);
        assertThat(holder.error()).isEmpty();
      }
    }
  }

  @Nested class solveResource {
    @Nested class and_the_solver_does_not_throw {
      @Test void returns_a_CloseableHandler_with_the_resource() throws FileNotFoundException, IOException {
        final var path = "./src/test/resources/readTest.txt";
        final var holder = Maybe.solveResource(() -> new FileInputStream(path));

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
        final var holder = Maybe.solveResource(() -> new FileInputStream("invalid.txt"));

        assertThat(holder.resource()).isEmpty();
        assertThat(holder.error())
          .isPresent()
          .containsInstanceOf(FileNotFoundException.class);
      }
    }
  }

  @Nested class map {
    @Nested class when_the_value_is_present {
      @Test void maps_the_value_with_the_passed_function() {
        final var maybe = Maybe.of(OK).map(String::length);

        assertThat(maybe.value()).contains(2);
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void returns_an_empty_Maybe() {
        final var maybe = Maybe.<String>empty().map(String::length);

        assertThat(maybe.value()).isEmpty();
      }
    }
  }

  @Nested class flatMap {
    @Nested class when_the_value_is_present {
      @Test void maps_the_value_with_the_passed_maybe_function() {
        final var maybe = Maybe.of(OK)
          .flatMap(str -> Maybe.of(str.length()));

        assertThat(maybe.value()).contains(2);
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void returns_an_empty_Maybe() {
        final var maybe = Maybe.<String>empty()
          .flatMap(str -> Maybe.of(str.length()));

        assertThat(maybe.value()).isEmpty();
      }
    }
  }

  @Nested class solve {
    @Nested class when_the_value_is_present {
      @Test void the_callback_is_called_with_the_value() {
        final var functionSpy = Spy.throwingFunction(v -> OK);
        final var handler = Maybe.of(1).solve(functionSpy);

        assertThat(handler.success()).contains(OK);
        assertThat(handler.error()).isEmpty();

        verify(functionSpy).apply(1);
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void the_callback_is_never_called_and_returns_a_handler_with_an_error() throws IOException {
        final var functionSpy = Spy.lambda(failFunction);
        final var handler = Maybe.<String>empty().solve(functionSpy);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(NoSuchElementException.class)
          .hasMessage("No value present");

        verify(functionSpy, never()).apply(any());
      }
    }

    @Nested class when_the_new_operation_succeeds {
      @Test void returns_a_handler_with_the_solved_value() {
        final var functionSpy = Spy.lambda(ThrowingFunction.<String, RuntimeException>identity());
        final var handler = Maybe.of(OK)
          .solve(functionSpy);

        assertThat(handler.success()).contains(OK);
        assertThat(handler.error()).isEmpty();

        verify(functionSpy).apply(OK);
      }
    }

    @Nested class when_the_new_operation_fails {
      @Test void returns_a_handler_with_the_error() throws IOException {
        final var functionSpy = Spy.lambda(failFunction);
        final var handler = Maybe.of(OK)
          .solve(functionSpy);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).contains(FAILURE);

        verify(functionSpy).apply(OK);
      }
    }
  }

  @Nested class effect {
    @Nested class when_the_value_is_present {
      @Test void the_callback_is_called_with_the_value() {
        final var consumerSpy = Spy.throwingConsumer(v -> { });
        final var handler = Maybe.of(OK)
          .effect(consumerSpy);

        assertThat(handler.error()).isEmpty();

        verify(consumerSpy).accept(OK);
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void the_callback_is_never_called_and_returns_a_handler_with_an_error() {
        final var consumerSpy = Spy.throwingConsumer(v -> { });
        final var handler = Maybe.empty()
          .effect(consumerSpy);

        assertThat(handler.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(NoSuchElementException.class)
          .hasMessage("No value present");

        verify(consumerSpy, never()).accept(any());
      }
    }

    @Nested class when_the_new_operation_succeeds {
      @Test void returns_an_empty_handler() {
        final var consumerSpy = Spy.throwingConsumer(v -> { });
        final var handler = Maybe.of(OK)
          .effect(consumerSpy);

        assertThat(handler.error()).isEmpty();

        verify(consumerSpy).accept(OK);
      }
    }

    @Nested class when_the_new_operation_fails {
      @Test void returns_a_handler_with_the_error() throws IOException {
        final var consumerSpy = Spy.lambda(failConsumer);
        final var handler = Maybe.of(OK)
          .effect(consumerSpy);

        assertThat(handler.error()).contains(FAILURE);

        verify(consumerSpy).accept(OK);
      }
    }
  }

  @Nested class cast {
    @Nested class when_the_value_is_present {
      @Nested class and_the_value_is_an_instance_of_the_type {
        @Test void returns_a_solve_handler_with_the_cast_value() {
          final var mapperSpy = Spy.function((Throwable e) -> FAILURE);
          final var handler = Maybe.of((Number) 3);
          final var overloads = List.of(
            handler.cast(Integer.class),
            handler.cast(Integer.class, mapperSpy)
          );

          assertThat(overloads).isNotEmpty().allSatisfy(overload -> {
            assertThat(overload.success()).contains(3);
            assertThat(overload.error()).isEmpty();
          });

          verify(mapperSpy, never()).apply(any());
        }
      }

      @Nested class and_the_value_is_not_an_instance_of_the_type {
        @Nested class and_the_error_mapper_is_not_provided {
          @Test void returns_a_solve_handler_with_a_ClassCastException() {
            final var handler = Maybe.of("3").cast(Integer.class);

            assertThat(handler.success()).isEmpty();
            assertThat(handler.error()).containsInstanceOf(ClassCastException.class);
          }
        }

        @Nested class and_the_error_mapper_is_provided {
          @Test void returns_a_solve_handler_with_the_mapped_error() {
            final var mapperSpy = Spy.function((Throwable e) -> FAILURE);
            final var handler = Maybe.of("3").cast(Integer.class, mapperSpy);

            assertThat(handler.success()).isEmpty();
            assertThat(handler.error()).contains(FAILURE);

            verify(mapperSpy).apply(any(ClassCastException.class));
          }
        }
      }
    }

    @Nested class when_the_value_is_not_present {
      @Nested class and_the_error_mapper_is_not_provided {
        @Test void returns_a_solve_handler_with_a_NoSuchElementException() {
          final var handler = Maybe.empty().cast(String.class);

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error()).containsInstanceOf(NoSuchElementException.class);
        }
      }

      @Nested class and_the_error_mapper_is_provided {
        @Test void returns_a_solve_handler_with_the_mapped_error() {
          final var mapperSpy = Spy.function((Throwable e) -> FAILURE);
          final var handler = Maybe.empty().cast(String.class, mapperSpy);

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error()).contains(FAILURE);

          verify(mapperSpy).apply(any(NoSuchElementException.class));
        }
      }
    }
  }

  @Nested class hasValue {
    @Nested class when_the_value_is_present {
      @Test void returns_true() {
        assertThat(Maybe.of(OK).hasValue()).isTrue();
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void returns_false() {
        assertThat(Maybe.empty().hasValue()).isFalse();
      }
    }
  }

  @Nested class isEmpty {
    @Nested class when_the_value_is_not_present {
      @Test void returns_true() {
        assertThat(Maybe.empty().isEmpty()).isTrue();
      }
    }

    @Nested class when_the_value_is_present {
      @Test void returns_false() {
        assertThat(Maybe.of(OK).isEmpty()).isFalse();
      }
    }
  }

  @Nested class toOptional {
    @Nested class when_the_value_is_present {
      @Test void returns_an_Optional_wrapping_the_value() {
        assertThat(Maybe.of(OK).toOptional())
          .contains(OK);
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void returns_an_empty_Optional() {
        assertThat(Maybe.empty().toOptional())
          .isEmpty();
      }
    }
  }

  @Nested class equals {
    @Nested class when_the_tested_object_is_the_same_as_the_value {
      @Test void returns_true() {
        final var maybe = Maybe.of(3);
        final var other = maybe;
        final var isEqual = maybe.equals(other);

        assertThat(isEqual).isTrue();
      }
    }

    @Nested class when_the_tested_object_is_not_the_same_as_the_value {
      @Test void returns_false() {
        final var maybe = Maybe.of(3);
        final var other = (Object) Integer.valueOf(3);
        final var isEqual = maybe.equals(other);

        assertThat(isEqual).isFalse();
      }
    }

    @Nested class when_both_wrapped_values_are_equal {
      @Test void returns_true() {
        final var maybe = Maybe.of(OK);
        final var other = Maybe.of(OK);
        final var isEqual = maybe.equals(other);

        assertThat(isEqual).isTrue();
      }
    }

    @Nested class when_both_wrapped_values_are_not_equal {
      @Test void returns_false() {
        final var maybe = Maybe.of(OK);
        final var other = Maybe.of("OTHER");
        final var isEqualToOther = maybe.equals(other);

        assertThat(isEqualToOther).isFalse();
      }
    }
  }

  @Nested class hashCode {
    @Nested class when_the_value_is_present {
      @Test void returns_the_hash_code_of_the_value() {
        final var maybe = Maybe.of(OK);

        assertThat(maybe).hasSameHashCodeAs(OK);
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void returns_zero() {
        final var maybe = Maybe.empty();

        assertThat(maybe.hashCode()).isZero();
      }
    }
  }

  @Nested class toString {
    @Nested class when_the_value_is_present {
      @Test void returns_the_string_representation_of_the_value() {
        final var maybe = Maybe.of(OK);

        assertThat(maybe).hasToString("Maybe[OK]");
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void returns_the_string_representation_of_empty() {
        final var maybe = Maybe.empty();

        assertThat(maybe).hasToString("Maybe.empty");
      }
    }
  }

  @Nested class leftOrNull {
    @Nested class when_the_left_value_is_present {
      @Test void returns_the_left_value() {
        final var either = Either.ofLeft("foo");

        assertThat(either.leftOrNull()).isEqualTo("foo");
      }
    }

    @Nested class when_the_right_value_is_present {
      @Test void returns_null() {
        final var either = Either.ofRight("foo");

        assertThat(either.leftOrNull()).isNull();
      }
    }
  }

  @Nested class rightOrNull {
    @Nested class when_the_left_value_is_present {
      @Test void returns_null() {
        final var either = Either.ofLeft("foo");

        assertThat(either.rightOrNull()).isNull();
      }
    }

    @Nested class when_the_right_value_is_present {
      @Test void returns_the_right_value() {
        final var either = Either.ofRight("foo");

        assertThat(either.rightOrNull()).isEqualTo("foo");
      }
    }
  }

  @Nested class leftToOptional {
    @Nested class when_the_left_value_is_present {
      @Test void returs_an_Optional_with_the_left_value() {
        final var either = Either.ofLeft("foo");

        assertThat(either.leftToOptional())
          .isInstanceOf(Optional.class)
          .contains("foo");
      }
    }

    @Nested class when_the_right_value_is_present {
      @Test void returns_an_empty_Optional() {
        final var either = Either.ofRight("foo");

        assertThat(either.leftToOptional())
          .isExactlyInstanceOf(Optional.class)
          .isEmpty();
      }
    }
  }

  @Nested class rightToOptional {
    @Nested class when_the_left_value_is_present {
      @Test void returns_an_empty_Optional() {
        final var either = Either.ofLeft("foo");

        assertThat(either.rightToOptional())
          .isInstanceOf(Optional.class)
          .isEmpty();
      }
    }

    @Nested class when_the_right_value_is_present {
      @Test void returs_an_Optional_with_the_right_value() {
        final var either = Either.ofRight("foo");

        assertThat(either.rightToOptional())
          .isExactlyInstanceOf(Optional.class)
          .contains("foo");
      }
    }
  }
}

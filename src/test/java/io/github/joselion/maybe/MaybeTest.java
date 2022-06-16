package io.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;

import org.assertj.core.api.AutoCloseableSoftAssertions;
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

  private static final IOException FAIL_EXCEPTION = new IOException("FAIL");

  private final ThrowingFunction<String, String, IOException> failFunction = val -> {
    throw FAIL_EXCEPTION;
  };

  private final ThrowingSupplier<String, IOException> failSupplier = () -> {
    throw FAIL_EXCEPTION;
  };

  private final ThrowingConsumer<String, IOException> failConsumer = it -> {
    throw FAIL_EXCEPTION;
  };

  private final ThrowingRunnable<IOException> failRunnable = () -> {
    throw FAIL_EXCEPTION;
  };

  @Nested class just {
    @Nested class when_a_value_is_passed {
      @Test void returns_a_Maybe_wrapping_the_value() {
        final var maybe = Maybe.just(OK);

        assertThat(maybe.value()).contains(OK);
      }
    }

    @Nested class when_null_is_passed {
      @Test void returns_a_Maybe_wrapping_nothing() {
        final var maybe = Maybe.just(null);

        assertThat(maybe.value()).isEmpty();
      }
    }
  }

  @Nested class nothing {
    @Test void returns_a_Maybe_wrapping_nothing() {
      final var maybe = Maybe.nothing();

      assertThat(maybe.value()).isEmpty();
    }
  }

  @Nested class fromOptional {
    @Nested class when_the_optional_has_a_value {
      @Test void returns_a_Maybe_wrapping_the_value() {
        final var maybe = Maybe.fromOptional(Optional.of(OK));

        assertThat(maybe.value()).contains(OK);
      }
    }

    @Nested class when_the_optional_is_empty {
      @Test void returns_a_Maybe_wrapping_nothing() {
        final var maybe = Maybe.fromOptional(Optional.empty());

        assertThat(maybe.value()).isEmpty();
      }
    }
  }

  @Nested class fromResolver {
    @Nested class when_the_operation_succeeds {
      @Test void returns_a_handler_with_the_value() throws IOException {
        final var supplierSpy = Spy.<ThrowingSupplier<String, IOException>>lambda(() -> OK);
        final var handler = Maybe.fromResolver(supplierSpy);

        assertThat(handler.success()).contains(OK);
        assertThat(handler.error()).isEmpty();

        verify(supplierSpy, times(1)).get();
      }
    }

    @Nested class when_the_operation_fails {
      @Test void returns_a_handler_with_the_error() throws IOException {
        final var supplierSpy = Spy.lambda(failSupplier);
        final var handler = Maybe.fromResolver(supplierSpy);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).contains(FAIL_EXCEPTION);

        verify(supplierSpy, times(1)).get();
      }
    }
  }

  @Nested class fromEffect {
    @Nested class when_the_operation_succeeds {
      @Test void returns_a_handler_with_nothing() {
        final var runnableSpy = Spy.<ThrowingRunnable<RuntimeException>>lambda(() -> { });
        final var handler = Maybe.fromEffect(runnableSpy);

        assertThat(handler.error()).isEmpty();

        verify(runnableSpy, times(1)).run();
      }
    }

    @Nested class when_the_operation_fails {
      @Test void returns_a_handler_with_the_error() throws IOException {
        final var runnableSpy = Spy.lambda(failRunnable);
        final var handler = Maybe.fromEffect(runnableSpy);

        assertThat(handler.error()).contains(FAIL_EXCEPTION);

        verify(runnableSpy, times(1)).run();
      }
    }
  }

  @Nested class partialResolver {
    @Test void returns_a_function_that_takes_a_value_and_returns_a_resolve_handler() throws IOException {
      final var successSpy = Spy.<ThrowingFunction<String, Integer, RuntimeException>>lambda(String::length);
      final var failureSpy = Spy.lambda(failFunction);

      assertThat(Maybe.partialResolver(successSpy).apply(OK))
        .isInstanceOf(ResolveHandler.class)
        .extracting(ResolveHandler::success, optional(Integer.class))
        .contains(OK.length());
      assertThat(Maybe.partialResolver(failureSpy).apply(OK))
        .isInstanceOf(ResolveHandler.class)
        .extracting(ResolveHandler::error, optional(IOException.class))
        .contains(FAIL_EXCEPTION);

      verify(successSpy, times(1)).apply(OK);
      verify(failureSpy, times(1)).apply(OK);
    }
  }

  @Nested class partialEffect {
    @Test void returns_a_function_that_takes_a_value_and_returns_an_effect_handler() throws IOException {
      final var successSpy = Spy.<ThrowingConsumer<String, RuntimeException>>lambda(v -> { });
      final var failureSpy = Spy.lambda(failConsumer);

      assertThat(Maybe.partialEffect(successSpy).apply(OK))
        .isInstanceOf(EffectHandler.class)
        .extracting(EffectHandler::error, optional(RuntimeException.class))
        .isEmpty();

      assertThat(Maybe.partialEffect(failureSpy).apply(OK))
        .isInstanceOf(EffectHandler.class)
        .extracting(EffectHandler::error, optional(IOException.class))
        .contains(FAIL_EXCEPTION);

      verify(successSpy, times(1)).accept(OK);
      verify(failureSpy, times(1)).accept(OK);
    }
  }

  @Nested class withResource {
    @Test void returns_the_resource_spec_with_the_resource() {
      try (
        var softly = new AutoCloseableSoftAssertions();
        var fis = Maybe.just("./src/test/resources/readTest.txt")
          .resolve(FileInputStream::new)
          .orThrow(Error::new);
      ) {
        softly.assertThat(Maybe.withResource(fis).resource())
          .isPresent()
          .containsInstanceOf(FileInputStream.class)
          .containsSame(fis);
      } catch (Exception error) {
        throw new Error(error);
      }
    }
  }

  @Nested class map {
    @Nested class when_the_value_is_present {
      @Test void maps_the_value_with_the_passed_function() {
        final var maybe = Maybe.just(OK).map(String::length);

        assertThat(maybe.value()).contains(2);
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void returns_nothing() {
        final var maybe = Maybe.<String>nothing().map(String::length);

        assertThat(maybe.value()).isEmpty();
      }
    }
  }

  @Nested class flatMap {
    @Nested class when_the_value_is_present {
      @Test void maps_the_value_with_the_passed_maybe_function() {
        final var maybe = Maybe.just(OK)
          .flatMap(str -> Maybe.just(str.length()));

        assertThat(maybe.value()).contains(2);
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void returns_nothing() {
        final var maybe = Maybe.<String>nothing()
          .flatMap(str -> Maybe.just(str.length()));

        assertThat(maybe.value()).isEmpty();
      }
    }
  }

  @Nested class resolve {
    @Nested class when_the_value_is_present {
      @Test void the_callback_is_called_with_the_value() {
        final var functionSpy = Spy.<ThrowingFunction<Integer, String, RuntimeException>>lambda(v -> OK);
        final var handler = Maybe.just(1).resolve(functionSpy);

        assertThat(handler.success()).contains(OK);
        assertThat(handler.error()).isEmpty();

        verify(functionSpy, times(1)).apply(1);
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void the_callback_is_never_called() throws IOException {
        final var functionSpy = Spy.lambda(failFunction);
        final var handler = Maybe.<String>nothing().resolve(functionSpy);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).isEmpty();

        verify(functionSpy, never()).apply(any());
      }
    }

    @Nested class when_the_new_operation_succeeds {
      @Test void returns_a_handler_with_the_resolved_value() {
        final var functionSpy = Spy.lambda(ThrowingFunction.<String, RuntimeException>identity());
        final var handler = Maybe.just(OK)
          .resolve(functionSpy);

        assertThat(handler.success()).contains(OK);
        assertThat(handler.error()).isEmpty();

        verify(functionSpy, times(1)).apply(OK);
      }
    }

    @Nested class when_the_new_operation_fails {
      @Test void returns_a_handler_with_the_error() throws IOException {
        final var functionSpy = Spy.lambda(failFunction);
        final var handler = Maybe.just(OK)
          .resolve(functionSpy);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).contains(FAIL_EXCEPTION);

        verify(functionSpy, times(1)).apply(OK);
      }
    }
  }

  @Nested class runEffect {
    @Nested class when_the_value_is_present {
      @Test void the_callback_is_called_with_the_value() {
        final var consumerSpy = Spy.<ThrowingConsumer<String, RuntimeException>>lambda(v -> { });
        final var handler = Maybe.just(OK)
          .runEffect(consumerSpy);

        assertThat(handler.error()).isEmpty();

        verify(consumerSpy, times(1)).accept(OK);
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void the_callback_is_never_called() {
        final var consumerSpy = Spy.<ThrowingConsumer<Object, RuntimeException>>lambda(v -> { });
        final var handler = Maybe.nothing()
          .runEffect(consumerSpy);

        assertThat(handler.error()).isEmpty();

        verify(consumerSpy, never()).accept(any());
      }
    }

    @Nested class when_the_new_operation_succeeds {
      @Test void returns_the_a_handler_with_nothing() {
        final var consumerSpy = Spy.<ThrowingConsumer<String, RuntimeException>>lambda(v -> { });
        final var handler = Maybe.just(OK)
          .runEffect(consumerSpy);

        assertThat(handler.error()).isEmpty();

        verify(consumerSpy, times(1)).accept(OK);
      }
    }

    @Nested class when_the_new_operation_fails {
      @Test void returns_a_handler_with_the_error() throws IOException {
        final var consumerSpy = Spy.lambda(failConsumer);
        final var handler = Maybe.just(OK)
          .runEffect(consumerSpy);

        assertThat(handler.error()).contains(FAIL_EXCEPTION);

        verify(consumerSpy, times(1)).accept(OK);
      }
    }
  }

  @Nested class cast {
    @Nested class when_the_value_is_castable_to_the_passed_type {
      @Test void returns_a_maybe_with_the_value_cast() {
        final var maybe = Maybe.<Number>just(3);

        assertThat(maybe.cast(Integer.class).value()).contains(3);
      }
    }

    @Nested class when_the_value_is_not_castable_to_the_passed_type {
      @Test void returns_nothing() {
        final var maybe = Maybe.just("3");

        assertThat(maybe.cast(Integer.class).value()).isEmpty();
      }
    }
  }

  @Nested class hasValue {
    @Nested class when_the_value_is_present {
      @Test void returns_true() {
        assertThat(Maybe.just(OK).hasValue()).isTrue();
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void returns_false() {
        assertThat(Maybe.nothing().hasValue()).isFalse();
      }
    }
  }

  @Nested class hasNothing {
    @Nested class when_the_value_is_not_present {
      @Test void returns_true() {
        assertThat(Maybe.nothing().hasNothing()).isTrue();
      }
    }

    @Nested class when_the_value_is_present {
      @Test void returns_false() {
        assertThat(Maybe.just(OK).hasNothing()).isFalse();
      }
    }
  }

  @Nested class toOptional {
    @Nested class when_the_value_is_present {
      @Test void returns_an_Optional_wrapping_the_value() {
        assertThat(Maybe.just(OK).toOptional())
          .contains(OK);
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void returns_an_empty_Optional() {
        assertThat(Maybe.nothing().toOptional())
          .isEmpty();
      }
    }
  }

  @Nested class equals {
    @Nested class when_the_tested_object_is_the_same_as_the_value {
      @Test void returns_true() {
        final var maybe = Maybe.just(3);
        final var other = maybe;
        final var isEqual = maybe.equals(other);

        assertThat(isEqual).isTrue();
      }
    }

    @Nested class when_the_tested_object_is_not_the_same_as_the_value {
      @Test void returns_false() {
        final var maybe = Maybe.just(3);
        final var other = (Object) Integer.valueOf(3);
        final var isEqual = maybe.equals(other);

        assertThat(isEqual).isFalse();
      }
    }

    @Nested class when_both_wrapped_values_are_equal {
      @Test void returns_true() {
        final var maybe = Maybe.just(OK);
        final var other = Maybe.just(OK);
        final var isEqual = maybe.equals(other);

        assertThat(isEqual).isTrue();
      }
    }

    @Nested class when_both_wrapped_values_are_not_equal {
      @Test void returns_false() {
        final var maybe = Maybe.just(OK);
        final var other = Maybe.just("OTHER");
        final var isEqualToOther = maybe.equals(other);

        assertThat(isEqualToOther).isFalse();
      }
    }
  }

  @Nested class hashCode {
    @Nested class when_the_value_is_present {
      @Test void returns_the_hash_code_of_the_value() {
        final var maybe = Maybe.just(OK);

        assertThat(maybe).hasSameHashCodeAs(OK);
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void returns_zero() {
        final var maybe = Maybe.nothing();

        assertThat(maybe.hashCode()).isZero();
      }
    }
  }

  @Nested class toString {
    @Nested class when_the_value_is_present {
      @Test void returns_the_string_representation_of_the_value() {
        final var maybe = Maybe.just(OK);

        assertThat(maybe).hasToString("Maybe[OK]");
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void returns_the_string_representation_of_nothing() {
        final var maybe = Maybe.nothing();

        assertThat(maybe).hasToString("Maybe.nothing");
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

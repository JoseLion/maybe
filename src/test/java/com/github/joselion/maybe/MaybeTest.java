package com.github.joselion.maybe;

import static com.github.joselion.maybe.helpers.Helpers.spyLambda;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import com.github.joselion.maybe.helpers.UnitTest;
import com.github.joselion.maybe.util.ConsumerChecked;
import com.github.joselion.maybe.util.FunctionChecked;
import com.github.joselion.maybe.util.RunnableChecked;
import com.github.joselion.maybe.util.SupplierChecked;

import org.assertj.core.api.AutoCloseableSoftAssertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest class MaybeTest {

  private static final String OK = "OK";

  private static final IOException FAIL_EXCEPTION = new IOException("FAIL");

  private final FunctionChecked<String, String, IOException> failFunction = val -> {
    throw FAIL_EXCEPTION;
  };

  private final SupplierChecked<String, IOException> failSupplier = () -> {
    throw FAIL_EXCEPTION;
  };

  private final ConsumerChecked<String, IOException> failConsumer = it -> {
    throw FAIL_EXCEPTION;
  };

  private final RunnableChecked<IOException> failRunnable = () -> {
    throw FAIL_EXCEPTION;
  };

  @Nested class just {
    @Nested class when_a_value_is_passed {
      @Test void returns_a_Maybe_wrapping_the_value() {
        final Maybe<String> maybe = Maybe.just(OK);

        assertThat(maybe.value()).contains(OK);
      }
    }

    @Nested class when_null_is_passed {
      @Test void returns_a_Maybe_wrapping_nothing() {
        final Maybe<Object> maybe = Maybe.just(null);

        assertThat(maybe.value()).isEmpty();
      }
    }
  }

  @Nested class nothing {
    @Test void returns_a_Maybe_wrapping_nothing() {
      Maybe<Object> maybe = Maybe.nothing();

      assertThat(maybe.value()).isEmpty();
    }
  }

  @Nested class fromResolver {
    @Nested class when_the_operation_succeeds {
      @Test void returns_a_handler_with_the_value() throws IOException {
        final SupplierChecked<String, IOException> supplierSpy = spyLambda(() -> OK);
        final FunctionChecked<String, String, IOException> functionSpy = spyLambda(FunctionChecked.identity());
        final List<ResolveHandler<String, ?>> handlers = List.of(
          Maybe.fromResolver(supplierSpy),
          Optional.of(OK).map(Maybe.fromResolver(functionSpy)).orElseThrow()
        );

        assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
          assertThat(handler.success()).contains(OK);
          assertThat(handler.error()).isEmpty();
        });

        verify(supplierSpy, times(1)).get();
        verify(functionSpy, times(1)).apply(OK);
      }
    }

    @Nested class when_the_operation_fails {
      @Test void returns_a_handler_with_the_error() throws IOException {
        final SupplierChecked<String, IOException> supplierSpy = spyLambda(failSupplier);
        final FunctionChecked<String, String, IOException> functionSpy = spyLambda(failFunction);
        final List<ResolveHandler<?, IOException>> handlers = List.of(
          Maybe.fromResolver(supplierSpy),
          Optional.of(OK).map(Maybe.fromResolver(functionSpy)).orElseThrow()
        );

        assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
          assertThat(handler.success()).isEmpty();
          assertThat(handler.error()).contains(FAIL_EXCEPTION);
        });

        verify(supplierSpy, times(1)).get();
        verify(functionSpy, times(1)).apply(OK);
      }
    }
  }

  @Nested class fromEffect {
    @Nested class when_the_operation_succeeds {
      @Test void returns_a_handler_with_nothing() {
        final RunnableChecked<RuntimeException> runnableSpy = spyLambda(() -> { });
        final ConsumerChecked<String, RuntimeException> consumerSpy = spyLambda(v -> { });
        final List<EffectHandler<RuntimeException>> handlers = List.of(
          Maybe.fromEffect(runnableSpy),
          Optional.of(OK).map(Maybe.fromEffect(consumerSpy)).orElseThrow()
        );

        assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
          assertThat(handler.error()).isEmpty();
        });

        verify(runnableSpy, times(1)).run();
        verify(consumerSpy, times(1)).accept(OK);
      }
    }

    @Nested class when_the_operation_fails {
      @Test void returns_a_handler_with_the_error() throws IOException {
        final RunnableChecked<IOException> runnableSpy = spyLambda(failRunnable);
        final ConsumerChecked<String, IOException> consumerSpy = spyLambda(failConsumer);
        final List<EffectHandler<IOException>> handlers = List.of(
          Maybe.fromEffect(runnableSpy),
          Optional.of(OK).map(Maybe.fromEffect(consumerSpy)).orElseThrow()
        );

        assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
          assertThat(handler.error()).contains(FAIL_EXCEPTION);
        });

        verify(runnableSpy, times(1)).run();
        verify(consumerSpy, times(1)).accept(OK);
      }
    }
  }

  @Nested class withResource {
    @Test void returns_the_resource_spec_with_the_resource() {
      try (
        AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions();
        FileInputStream fis = Maybe.just("./src/test/resources/readTest.txt")
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
        final Maybe<Integer> maybe = Maybe.just(OK).map(String::length);

        assertThat(maybe.value()).contains(2);
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_nothing() {
        Maybe<Integer> maybe = Maybe.<String>nothing().map(String::length);

        assertThat(maybe.value()).isEmpty();
      }
    }
  }

  @Nested class flatMap {
    @Nested class when_the_value_is_present {
      @Test void maps_the_value_with_the_passed_maybe_function() {
        final Maybe<Integer> maybe = Maybe.just(OK)
          .flatMap(str -> Maybe.just(str.length()));

        assertThat(maybe.value()).contains(2);
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_nothing() {
        final Maybe<Integer> maybe = Maybe.<String>nothing()
          .flatMap(str -> Maybe.just(str.length()));

        assertThat(maybe.value()).isEmpty();
      }
    }
  }

  @Nested class resolve {
    @Nested class when_the_value_is_present {
      @Test void the_callback_is_called_with_the_value() {
        final FunctionChecked<Integer, String, RuntimeException> functionSpy = spyLambda(v -> OK);
        final ResolveHandler<String, ?> handler = Maybe.just(1).resolve(functionSpy);

        assertThat(handler.success()).contains(OK);
        assertThat(handler.error()).isEmpty();

        verify(functionSpy, times(1)).apply(1);
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void the_callback_is_never_called() throws IOException {
        final FunctionChecked<String, String, IOException> functionSpy = spyLambda(failFunction);
        final ResolveHandler<String, IOException> handler = Maybe.<String>nothing().resolve(functionSpy);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).isEmpty();

        verify(functionSpy, never()).apply(any());
      }
    }

    @Nested class when_the_new_operation_succeeds {
      @Test void returns_a_handler_with_the_resolved_value() {
        final FunctionChecked<String, String, RuntimeException> functionSpy = spyLambda(FunctionChecked.identity());
        final ResolveHandler<String, ?> handler = Maybe.just(OK)
          .resolve(functionSpy);

        assertThat(handler.success()).contains(OK);
        assertThat(handler.error()).isEmpty();

        verify(functionSpy, times(1)).apply(OK);
      }
    }

    @Nested class when_the_new_operation_fails {
      @Test void returns_a_handler_with_the_error() throws IOException {
        final FunctionChecked<String, String, IOException> functionSpy = spyLambda(failFunction);
        final ResolveHandler<?, IOException> handler = Maybe.just(OK)
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
        final ConsumerChecked<String, RuntimeException> consumerSpy = spyLambda(v -> { });
        final EffectHandler<RuntimeException> handler = Maybe.just(OK)
          .runEffect(consumerSpy);

        assertThat(handler.error()).isEmpty();

        verify(consumerSpy, times(1)).accept(OK);
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void the_callback_is_never_called() {
        final ConsumerChecked<Object, RuntimeException> consumerSpy = spyLambda(v -> { });
        final EffectHandler<RuntimeException> handler = Maybe.nothing()
          .runEffect(consumerSpy);

        assertThat(handler.error()).isEmpty();

        verify(consumerSpy, never()).accept(any());
      }
    }

    @Nested class when_the_new_operation_succeeds {
      @Test void returns_the_a_handler_with_nothing() {
        final ConsumerChecked<String, RuntimeException> consumerSpy = spyLambda(v -> { });
        final EffectHandler<RuntimeException> handler = Maybe.just(OK)
          .runEffect(consumerSpy);

        assertThat(handler.error()).isEmpty();

        verify(consumerSpy, times(1)).accept(OK);
      }
    }

    @Nested class when_the_new_operation_fails {
      @Test void returns_a_handler_with_the_error() throws IOException {
        final ConsumerChecked<String, IOException> consumerSpy = spyLambda(failConsumer);
        final EffectHandler<IOException> handler = Maybe.just(OK)
          .runEffect(consumerSpy);

        assertThat(handler.error()).contains(FAIL_EXCEPTION);

        verify(consumerSpy, times(1)).accept(OK);
      }
    }
  }

  @Nested class cast {
    @Nested class when_the_value_is_castable_to_the_passed_type {
      @Test void returns_a_maybe_with_the_value_cast() {
        final Maybe<Number> maybe = Maybe.just(3);

        assertThat(maybe.cast(Integer.class).value()).contains(3);
      }
    }

    @Nested class when_the_value_is_NOT_castable_to_the_passed_type {
      @Test void returns_nothing() {
        final Maybe<String> maybe = Maybe.just("3");

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

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_false() {
        assertThat(Maybe.nothing().hasValue()).isFalse();
      }
    }
  }

  @Nested class hasNothing {
    @Nested class when_the_value_is_NOT_present {
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

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_an_empty_Optional() {
        assertThat(Maybe.nothing().toOptional())
          .isEmpty();
      }
    }
  }

  @Nested class equals {
    @Nested class when_the_tested_object_is_the_same_instance {
      @Test void returns_true() {
        final Maybe<Integer> maybe = Maybe.just(3);
        final Object other = maybe;
        final boolean isEqual = maybe.equals(other);

        assertThat(isEqual).isTrue();
      }
    }

    @Nested class when_the_tested_objectis_NOT_the_same_instance {
      @Test void returns_false() {
        final Maybe<Integer> maybe = Maybe.just(3);
        final Object other = Integer.valueOf(3);
        final boolean isEqual = maybe.equals(other);

        assertThat(isEqual).isFalse();
      }
    }

    @Nested class when_both_wrapped_values_are_equal {
      @Test void returns_true() {
        final Maybe<String> maybe = Maybe.just(OK);
        final Maybe<String> other = Maybe.just(OK);
        final boolean isEqual = maybe.equals(other);

        assertThat(isEqual).isTrue();
      }
    }

    @Nested class when_both_wrapped_values_are_NOT_equal {
      @Test void returns_false() {
        final Maybe<String> maybe = Maybe.just(OK);
        final Maybe<String> other = Maybe.just("OTHER");
        final boolean isEqualToOther = maybe.equals(other);

        assertThat(isEqualToOther).isFalse();
      }
    }
  }

  @Nested class hashCode {
    @Nested class when_the_value_is_present {
      @Test void returns_the_hash_code_of_the_value() {
        final Maybe<String> maybe = Maybe.just(OK);

        assertThat(maybe).hasSameHashCodeAs(OK);
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_zero() {
        final Maybe<?> maybe = Maybe.nothing();

        assertThat(maybe.hashCode()).isZero();
      }
    }
  }

  @Nested class toString {
    @Nested class when_the_value_is_present {
      @Test void returns_the_string_representation_of_the_value() {
        final Maybe<String> maybe = Maybe.just(OK);

        assertThat(maybe).hasToString("Maybe[OK]");
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_the_string_representation_of_nothing() {
        final Maybe<?> maybe = Maybe.nothing();

        assertThat(maybe).hasToString("Maybe.nothing");
      }
    }
  }
}

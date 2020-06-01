package com.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;

import java.io.IOException;

import com.github.joselion.maybe.helpers.UnitTest;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class MaybeTest {

  private final String SUCCESS = "success";

  private final String ERROR = "error";

  @Nested
  class just {

    @Nested
    class when_a_value_is_passed {

      @Test
      void returns_the_monad_with_the_value() {
        final Maybe<String> maybe = Maybe.just("foo");

        assertThat(maybe).extracting(SUCCESS, optional(String.class)).contains("foo");
      }
    }

    @Nested
    class when_null_is_passed {

      @Test
      void there_is_nothing_in_the_monad() {
        final Maybe<Object> maybe = Maybe.just(null);

        assertThat(maybe).extracting(SUCCESS, optional(Object.class)).isEmpty();
      }
    }
  }

  @Nested
  class nothing {

    @Test
    void there_is_nothing_in_the_monad() {
      Maybe<Object> maybe = Maybe.nothing();

      assertThat(maybe).extracting(SUCCESS, optional(Object.class)).isEmpty();
    }
  }

  @Nested
  class resolve {

    @Nested
    class when_the_operation_success {

      @Test
      void returns_a_handler_with_the_value() {
        final ResolveHandler<String, ?> handler = Maybe.resolve(() -> "OK");

        assertThat(handler)
          .extracting(SUCCESS, optional(String.class))
          .contains("OK");

        assertThat(handler)
          .extracting(ERROR, optional(RuntimeException.class))
          .isEmpty();
      }
    }

    @Nested
    class when_the_operation_fails {

      @Test
      void returns_a_handler_with_the_error() {
        final IOException exception = new IOException("FAIL");
        final ResolveHandler<?, IOException> handler = Maybe.resolve(() -> { throw exception; });

        assertThat(handler)
          .extracting(SUCCESS, optional(Object.class))
          .isEmpty();

        assertThat(handler)
          .extracting(ERROR, optional(IOException.class))
          .containsInstanceOf(IOException.class)
          .contains(exception);
      }
    }
  }

  @Nested
  class runEffect {

    @Nested
    class when_the_operation_success {

      @Test
      void returns_a_handler_with_nothing() {
        final EffectHandler<?> handler = Maybe.runEffect(() -> { });

        assertThat(handler)
          .extracting(ERROR, optional(RuntimeException.class))
          .isEmpty();
      }
    }

    @Nested
    class when_the_operation_fails {

      @Test
      void returns_a_handler_with_the_error() {
        final IOException exception = new IOException("FAIL");
        final EffectHandler<IOException> handler = Maybe.runEffect(() -> { throw exception; });

        assertThat(handler)
          .extracting(ERROR, optional(IOException.class))
          .containsInstanceOf(IOException.class)
          .contains(exception);
      }
    }
  }

  @Nested
  class map {

    @Nested
    class when_there_is_a_value_in_the_monad {

      @Test
      void maps_the_value_with_the_passed_function() {
        final Maybe<Integer> maybe = Maybe.just("OK").map(String::length);
        
        assertThat(maybe)
          .extracting(SUCCESS, optional(Integer.class))
            .containsInstanceOf(Integer.class)
            .contains(2);
      }
    }

    @Nested
    class when_there_is_NO_value_in_the_monad {

      @Test
      void returns_nothing() {
        Maybe<Integer> maybe = Maybe.<String>nothing().map(String::length);

        assertThat(maybe)
          .extracting(SUCCESS, optional(IOException.class))
          .isEmpty();
      }
    }
  }

  @Nested
  class flatMap {

    @Nested
    class when_there_is_a_value_in_the_monad {

      @Test
      void maps_the_value_with_the_passed_maybe_function() {
        final Maybe<Integer> maybe = Maybe.just("OK").flatMap(str -> Maybe.just(str.length()));

        assertThat(maybe)
          .extracting(SUCCESS, optional(Integer.class))
          .containsInstanceOf(Integer.class)
          .contains(2);
      }
    }

    @Nested
    class when_there_is_NO_value_in_the_monad {

      @Test
      void returns_nothing() {
        final Maybe<Integer> maybe = Maybe.<String>nothing()
          .flatMap(str -> Maybe.just(str.length()));

        assertThat(maybe)
          .extracting(SUCCESS, optional(Integer.class))
          .isEmpty();
      }
    }
  }

  @Nested
  class thenResolve {

    @Nested
    class when_the_previous_operation_resolves {

      @Test
      void the_then_operation_is_called_with_the_previous_value() {
        final ResolveHandler<String, ?> handler = Maybe.just(1)
          .thenResolve(value -> {
            assertThat(value).isEqualTo(1);
            return "OK";
          });

        assertThat(handler)
          .extracting(SUCCESS, optional(String.class))
          .contains("OK");

        assertThat(handler)
          .extracting(ERROR, optional(Exception.class))
          .isEmpty();
      }
    }

    @Nested
    class when_the_previous_operation_fails {

      @Test
      void the_then_operation_is_not_called() {
        final ResolveHandler<?, ?> handler = Maybe.nothing()
          .thenResolve(value -> {
            throw new AssertionError("The then operation should not be executed");
          });

        assertThat(handler)
          .extracting(SUCCESS, optional(Object.class))
          .isEmpty();

        assertThat(handler)
          .extracting(ERROR, optional(Exception.class))
          .isEmpty();
      }
    }

    @Nested
    class when_the_new_operation_success {

      @Test
      void returns_the_a_handler_with_the_resolved_value() {
        final ResolveHandler<String, ?> handler = Maybe.just(3)
          .thenResolve(value -> "OK".repeat(value));

        assertThat(handler)
          .extracting(SUCCESS, optional(String.class))
          .contains("OKOKOK");

        assertThat(handler)
          .extracting(ERROR, optional(Exception.class))
          .isEmpty();
      }
    }

    @Nested
    class when_the_new_operation_fails {

      @Test
      void returns_a_handler_with_the_error() {
        final IOException exception = new IOException("FAIL");
        final ResolveHandler<?, IOException> handler = Maybe.just(3)
          .thenResolve(value -> { throw exception; });

        assertThat(handler)
          .extracting(SUCCESS, optional(Object.class))
          .isEmpty();

        assertThat(handler)
          .extracting(ERROR, optional(IOException.class))
          .containsInstanceOf(IOException.class)
          .contains(exception);
      }
    }
  }

  @Nested
  class thenRunEffect {

    @Nested
    class when_the_previous_operation_resolves {

      @Test
      void the_then_operation_is_called_with_the_previous_value() {
        final EffectHandler<RuntimeException> handler = Maybe.just(1)
          .thenRunEffect(value -> {
            assertThat(value).isEqualTo(1);
          })
          .and()
          .thenRunEffect(none -> {
            assertThat(none).isExactlyInstanceOf(Void.class);
          });

        assertThat(handler)
          .extracting(ERROR, optional(RuntimeException.class))
          .isEmpty();
      }
    }

    @Nested
    class when_the_previous_operation_fails {

      @Test
      void the_then_operation_is_not_called() {
        final EffectHandler<RuntimeException> handler = Maybe.nothing()
          .thenRunEffect(value -> {
            throw new AssertionError("The then operation should not be executed");
          });

        assertThat(handler)
          .extracting(ERROR, optional(Exception.class))
          .isEmpty();
      }
    }

    @Nested
    class when_the_new_operation_success {

      @Test
      void returns_the_a_handler_with_nothing() {
        final EffectHandler<RuntimeException> handler = Maybe.just(3)
          .thenRunEffect(value -> {
            assertThat(value).isEqualTo(3);
          });

        assertThat(handler)
          .extracting(ERROR, optional(Exception.class))
          .isEmpty();
      }
    }

    @Nested
    class when_the_new_operation_fails {

      @Test
      void returns_a_handler_with_the_error() {
        final IOException exception = new IOException("FAIL");
        final EffectHandler<IOException> handler = Maybe.just(3)
          .thenRunEffect(value -> { throw exception; });

        assertThat(handler)
          .extracting(ERROR, optional(IOException.class))
          .containsInstanceOf(IOException.class)
          .contains(exception);
      }
    }
  }
  
  @Nested
  class cast {

    @Nested
    class when_the_value_is_castable_to_the_passed_type {

      @Test
      void returns_a_maybe_with_the_value_cast() {
        final Maybe<Number> maybe = Maybe.<Number>just(3);

        assertThat(maybe.cast(Integer.class))
          .extracting(SUCCESS, optional(Integer.class))
          .containsInstanceOf(Integer.class)
          .contains(3);
      }
    }

    @Nested
    class when_the_value_is_NOT_castable_to_the_passed_type {

      @Test
      void returns_nothing() {
        final Maybe<String> maybe = Maybe.just("3");

        assertThat(maybe.cast(Integer.class))
          .extracting(SUCCESS, optional(Integer.class))
          .isEmpty();
      }
    }
  }

  @Nested
  class hasValue {

    @Nested
    class when_there_is_a_value_in_the_monad {

      @Test
      void returns_true() {
        assertThat(Maybe.just("OK").hasValue()).isTrue();
      }
    }

    @Nested
    class when_there_is_NO_value_in_the_monad {

      @Test
      void returns_false() {
        assertThat(Maybe.nothing().hasValue()).isFalse();
      }
    }
  }

  @Nested
  class hasNothing {

    @Nested
    class when_there_is_nothing_in_the_monad {

      @Test
      void returns_true() {
        assertThat(Maybe.nothing().hasNothing()).isTrue();
      }
    }

    @Nested
    class when_there_is_a_value_in_the_monad {

      @Test
      void returns_false() {
        assertThat(Maybe.just("OK").hasNothing()).isFalse();
      }
    }
  }

  @Nested
  class toOptional {
    
    @Test
    void returns_the_value_of_the_monad_as_optional() {
      final Maybe<String> maybe = Maybe.just("OK");

      assertThat(maybe.toOptional())
        .contains("OK");
    }
  }

  @Nested
  class equals {

    @Nested
    class when_the_tested_object_is_the_same_instance {

      @Test
      void returns_true() {
        final Maybe<Integer> maybe = Maybe.just(3);
        final Object other = maybe;

        assertThat(maybe.equals(other)).isTrue();
      }
    }

    @Nested
    class when_both_wrapped_values_are_equal {

      @Test
      void returns_true() {
        final Maybe<String> maybe = Maybe.just("OK");
        final Maybe<String> other = Maybe.just("OK");

        assertThat(maybe.equals(other)).isTrue();
      }
    }

    @Nested
    class when_both_wrapped_values_are_NOT_equal {

      @Test
      void returns_false() {
        final Maybe<String> maybe = Maybe.just("OK");
        final Maybe<String> other = Maybe.just("OTHER");
        final Object obj = "OK";

        assertThat(maybe.equals(obj)).isFalse();
        assertThat(maybe.equals(other)).isFalse();
      }
    }
  }

  @Nested
  class hashCode {
    @Nested
    class when_there_is_a_value_in_the_monad {
  
      @Test
      void returns_the_hash_code_of_the_value() {
        final Maybe<String> maybe = Maybe.just("OK");
  
        assertThat(maybe.hashCode()).isEqualTo("OK".hashCode());
      }
    }

    @Nested
    class when_there_is_NO_value_in_the_monad {

      @Test
      void returns_zero() {
        final Maybe<?> maybe = Maybe.nothing();

        assertThat(maybe.hashCode()).isEqualTo(0);
      }
    }
  }

  @Nested
  class toString {
    @Nested
    class when_there_is_a_value_in_the_monad {
  
      @Test
      void returns_the_string_representation_of_the_value() {
        final Maybe<String> maybe = Maybe.just("OK");
  
        assertThat(maybe.toString()).isEqualTo("Maybe[OK]");
      }
    }

    @Nested
    class when_there_is_NO_value_in_the_monad {

      @Test
      void returns_the_string_representation_of_nothing() {
        final Maybe<?> maybe = Maybe.nothing();

        assertThat(maybe.toString()).isEqualTo("Maybe.nothing");
      }
    }
  }
}

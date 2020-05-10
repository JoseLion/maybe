package com.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import com.github.joselion.maybe.exceptions.MaybeFailedException;
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

        assertThat(maybe)
          .extracting(SUCCESS, optional(String.class))
          .contains("foo");
      }
    }

    @Nested
    class when_null_is_passed {

      @Test
      void there_is_nothing_in_the_monad() {
        final Maybe<Object> maybe = Maybe.just(null);

        assertThat(maybe)
          .extracting(SUCCESS, optional(Object.class))
          .isEmpty();
      }
    }
  }

  @Nested
  class nothing {

    @Test
    void there_is_nothing_in_the_monad() {
      Maybe<Object> maybe = Maybe.nothing();

      assertThat(maybe)
        .extracting(SUCCESS, optional(Object.class))
        .isEmpty();
    }
  }

  @Nested
  class resolve {

    @Nested
    class when_the_operation_success {

      @Test
      void returns_the_a_handler_with_the_success_value() {
        final ResolveHandler<String, Exception> handler = Maybe.resolve(() -> "OK");

        assertThat(handler)
          .extracting(SUCCESS, optional(String.class))
          .contains("OK");
      }
    }

    @Nested
    class when_the_operation_fails {

      @Test
      void returns_the_a_handler_with_the_success_value() {
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
  class getUnsafe {

    @Nested
    class when_there_is_a_success_value_in_the_monad {

      @Test
      void returns_the_success_value() {
        final Maybe<Boolean> maybe = Maybe.just(true);

        assertThat(maybe.getUnsafe()).isEqualTo(true);
      }
    }

    @Nested
    class when_there_is_NO_success_value_in_the_monad {

      @Test
      void throws_a_NoShuchElement_exception() {
        final Maybe<?> maybe = Maybe.nothing();

        assertThat(
          assertThrows(MaybeFailedException.class, maybe::getUnsafe)
        )
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Cannot unbox the success value. The operation failed to resolve or execute");
      }
    }
  }

  @Nested
  class orElse {

    @Nested
    class when_there_is_a_success_value_in_the_monad {

      @Test
      void returns_the_success_value() {
        final Maybe<String> maybe = Maybe.just("OK");

        assertThat(maybe.orElse("BAD")).isEqualTo("OK");
        assertThat(maybe.orElse(() -> "BAD")).isEqualTo("OK");
      }
    }

    @Nested
    class when_there_is_NO_success_value_in_the_monad {

      @Test
      void returns_the_other_value() {
        final Maybe<String> maybe = Maybe.nothing();

        assertThat(maybe.orElse("OTHER")).isEqualTo("OTHER");
        assertThat(maybe.orElse(() -> "OTHER")).isEqualTo("OTHER");
      }
    }
  }

  @Nested
  class orThrow {
    
    @Nested
    class when_there_is_a_success_value_in_the_monad {

      @Test
      void returns_the_success_value() {
        final Maybe<String> maybe = Maybe.just("OK");

        assertThat(maybe.orThrow(new RuntimeException())).isEqualTo("OK");
        assertThat(maybe.orThrow(RuntimeException::new)).isEqualTo("OK");
      }
    }

    @Nested
    class when_there_is_NO_success_value_in_the_monad {

      @Test
      void throws_the_passed_failure_exception() {
        final Maybe<?> maybe = Maybe.nothing();

        assertThrows(
          RuntimeException.class,
          () -> maybe.orThrow(new RuntimeException())
        );
        assertThrows(
          RuntimeException.class,
          () -> maybe.orThrow(RuntimeException::new)
        );
      }
    }
  }

  @Nested
  class map {

    @Nested
    class when_there_is_a_success_value_in_the_monad {

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
    class when_there_is_NO_success_value_in_the_monad {

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
    class when_there_is_a_success_value_in_the_monad {

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
    class when_there_is_NO_success_value_in_the_monad {

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
    class when_the_previous_operation_failed {

      @Test
      void the_then_operation_is_not_called() {
        final ResolveHandler<?, ?> handler = Maybe.nothing()
          .thenResolve(value -> {
            throw new Exception("The then operation should not be executed");
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
  class cast {

    @Nested
    class when_there_is_a_success_value_in_the_monad {

      @Test
      void casts_the_value_to_the_passed_type() {
        final Maybe<Object> maybe = Maybe.just(3);

        assertThat(maybe.cast(Integer.class))
          .extracting(SUCCESS, optional(Integer.class))
          .containsInstanceOf(Integer.class)
          .contains(3);
      }
    }

    @Nested
    class when_there_is_NO_success_value_in_the_monad {

      @Test
      void returns_nothing() {
        final Maybe<String> maybe = Maybe.<String>nothing();

        assertThat(maybe.cast(Integer.class))
          .extracting(SUCCESS, optional(Integer.class))
          .isEmpty();
      }
    }
  }

  @Nested
  class hasSuccess {

    @Nested
    class when_there_is_a_success_value_in_the_monad {

      @Test
      void returns_true() {
        assertThat(Maybe.just("OK").hasSuccess()).isTrue();
      }
    }

    @Nested
    class when_there_is_NO_success_value_in_the_monad {

      @Test
      void returns_false() {
        assertThat(Maybe.nothing().hasSuccess()).isFalse();
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
    class when_there_is_a_success_value_in_the_monad {

      @Test
      void returns_false() {
        assertThat(Maybe.just("OK").hasNothing()).isFalse();
      }
    }
  }
}

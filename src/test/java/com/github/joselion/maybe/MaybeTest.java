package com.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.optional;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.EOFException;
import java.io.IOException;
import java.util.NoSuchElementException;

import com.github.joselion.maybe.exceptions.MaybeFailureException;
import com.github.joselion.maybe.util.SupplierChecked;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class MaybeTest {

  private final String SUCCESS = "success";

  private final String FAILURE = "failure";

  @Nested
  class just {

    @Nested
    class when_a_value_is_passed {

      @Test
      void returns_the_monad_with_the_value() {
        final Maybe<String, Exception> maybe = Maybe.just("foo");

        assertThat(maybe).extracting(SUCCESS, optional(String.class)).contains("foo");
        assertThat(maybe).extracting(FAILURE, optional(Exception.class)).isEmpty();
      }
    }

    @Nested
    class when_null_is_passed {

      @Test
      void there_is_nothing_in_the_monad() {
        final Maybe<Object, Exception> maybe = Maybe.just(null);

        assertThat(maybe).extracting(SUCCESS, optional(Object.class)).isEmpty();
        assertThat(maybe).extracting(FAILURE, optional(Exception.class)).isEmpty();
      }
    }
  }

  @Nested
  class failure {

    @Nested
    class when_an_exception_is_passed {

      @Test
      void returns_the_monad_with_the_failure_exception() {
        final IOException exception = new IOException();
        final Maybe<Object, IOException> maybe = Maybe.fail(exception);

        assertThat(maybe).extracting(SUCCESS, optional(Object.class)).isEmpty();
        assertThat(maybe).extracting(FAILURE, optional(IOException.class)).contains(exception);
      }
    }

    @Nested
    class when_null_is_passed {

      @Test
      void there_is_nothing_in_the_monad() {
        final Maybe<Object, Exception> maybe = Maybe.fail(null);

        assertThat(maybe).extracting(SUCCESS, optional(Object.class)).isEmpty();
        assertThat(maybe).extracting(FAILURE, optional(Exception.class)).isEmpty();
      }
    }
  }

  @Nested
  class nothing {

    @Test
    void there_is_nothing_in_the_monad() {
      Maybe<Object, Exception> maybe = Maybe.nothing();

      assertThat(maybe).extracting(SUCCESS, optional(Object.class)).isEmpty();
      assertThat(maybe).extracting(FAILURE, optional(Exception.class)).isEmpty();
    }
  }

  @Nested
  class resolve {

    @Nested
    class when_the_operation_success {

      @Test
      void returns_the_monad_with_the_value() {
        final Maybe<String, RuntimeException> maybe = Maybe.resolve(() -> "OK");

        assertThat(maybe).extracting(SUCCESS, optional(String.class)).contains("OK");
        assertThat(maybe).extracting(FAILURE, optional(RuntimeException.class)).isEmpty();
      }
    }

    @Nested
    class when_the_operation_fails {

      @Test
      void returns_the_monad_with_the_failure_exception() {
        final IOException exception = new IOException("FAIL");
        final Maybe<String, IOException> maybe = Maybe.resolve(() -> { throw exception; });

        assertThat(maybe).extracting(SUCCESS, optional(String.class)).isEmpty();
        assertThat(maybe).extracting(FAILURE, optional(IOException.class)).contains(exception);
      }
    }
  }

  @Nested
  class execute {

    @Nested
    class when_the_operation_success {

      @Test
      void returns_the_monad_with_nothing() {
        final Maybe<Void, IOException> maybe = Maybe.execute(() -> { });

        assertThat(maybe).extracting(SUCCESS, optional(Void.class)).isEmpty();
        assertThat(maybe).extracting(FAILURE, optional(IOException.class)).isEmpty();
      }
    }

    @Nested
    class when_the_operation_fails {

      @Test
      void returns_the_monad_with_the_failure_exception() {
        final IOException exception = new IOException("FAIL");
        final Maybe<Void, IOException> maybe = Maybe.execute(() -> { throw exception; });

        assertThat(maybe).extracting(SUCCESS, optional(Void.class)).isEmpty();
        assertThat(maybe).extracting(FAILURE, optional(IOException.class)).contains(exception);
      }
    }
  }

  @Nested
  class getSafe {
    
    @Nested
    class when_there_is_a_success_value_in_the_monad {

      @Test
      void returns_the_success_value() throws Exception {
        final Maybe<Integer, Exception> maybe = Maybe.just(5);

        assertThat(maybe.getSafe()).isEqualTo(5);
      }
    }

    @Nested
    class when_there_is_NO_success_value_in_the_monad {

      @Nested
      class and_the_failure_exception_is_present {
        @Test
        void throws_the_failure_exception() {
          final Maybe<Object, IOException> maybe = Maybe.fail(new IOException("FAIL"));
  
          assertThat(
            assertThrows(IOException.class, maybe::getSafe)
          )
          .hasMessage("FAIL");
        }
      }

      @Nested
      class and_the_failure_exception_is_NOT_present {

        @Test
        void throws_a_NoSuchElementException_exception() {
          assertThrows(NoSuchElementException.class, Maybe.nothing()::getSafe);
        }
      }

    }
  }

  @Nested
  class getUnsafe {

    @Nested
    class when_there_is_a_success_value_in_the_monad {

      @Test
      void returns_the_success_value() {
        final Maybe<Boolean, Exception> maybe = Maybe.just(true);

        assertThat(maybe.getUnsafe()).isEqualTo(true);
      }
    }

    @Nested
    class when_there_is_NO_success_value_in_the_monad {

      @Nested
      class and_the_failure_exception_is_present {

        @Test
        void throws_a_MaybeFailureException_exception() {
          final Maybe<Object, IOException> maybe = Maybe.fail(new IOException("FAIL"));

          assertThat(
            assertThrows(MaybeFailureException.class, maybe::getUnsafe)
          )
          .hasMessage("The Maybe oparation failed with an excpetion!")
          .hasRootCauseExactlyInstanceOf(IOException.class)
          .hasRootCauseMessage("FAIL");
        }
      }

      @Nested
      class and_the_failure_exception_is_NOT_present {

        @Test
        void throws_a_NoSuchElementException_exception() {
          assertThrows(NoSuchElementException.class, Maybe.nothing()::getUnsafe);
        }
      }
    }
  }

  @Nested
  class orElse {

    @Nested
    class when_there_is_a_success_value_in_the_monad {

      @Test
      void returns_the_success_value() {
        final Maybe<String, IOException> maybe = Maybe.resolve(throwingOp(false));

        assertThat(maybe.orElse("BAD")).isEqualTo("OK");
        assertThat(
          maybe.orElse(exception -> {
            assertThat(exception).isEmpty();
            return "BAD";
          })
        )
        .isEqualTo("OK");
      }
    }

    @Nested
    class when_there_is_NO_success_value_in_the_monad {

      @Test
      void returns_the_other_value() {
        final Maybe<String, IOException> maybe = Maybe.resolve(throwingOp(true));

        assertThat(maybe.orElse("OTHER")).isEqualTo("OTHER");
        assertThat(
          maybe.orElse(exception -> {
            assertThat(exception).hasValueSatisfying(ex -> {
              assertThat(ex)
                .isExactlyInstanceOf(IOException.class)
                .hasMessage("FAIL");
            });
            return "OTHER";
          })
        )
        .isEqualTo("OTHER");
      }
    }
  }

  @Nested
  class orThrow {
    
    @Nested
    class when_there_is_a_success_value_in_the_monad {

      @Test
      void returns_the_success_value() throws IOException, EOFException {
        final Maybe<String, IOException> maybe = Maybe.resolve(throwingOp(false));

        assertThat(maybe.orThrow()).isEqualTo("OK");
        assertThat(maybe.orThrow(e -> new EOFException())).isEqualTo("OK");
        assertThat(maybe.orThrow(RuntimeException::new)).isEqualTo("OK");
      }
    }

    @Nested
    class when_there_is_a_failure_exception_in_the_monad {

      @Test
      void throws_the_failure_exception() {
        final Maybe<String, IOException> maybe = Maybe.resolve(throwingOp(true));

        assertThrows(IOException.class, () -> maybe.orThrow());
        assertThrows(EOFException.class, () -> maybe.orThrow(e -> new EOFException()));
        assertThrows(RuntimeException.class, () -> maybe.orThrow(RuntimeException::new));
      }
    }
  }

  @Nested
  class map {

    @Nested
    class when_there_is_a_success_value_in_the_monad {

      @Test
      void maps_the_value_with_the_passed_function() {
        final Maybe<Integer, IOException> maybe = Maybe.resolve(throwingOp(false)).map(String::length);
        
        assertThat(maybe)
          .extracting(SUCCESS, optional(Integer.class))
            .containsInstanceOf(Integer.class)
            .contains(2);

        assertThat(maybe)
          .extracting(FAILURE, optional(IOException.class))
            .isEmpty();
      }
    }

    @Nested
    class when_there_is_NO_success_value_in_the_monad {

      @Nested
      class and_the_failure_exception_is_present {

        @Test
        void returns_the_monad_with_the_failure_exception() {
          final Maybe<Integer, IOException> maybe = Maybe.resolve(throwingOp(true)).map(String::length);

          assertThat(maybe)
            .extracting(SUCCESS, optional(Integer.class))
              .isEmpty();

          assertThat(maybe)
            .extracting(FAILURE, optional(IOException.class))
              .hasValueSatisfying(exception -> {
                assertThat(exception)
                  .isExactlyInstanceOf(IOException.class)
                  .hasMessage("FAIL");
              });
        }
      }

      @Nested
      class and_the_failure_exception_is_NOT_present {

        @Test
        void returns_the_monad_with_nothing() {
          Maybe<Integer, IOException> maybe = Maybe.<String, IOException>nothing().map(String::length);

          assertThat(maybe)
          .extracting(SUCCESS, optional(IOException.class))
            .isEmpty();

            assertThat(maybe)
            .extracting(FAILURE, optional(IOException.class))
              .isEmpty();
        }
      }
    }
  }

  @Nested
  class flatMap {

    @Nested
    class when_there_is_a_success_value_in_the_monad {

      @Test
      void maps_the_value_with_the_passed_maybe_function() {
        final Maybe<Integer, IOException> maybe = Maybe.resolve(throwingOp(false)).flatMap(str -> Maybe.just(str.length()));

        assertThat(maybe)
          .extracting(SUCCESS, optional(Integer.class))
            .containsInstanceOf(Integer.class)
            .contains(2);

        assertThat(maybe)
          .extracting(FAILURE, optional(IOException.class))
            .isEmpty();
      }
    }

    @Nested
    class when_there_is_NO_success_value_in_the_monad {

      @Nested
      class and_the_failure_exception_is_present {

        @Test
        void returns_the_monad_with_the_failure_exception() {
          final Maybe<Integer, IOException> maybe = Maybe.resolve(throwingOp(true)).flatMap(str -> Maybe.just(str.length()));

          assertThat(maybe)
            .extracting(SUCCESS, optional(Integer.class))
              .isEmpty();

          assertThat(maybe)
            .extracting(FAILURE, optional(IOException.class))
              .hasValueSatisfying(exception -> {
                assertThat(exception)
                  .isExactlyInstanceOf(IOException.class)
                  .hasMessage("FAIL");
              });
        }
      }

      @Nested
      class and_the_failure_exception_is_NOT_present {

        @Test
        void returns_the_monad_with_nothing() {
          Maybe<Integer, IOException> maybe = Maybe.<String, IOException>nothing().flatMap(str -> Maybe.just(str.length()));

          assertThat(maybe)
          .extracting(SUCCESS, optional(IOException.class))
            .isEmpty();

            assertThat(maybe)
            .extracting(FAILURE, optional(IOException.class))
              .isEmpty();
        }
      }
    }
  }

  @Nested
  class thenResolve {

    @Nested
    class when_the_previous_maybe_had_resolved {

      @Test
      void the_success_argument_has_a_value_and_the_failure_is_empty() {
        Maybe.just(1).thenResolve((prev, error) -> {
          assertThat(prev).contains(1);
          assertThat(error).isEmpty();

          return "OK";
        });
      }
    }

    @Nested
    class when_the_previous_maybe_had_failed {

      @Test
      void the_success_argument_is_empty_and_the_failure_has_an_exception() {
        Maybe.fail(new IOException("FAIL"))
          .thenResolve((prev, error) -> {
            assertThat(prev).isEmpty();
            assertThat(error).hasValueSatisfying(e -> {
              assertThat(e)
                .isExactlyInstanceOf(IOException.class)
                .hasMessage("FAIL");
            });

            return "OK";
          });
      }
    }

    @Nested
    class when_the_previous_maybe_had_nothing {

      @Test
      void the_success_and_failure_arguments_are_empty() {
        Maybe.nothing()
          .thenResolve((prev, error) -> {
            assertThat(prev).isEmpty();
            assertThat(error).isEmpty();

            return null;
          });
      }
    }

    @Nested
    class when_the_new_operation_success {

      @Test
      void returns_the_monad_with_the_new_value() {
        final Maybe<String, IOException> maybe = Maybe.just(3).thenResolve((num, error) -> "OK".repeat(num.orElse(0)));

        assertThat(maybe)
          .extracting(SUCCESS, optional(String.class))
            .contains("OKOKOK");

        assertThat(maybe)
          .extracting(FAILURE, optional(IOException.class))
            .isEmpty();
      }
    }

    @Nested
    class when_the_new_operation_fails {

      @Test
      void returns_the_monad_with_the_failure_exception() {
        final IOException exception = new IOException("FAIL");
        final Maybe<String, IOException> maybe = Maybe.just(3).thenResolve((num, error) -> { throw exception; });

        assertThat(maybe)
          .extracting(SUCCESS, optional(String.class))
            .isEmpty();

        assertThat(maybe)
          .extracting(FAILURE, optional(IOException.class))
            .contains(exception);
      }
    }
  }

  @Nested
  class thenExecute {

    @Nested
    class when_the_previous_maybe_had_resolved {

      @Test
      void the_success_argument_has_a_value_and_the_failure_is_empty() {
        Maybe.just(1).thenExecute((prev, error) -> {
          assertThat(prev).contains(1);
          assertThat(error).isEmpty();
        });
      }
    }

    @Nested
    class when_the_previous_maybe_had_failed {

      @Test
      void the_success_argument_is_empty_and_the_failure_has_an_exception() {
        Maybe.fail(new IOException("FAIL"))
          .thenExecute((prev, error) -> {
            assertThat(prev).isEmpty();
            assertThat(error).hasValueSatisfying(e -> {
              assertThat(e)
                .isExactlyInstanceOf(IOException.class)
                .hasMessage("FAIL");
            });
          });
      }
    }

    @Nested
    class when_the_previous_maybe_had_nothing {

      @Test
      void the_success_and_failure_arguments_are_empty() {
        Maybe.nothing()
          .thenExecute((prev, error) -> {
            assertThat(prev).isEmpty();
            assertThat(error).isEmpty();
          });
      }
    }

    @Nested
    class when_the_new_operation_success {

      @Test
      void returns_the_monad_with_nothing() {
        final Maybe<Void, IOException> maybe = Maybe.just(3).thenExecute((num, error) -> { });

        assertThat(maybe)
          .extracting(SUCCESS, optional(Void.class))
            .isEmpty();

        assertThat(maybe)
          .extracting(FAILURE, optional(IOException.class))
            .isEmpty();
      }
    }

    @Nested
    class when_the_new_operation_fails {

      @Test
      void returns_the_monad_with_the_failure_exception() {
        final IOException exception = new IOException("FAIL");
        final Maybe<Void, IOException> maybe = Maybe.just(3).thenExecute((num, error) -> { throw exception; });

        assertThat(maybe)
          .extracting(SUCCESS, optional(Void.class))
            .isEmpty();

        assertThat(maybe)
          .extracting(FAILURE, optional(IOException.class))
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
        final Maybe<Object, IOException> maybe = Maybe.just(3);

        assertThat(maybe.cast(Integer.class))
          .extracting(SUCCESS, optional(Integer.class))
            .containsInstanceOf(Integer.class)
            .contains(3);

        assertThat(maybe.cast(Integer.class))
          .extracting(FAILURE, optional(IOException.class))
          .isEmpty();
      }
    }

    @Nested
    class when_there_is_NO_success_value_in_the_monad {

      @Nested
      class and_the_failure_exception_is_present {
        @Test
        void returns_the_monad_with_the_failure_exception() {
          final IOException exception = new IOException("FAIL");
          final Maybe<Object, IOException> maybe = Maybe.fail(exception);
  
          assertThat(maybe.cast(Integer.class))
            .extracting(SUCCESS, optional(Integer.class))
              .isEmpty();
  
          assertThat(maybe.cast(Integer.class))
            .extracting(FAILURE, optional(IOException.class))
            .contains(exception);
        }
      }

      @Nested
      class and_the_failure_exception_is_NOT_present {
        
        @Test
        void returns_the_monad_with_nothing() {
          assertThat(Maybe.nothing().cast(Integer.class))
            .extracting(SUCCESS, optional(Integer.class))
              .isEmpty();

          assertThat(Maybe.nothing().cast(Integer.class))
            .extracting(FAILURE, optional(IOException.class))
              .isEmpty();
        }
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
        assertThat(Maybe.fail(new IOException("FAIL")).hasSuccess()).isFalse();
        assertThat(Maybe.nothing().hasSuccess()).isFalse();
      }
    }
  }

  @Nested
  class hasFailure {

    @Nested
    class when_there_is_NO_success_value_in_the_monad {

      @Test
      void returns_true() {
        assertThat(Maybe.fail(new IOException("FAIL")).hasFailure()).isTrue();
      }
    }

    @Nested
    class when_there_is_a_success_value_in_the_monad {

      @Test
      void returns_false() {
        assertThat(Maybe.just("OK").hasFailure()).isFalse();
        assertThat(Maybe.nothing().hasFailure()).isFalse();
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

    @Nested
    class when_there_is_a_failure_exception_in_the_monad {

      @Test
      void returns_false() {
        assertThat(Maybe.fail(new IOException("FAIL")).hasNothing()).isFalse();
      }
    }
  }
  
  private SupplierChecked<String, IOException> throwingOp(boolean shouldThrow) {
    return () -> {
      if (shouldThrow) {
        throw new IOException("FAIL");
      }
  
      return "OK";
    };
  }
}
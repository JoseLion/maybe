package io.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.maybe.util.function.ThrowingRunnable;
import io.github.joselion.testing.Spy;
import io.github.joselion.testing.UnitTest;

@UnitTest class EffectHandlerTest {

  private static final FileSystemException FAILURE = new FileSystemException("FAIL");

  private final ThrowingRunnable<FileSystemException> throwingOp = () -> {
    throw FAILURE;
  };

  private final ThrowingRunnable<RuntimeException> noOp = () -> { };

  @Nested class empty {
    @Test void returns_an_empty_handler() {
      final var handler = EffectHandler.empty();

      assertThat(handler.error()).isEmpty();
    }
  }

  @Nested class failure {
    @Nested class when_the_error_is_not_null {
      @Test void returns_a_handler_with_the_error() {
        final var handler = EffectHandler.failure(FAILURE);

        assertThat(handler.error()).containsSame(FAILURE);
      }
    }

    @Nested class when_the_error_is_null {
      @Test void returns_a_handler_with_a_NullPointerException_error() {
        final var handler = EffectHandler.failure(null);

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
        final var runnableSpy = Spy.runnable(() -> { });

        Maybe.from(noOp).doOnSuccess(runnableSpy);

        verify(runnableSpy).run();
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void never_calls_the_effect_callback() {
        final var runnableSpy = Spy.runnable(() -> { });

        Maybe.from(throwingOp).doOnSuccess(runnableSpy);

        verify(runnableSpy, never()).run();
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
              .effect(() -> { })
              .doOnError(consumerSpy);

            verify(consumerSpy).accept(FAILURE);
          }
        }
      }
    }

    @Nested class when_the_error_is_not_present {
      @Test void never_calls_the_effect_callback() {
        final var runtimeSpy = Spy.<RuntimeException>consumer(error -> { });
        final var throwableSpy = Spy.<Throwable>consumer(error -> { });

        Maybe.from(noOp)
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
            final var consumerSpy = Spy.<FileSystemException>consumer(e -> { });
            final var handler = Maybe
              .from(throwingOp)
              .catchError(FileSystemException.class, consumerSpy);

            assertThat(handler.error()).isEmpty();

            verify(consumerSpy).accept(FAILURE);
          }
        }

        @Nested class and_the_error_is_not_an_instance_of_the_provided_type {
          @Test void never_calls_the_handler_function() {
            final var consumerSpy = Spy.<AccessDeniedException>consumer(e -> { });
            final var handler = Maybe
              .from(throwingOp)
              .catchError(AccessDeniedException.class, consumerSpy);

            assertThat(handler.error()).contains(FAILURE);

            verify(consumerSpy, never()).accept(any());
          }
        }
      }

      @Nested class and_the_error_type_is_not_provided {
        @Nested class and_the_error_matches_the_type_of_the_arg {
          @Test void calls_the_handler_function() {
            final var consumerSpy = Spy.<Throwable>consumer(e -> { });
            final var handler = Maybe.from(throwingOp)
              .catchError(consumerSpy);

            assertThat(handler.error()).isEmpty();

            verify(consumerSpy).accept(FAILURE);
          }
        }

        @Nested class and_the_error_does_not_match_the_type_of_the_arg {
          @Test void calls_the_handler_function() {
            final var consumerSpy = Spy.<Throwable>consumer(e -> { });
            final var handler = Maybe
              .from(throwingOp)
              .effect(() -> { })
              .catchError(consumerSpy);

            assertThat(handler.error()).isEmpty();

            verify(consumerSpy).accept(FAILURE);
          }
        }
      }
    }

    @Nested class when_the_error_is_not_present {
      @Test void never_calls_the_handler_function() {
        final var runtimeSpy = Spy.<RuntimeException>consumer(e -> { });
        final var throwableSpy = Spy.<Throwable>consumer(e -> { });
        final var handlers = List.of(
          Maybe.from(noOp).catchError(RuntimeException.class, runtimeSpy),
          Maybe.from(noOp).catchError(throwableSpy)
        );

        assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
          assertThat(handler.error()).isEmpty();
        });

        verify(runtimeSpy, never()).accept(any());
        verify(throwableSpy, never()).accept(any());
      }
    }
  }

  @Nested class effect {
    @Nested class when_the_error_is_not_present {
      @Test void calls_the_effect_callback_and_returns_a_new_handler() throws FileSystemException {
        final var effectSpy = Spy.lambda(throwingOp);
        final var successSpy = Spy.lambda(throwingOp);
        final var errorSpy = Spy.throwingConsumer((Throwable err) -> throwingOp.run());
        final var handler = Maybe.from(noOp);
        final var newHandlers = List.of(
          handler.effect(effectSpy),
          handler.effect(successSpy, errorSpy)
        );

        assertThat(newHandlers).isNotEmpty().allSatisfy(newHandler -> {
          assertThat(newHandler).isNotSameAs(handler);
          assertThat(newHandler.error()).contains(FAILURE);
        });

        verify(effectSpy).run();
        verify(successSpy).run();
        verify(errorSpy, never()).accept(any());
      }
    }

    @Nested class when_the_error_is_present {
      @Nested class and_the_error_callback_is_provided {
        @Nested class and_the_error_matches_the_type_of_the_arg {
          @Test void calls_only_the_error_callback_and_returns_a_handler_with_the_error() throws FileSystemException {
            final var successSpy = Spy.throwingRunnable(() -> { });
            final var errorSpy = Spy.throwingConsumer((Throwable err) -> throwingOp.run());
            final var handler = Maybe.from(throwingOp).effect(successSpy, errorSpy);

            assertThat(handler.error()).contains(FAILURE);

            verify(successSpy, never()).run();
            verify(errorSpy).accept(FAILURE);
          }
        }

        @Nested class and_the_error_does_not_match_the_type_of_the_arg {
          @Test void calls_only_the_error_callback_and_returns_a_handler_with_the_error() throws FileSystemException {
            final var successSpy = Spy.throwingRunnable(() -> { });
            final var errorSpy = Spy.throwingConsumer((Throwable err) -> throwingOp.run());
            final var handler = Maybe.from(throwingOp).effect(() -> { }).effect(successSpy, errorSpy);

            assertThat(handler.error()).contains(FAILURE);

            verify(successSpy, never()).run();
            verify(errorSpy).accept(FAILURE);
          }
        }
      }

      @Nested class and_the_error_callback_is_not_provided {
        @Test void never_calls_the_effect_callback_and_returns_an_empty_handler() throws FileSystemException {
          final var effectSpy = Spy.lambda(throwingOp);
          final var handler = Maybe.from(throwingOp).effect(effectSpy);

          assertThat(handler.error()).contains(FAILURE);

          verify(effectSpy, never()).run();
        }
      }
    }
  }

  @Nested class orElse {
    @Nested class when_the_error_is_present {
      @Nested class and_the_error_matches_the_type_of_the_arg {
        @Test void calls_the_effect_callback() {
          final var consumerSpy = Spy.<Throwable>consumer(e -> { });
          final var handler = Maybe.from(throwingOp);

          handler.orElse(consumerSpy);

          verify(consumerSpy).accept(FAILURE);
        }
      }

      @Nested class and_the_error_does_not_match_the_type_of_the_arg {
        @Test void calls_the_effect_callback() {
          final var consumerSpy = Spy.<Throwable>consumer(e -> { });
          final var handler = Maybe.from(throwingOp).effect(() -> { });

          handler.orElse(consumerSpy);

          verify(consumerSpy).accept(FAILURE);
        }
      }
    }

    @Nested class when_the_error_is_not_present {
      @Test void never_calls_the_effect_callback() {
        final var consumerSpy = Spy.<Throwable>consumer(e -> { });
        final var handler = Maybe.from(noOp);

        handler.orElse(consumerSpy);

        verify(consumerSpy, never()).accept(any());
      }
    }
  }

  @Nested class orThrow {
    @Nested class when_the_error_is_present {
      @Nested class and_the_mapper_is_not_provided {
        @Test void throws_the_present_error() {
          final var handler = Maybe.from(throwingOp);

          assertThatCode(handler::orThrow).isEqualTo(FAILURE);
        }
      }

      @Nested class and_the_mapper_is_provided {
        @Nested class and_the_error_matches_the_type_of_the_arg {
          @Test void throws_the_error() {
            final var anotherError = new RuntimeException("OTHER");
            final var functionSpy = Spy.function((Throwable err) -> anotherError);
            final var handler = Maybe.from(throwingOp);

            assertThatCode(() -> handler.orThrow(functionSpy)).isEqualTo(anotherError);

            verify(functionSpy).apply(FAILURE);
          }
        }

        @Nested class and_the_error_does_not_match_the_type_of_the_arg {
          @Test void throws_the_error() {
            final var anotherError = new RuntimeException("OTHER");
            final var functionSpy = Spy.function((Throwable err) -> anotherError);
            final var handler = Maybe.from(throwingOp).effect(() -> { });

            assertThatCode(() -> handler.orThrow(functionSpy)).isEqualTo(anotherError);

            verify(functionSpy).apply(FAILURE);
          }
        }
      }
    }

    @Nested class when_the_error_is_not_present {
      @Test void no_exception_is_thrown() {
        final var functionSpy = Spy.function((Throwable err) -> FAILURE);
        final var handler = Maybe.from(noOp);

        assertThatCode(handler::orThrow).doesNotThrowAnyException();
        assertThatCode(() -> handler.orThrow(functionSpy)).doesNotThrowAnyException();

        verify(functionSpy, never()).apply(any());
      }
    }
  }
}

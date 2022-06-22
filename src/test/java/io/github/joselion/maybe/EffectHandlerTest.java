package io.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingRunnable;
import io.github.joselion.testing.Spy;
import io.github.joselion.testing.UnitTest;

@UnitTest class EffectHandlerTest {

  private static final FileSystemException FAIL_EXCEPTION = new FileSystemException("FAIL");

  private final ThrowingRunnable<FileSystemException> throwingOp = () -> {
    throw FAIL_EXCEPTION;
  };

  private final ThrowingRunnable<RuntimeException> noOp = () -> { };

  @Nested class doOnSuccess {
    @Nested class when_the_value_is_present {
      @Test void calls_the_effect_callback() {
        final var runnableSpy = Spy.<Runnable>lambda(() -> { });

        Maybe.fromEffect(noOp).doOnSuccess(runnableSpy);

        verify(runnableSpy, times(1)).run();
      }
    }

    @Nested class when_the_value_is_not_present {
      @Test void never_calls_the_effect_callback() {
        final var runnableSpy = Spy.<Runnable>lambda(() -> { });

        Maybe.fromEffect(throwingOp).doOnSuccess(runnableSpy);

        verify(runnableSpy, never()).run();
      }
    }
  }

  @Nested class doOnError {
    @Nested class when_the_error_is_present {
      @Nested class and_the_error_type_is_provided {
        @Nested class and_the_error_is_an_instance_of_the_provided_type {
          @Test void calls_the_effect_callback() {
            final var consumerSpy = Spy.<Consumer<FileSystemException>>lambda(error -> { });

            Maybe.fromEffect(throwingOp)
              .doOnError(FileSystemException.class, consumerSpy);

            verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
          }
        }

        @Nested class and_the_error_is_not_an_instance_of_the_provided_type {
          @Test void never_calls_the_effect_callback() {
            final var consumerSpy = Spy.<Consumer<RuntimeException>>lambda(error -> { });

            Maybe.fromEffect(throwingOp)
              .doOnError(RuntimeException.class, consumerSpy);

            verify(consumerSpy, never()).accept(any());
          }
        }
      }

      @Nested class and_the_error_type_is_not_provided {
        @Test void calls_the_effect_callback() {
          final var consumerSpy = Spy.<Consumer<FileSystemException>>lambda(error -> { });

          Maybe.fromEffect(throwingOp)
            .doOnError(consumerSpy);

          verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
        }
      }
    }

    @Nested class when_the_error_is_not_present {
      @Test void never_calls_the_effect_callback() {
        final var cunsumerSpy = Spy.<Consumer<RuntimeException>>lambda(error -> { });

        Maybe.fromEffect(noOp)
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
            final var consumerSpy = Spy.<Consumer<FileSystemException>>lambda(e -> { });
            final var handler = Maybe.fromEffect(throwingOp)
              .catchError(FileSystemException.class, consumerSpy);

            assertThat(handler.error()).isEmpty();

            verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
          }
        }

        @Nested class and_the_error_is_not_an_instance_of_the_provided_type {
          @Test void never_calls_the_handler_function() {
            final var consumerSpy = Spy.<Consumer<AccessDeniedException>>lambda(e -> { });
            final var handler = Maybe.fromEffect(throwingOp)
              .catchError(AccessDeniedException.class, consumerSpy);

            assertThat(handler.error()).contains(FAIL_EXCEPTION);

            verify(consumerSpy, never()).accept(any());
          }
        }
      }

      @Nested class and_the_error_type_is_not_provided {
        @Test void calls_the_handler_function() {
          final var consumerSpy = Spy.<Consumer<FileSystemException>>lambda(e -> { });
          final var handler = Maybe.fromEffect(throwingOp)
            .catchError(consumerSpy);

          assertThat(handler.error()).isEmpty();

          verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
        }
      }
    }

    @Nested class when_the_error_is_not_present {
      @Test void never_calls_the_handler_function() {
        final var consumerSpy = Spy.<Consumer<RuntimeException>>lambda(e -> { });
        final var handlers = List.of(
          Maybe.fromEffect(noOp).catchError(RuntimeException.class, consumerSpy),
          Maybe.fromEffect(noOp).catchError(consumerSpy)
        );

        assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
          assertThat(handler.error()).isEmpty();
        });

        verify(consumerSpy, never()).accept(any());
      }
    }
  }

  @Nested class runEffect {
    @Nested class when_the_error_is_not_present {
      @Test void calls_the_effect_callback_and_returns_a_new_handler() throws FileSystemException {
        final var effectSpy = Spy.lambda(throwingOp);
        final var successSpy = Spy.lambda(throwingOp);
        final var errorSpy = Spy.<ThrowingConsumer<RuntimeException, FileSystemException>>lambda(
          err -> throwingOp.run()
        );
        final var handler = Maybe.fromEffect(noOp);
        final var newHandlers = List.of(
          handler.runEffect(effectSpy),
          handler.runEffect(successSpy, errorSpy)
        );

        assertThat(newHandlers).isNotEmpty().allSatisfy(newHandler -> {
          assertThat(newHandler).isNotSameAs(handler);
          assertThat(newHandler.error()).contains(FAIL_EXCEPTION);
        });

        verify(effectSpy, times(1)).run();
        verify(successSpy, times(1)).run();
        verify(errorSpy, never()).accept(any());
      }
    }

    @Nested class when_the_error_is_present {
      @Nested class and_the_error_callback_is_provided {
        @Test void calls_only_the_error_callback_and_returns_a_new_handler() throws FileSystemException {
          final var successSpy = Spy.<ThrowingRunnable<FileSystemException>>lambda(() -> { });
          final var errorSpy = Spy.<ThrowingConsumer<FileSystemException, FileSystemException>>lambda(
            err -> throwingOp.run()
          );
          final var handler = Maybe.fromEffect(throwingOp);
          final var newHandler = handler.runEffect(successSpy, errorSpy);

          assertThat(newHandler).isNotSameAs(handler);
          assertThat(newHandler.error()).contains(FAIL_EXCEPTION);

          verify(successSpy, never()).run();
          verify(errorSpy, times(1)).accept(FAIL_EXCEPTION);
        }
      }

      @Nested class and_the_error_callback_is_not_provided {
        @Test void never_calls_the_effect_callback_and_returns_a_new_empty_handler() throws FileSystemException {
          final var effectSpy = Spy.lambda(throwingOp);
          final var handler = Maybe.fromEffect(throwingOp);
          final var newHandler = handler.runEffect(effectSpy);

          assertThat(newHandler).isNotSameAs(handler);
          assertThat(newHandler.error()).isEmpty();

          verify(effectSpy, never()).run();
        }
      }
    }
  }

  @Nested class orElse {
    @Nested class when_the_error_is_present {
      @Test void calls_the_effect_callback() {
        final var consumerSpy = Spy.<Consumer<FileSystemException>>lambda(e -> { });
        final var handler = Maybe.fromEffect(throwingOp);

        handler.orElse(consumerSpy);

        verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
      }
    }

    @Nested class when_the_error_is_not_present {
      @Test void never_calls_the_effect_callback() {
        final var consumerSpy = Spy.<Consumer<RuntimeException>>lambda(e -> { });
        final var handler = Maybe.fromEffect(noOp);

        handler.orElse(consumerSpy);

        verify(consumerSpy, never()).accept(any());
      }
    }
  }

  @Nested class orThrow {
    @Nested class when_the_error_is_present {
      @Test void throws_the_error() {
        final var anotherError = new RuntimeException("OTHER");
        final var functionSpy = Spy.<Function<FileSystemException, RuntimeException>>lambda(err -> anotherError);
        final var handler = Maybe.fromEffect(throwingOp);

        assertThatThrownBy(handler::orThrow).isEqualTo(FAIL_EXCEPTION);
        assertThatThrownBy(() -> handler.orThrow(functionSpy)).isEqualTo(anotherError);

        verify(functionSpy, times(1)).apply(FAIL_EXCEPTION);
      }
    }

    @Nested class when_the_error_is_not_present {
      @Test void no_exception_is_thrown() {
        final var functionSpy = Spy.<Function<RuntimeException, FileSystemException>>lambda(err -> FAIL_EXCEPTION);
        final var handler = Maybe.fromEffect(noOp);

        assertThatCode(handler::orThrow).doesNotThrowAnyException();
        assertThatCode(() -> handler.orThrow(functionSpy)).doesNotThrowAnyException();

        verify(functionSpy, never()).apply(any());
      }
    }
  }
}

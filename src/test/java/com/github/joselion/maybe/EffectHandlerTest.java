package com.github.joselion.maybe;

import static com.github.joselion.maybe.helpers.Helpers.spyLambda;
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

import com.github.joselion.maybe.helpers.UnitTest;
import com.github.joselion.maybe.util.RunnableChecked;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest class EffectHandlerTest {

  private static final FileSystemException FAIL_EXCEPTION = new FileSystemException("FAIL");

  private final RunnableChecked<FileSystemException> throwingOp = () -> {
    throw FAIL_EXCEPTION;
  };

  private final RunnableChecked<RuntimeException> noOp = () -> { };

  @Nested class doOnError {
    @Nested class when_the_error_is_present {
      @Nested class and_the_error_type_is_provided {
        @Nested class and_the_error_is_an_instance_of_the_provided_type {
          @Test void calls_the_effect_callback() {
            final Consumer<FileSystemException> consumerSpy = spyLambda(error -> { });
            final Runnable runnableSpy = spyLambda(() -> { });

            Maybe.fromRunnable(throwingOp)
              .doOnError(FileSystemException.class, consumerSpy)
              .doOnError(FileSystemException.class, runnableSpy);

            verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
            verify(runnableSpy, times(1)).run();;
          }
        }

        @Nested class and_the_error_is_NOT_an_instance_of_the_provided_type {
          @Test void never_calls_the_effect_callback() {
            final Consumer<RuntimeException> consumerSpy = spyLambda(error -> { });
            final Runnable runnableSpy = spyLambda(() -> { });

            Maybe.fromRunnable(throwingOp)
              .doOnError(RuntimeException.class, consumerSpy)
              .doOnError(RuntimeException.class, runnableSpy);

            verify(consumerSpy, never()).accept(any());
            verify(runnableSpy, never()).run();
          }
        }
      }

      @Nested class and_the_error_type_is_NOT_provided {
        @Test void calls_the_effect_callback() {
          final Consumer<FileSystemException> consumerSpy = spyLambda(error -> { });
          final Runnable runnableSpy = spyLambda(() -> { });

          Maybe.fromRunnable(throwingOp)
            .doOnError(consumerSpy)
            .doOnError(runnableSpy);

          verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
          verify(runnableSpy, times(1)).run();
        }
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void never_calls_the_effect_callback() {
        final Consumer<RuntimeException> cunsumerSpy = spyLambda(error -> { });
        final Runnable runnableSpy = spyLambda(() -> { });

        Maybe.fromRunnable(noOp)
          .doOnError(RuntimeException.class, cunsumerSpy)
          .doOnError(RuntimeException.class, runnableSpy)
          .doOnError(cunsumerSpy)
          .doOnError(runnableSpy);

        verify(cunsumerSpy, never()).accept(any());
        verify(runnableSpy, never()).run();
      }
    }
  }

  @Nested class catchError {
    @Nested class when_the_error_is_present {
      @Nested class and_the_error_type_is_provided {
        @Nested class and_the_error_is_an_instance_of_the_provided_type {
          @Test void calls_the_handler_function() {
            final Consumer<FileSystemException> consumerSpy = spyLambda(e -> { });
            final Runnable runnableSpy = spyLambda(() -> { });
            final List<EffectHandler<FileSystemException>> handlers = List.of(
              Maybe.fromRunnable(throwingOp).catchError(FileSystemException.class, consumerSpy),
              Maybe.fromRunnable(throwingOp).catchError(FileSystemException.class, runnableSpy)
            );

            assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
              assertThat(handler.error()).isEmpty();
            });

            verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
            verify(runnableSpy, times(1)).run();
          }
        }

        @Nested class and_the_error_is_NOT_an_instance_of_the_provided_type {
          @Test void never_calls_the_handler_function() {
            final Consumer<AccessDeniedException> consumerSpy = spyLambda(e -> { });
            final Runnable runnableSpy = spyLambda(() -> { });
            final List<EffectHandler<FileSystemException>> handlers = List.of(
              Maybe.fromRunnable(throwingOp).catchError(AccessDeniedException.class, consumerSpy),
              Maybe.fromRunnable(throwingOp).catchError(AccessDeniedException.class, runnableSpy)
            );

            assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
              assertThat(handler.error()).contains(FAIL_EXCEPTION);
            });

            verify(consumerSpy, never()).accept(any());
            verify(runnableSpy, never()).run();
          }
        }
      }

      @Nested class and_the_error_type_is_NOT_provided {
        @Test void calls_the_handler_function() {
          final Consumer<FileSystemException> consumerSpy = spyLambda(e -> { });
          final Runnable runnableSpy = spyLambda(() -> { });
          final List<EffectHandler<FileSystemException>> handlers = List.of(
            Maybe.fromRunnable(throwingOp).catchError(consumerSpy),
            Maybe.fromRunnable(throwingOp).catchError(runnableSpy)
          );

          assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
            assertThat(handler.error()).isEmpty();
          });

          verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
          verify(runnableSpy, times(1)).run();
        }
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void never_calls_the_handler_function() {
        final Consumer<RuntimeException> consumerSpy = spyLambda(e -> { });
        final Runnable runnableSpy = spyLambda(() -> { });
        final List<EffectHandler<RuntimeException>> handlers = List.of(
          Maybe.fromRunnable(noOp).catchError(RuntimeException.class, consumerSpy),
          Maybe.fromRunnable(noOp).catchError(RuntimeException.class, runnableSpy),
          Maybe.fromRunnable(noOp).catchError(consumerSpy),
          Maybe.fromRunnable(noOp).catchError(runnableSpy)
        );

        assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
          assertThat(handler.error()).isEmpty();
        });

        verify(consumerSpy, never()).accept(any());
        verify(runnableSpy, never()).run();
      }
    }
  }

  @Nested class onErrorThrow {
    @Nested class when_the_error_is_present {
      @Test void throws_an_exception() {
        final RuntimeException anotherError = new RuntimeException("OTHER");
        final Function<FileSystemException, RuntimeException> functionSpy = spyLambda(err -> anotherError);
        final EffectHandler<FileSystemException> handler = Maybe.fromRunnable(throwingOp);

        assertThatThrownBy(handler::onErrorThrow).isEqualTo(FAIL_EXCEPTION);
        assertThatThrownBy(() -> handler.onErrorThrow(functionSpy)).isEqualTo(anotherError);

        verify(functionSpy, times(1)).apply(FAIL_EXCEPTION);
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void no_exception_is_thrown() {
        final Function<RuntimeException, FileSystemException> functionSpy = spyLambda(err -> FAIL_EXCEPTION);
        final EffectHandler<RuntimeException> handler = Maybe.fromRunnable(noOp);

        assertThatCode(handler::onErrorThrow).doesNotThrowAnyException();
        assertThatCode(() -> handler.onErrorThrow(functionSpy)).doesNotThrowAnyException();

        verify(functionSpy, never()).apply(any());
      }
    }
  }

  @Nested class toMaybe {
    @Test void returns_a_maybe_with_nothing() {
      assertThat(Maybe.fromRunnable(throwingOp).toMaybe().value())
        .isEmpty();
    }
  }
}

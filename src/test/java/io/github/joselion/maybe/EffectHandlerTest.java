package io.github.joselion.maybe;

import static io.github.joselion.testing.Helpers.spyLambda;
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

import io.github.joselion.maybe.util.ConsumerChecked;
import io.github.joselion.maybe.util.RunnableChecked;
import io.github.joselion.testing.UnitTest;

@UnitTest class EffectHandlerTest {

  private static final FileSystemException FAIL_EXCEPTION = new FileSystemException("FAIL");

  private final RunnableChecked<FileSystemException> throwingOp = () -> {
    throw FAIL_EXCEPTION;
  };

  private final RunnableChecked<RuntimeException> noOp = () -> { };

  @Nested class doOnSuccess {
    @Nested class when_the_value_is_present {
      @Test void calls_the_effect_callback() {
        final Runnable runnableSpy = spyLambda(() -> { });

        Maybe.fromEffect(noOp).doOnSuccess(runnableSpy);

        verify(runnableSpy, times(1)).run();
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void never_calls_the_effect_callback() {
        final Runnable runnableSpy = spyLambda(() -> { });

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
            final Consumer<FileSystemException> consumerSpy = spyLambda(error -> { });

            Maybe.fromEffect(throwingOp)
              .doOnError(FileSystemException.class, consumerSpy);

            verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
          }
        }

        @Nested class and_the_error_is_NOT_an_instance_of_the_provided_type {
          @Test void never_calls_the_effect_callback() {
            final Consumer<RuntimeException> consumerSpy = spyLambda(error -> { });

            Maybe.fromEffect(throwingOp)
              .doOnError(RuntimeException.class, consumerSpy);

            verify(consumerSpy, never()).accept(any());
          }
        }
      }

      @Nested class and_the_error_type_is_NOT_provided {
        @Test void calls_the_effect_callback() {
          final Consumer<FileSystemException> consumerSpy = spyLambda(error -> { });

          Maybe.fromEffect(throwingOp)
            .doOnError(consumerSpy);

          verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
        }
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void never_calls_the_effect_callback() {
        final Consumer<RuntimeException> cunsumerSpy = spyLambda(error -> { });

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
            final Consumer<FileSystemException> consumerSpy = spyLambda(e -> { });
            final EffectHandler<FileSystemException> handler = Maybe.fromEffect(throwingOp)
              .catchError(FileSystemException.class, consumerSpy);

            assertThat(handler.error()).isEmpty();

            verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
          }
        }

        @Nested class and_the_error_is_NOT_an_instance_of_the_provided_type {
          @Test void never_calls_the_handler_function() {
            final Consumer<AccessDeniedException> consumerSpy = spyLambda(e -> { });
            final EffectHandler<FileSystemException> handler = Maybe.fromEffect(throwingOp)
              .catchError(AccessDeniedException.class, consumerSpy);

            assertThat(handler.error()).contains(FAIL_EXCEPTION);

            verify(consumerSpy, never()).accept(any());
          }
        }
      }

      @Nested class and_the_error_type_is_NOT_provided {
        @Test void calls_the_handler_function() {
          final Consumer<FileSystemException> consumerSpy = spyLambda(e -> { });
          final EffectHandler<FileSystemException> handler = Maybe.fromEffect(throwingOp)
            .catchError(consumerSpy);

          assertThat(handler.error()).isEmpty();

          verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
        }
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void never_calls_the_handler_function() {
        final Consumer<RuntimeException> consumerSpy = spyLambda(e -> { });
        final List<EffectHandler<RuntimeException>> handlers = List.of(
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
    @Nested class when_the_error_is_NOT_present {
      @Test void calls_the_effect_callback_and_returns_a_new_handler() throws FileSystemException {
        final RunnableChecked<FileSystemException> effectSpy = spyLambda(throwingOp);
        final RunnableChecked<FileSystemException> successSpy = spyLambda(throwingOp);
        final ConsumerChecked<RuntimeException, FileSystemException> errorSpy = spyLambda(err -> throwingOp.run());
        final EffectHandler<RuntimeException> handler = Maybe.fromEffect(noOp);
        final List<EffectHandler<FileSystemException>> newHandlers = List.of(
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
          final RunnableChecked<FileSystemException> successSpy = spyLambda(() -> { });
          final ConsumerChecked<FileSystemException, FileSystemException> errorSpy = spyLambda(err -> throwingOp.run());
          final EffectHandler<FileSystemException> handler = Maybe.fromEffect(throwingOp);
          final EffectHandler<FileSystemException> newHandler = handler.runEffect(successSpy, errorSpy);

          assertThat(newHandler).isNotSameAs(handler);
          assertThat(newHandler.error()).contains(FAIL_EXCEPTION);

          verify(successSpy, never()).run();
          verify(errorSpy, times(1)).accept(FAIL_EXCEPTION);
        }
      }

      @Nested class and_the_error_callback_is_NOT_provided {
        @Test void never_calls_the_effect_callback_and_returns_a_new_empty_handler() throws FileSystemException {
          final RunnableChecked<FileSystemException> effectSpy = spyLambda(throwingOp);
          final EffectHandler<FileSystemException> handler = Maybe.fromEffect(throwingOp);
          final EffectHandler<FileSystemException> newHandler = handler.runEffect(effectSpy);

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
        final Consumer<FileSystemException> consumerSpy = spyLambda(e -> { });
        final EffectHandler<FileSystemException> handler = Maybe.fromEffect(throwingOp);

        handler.orElse(consumerSpy);

        verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void never_calls_the_effect_callback() {
        final Consumer<RuntimeException> consumerSpy = spyLambda(e -> { });
        final EffectHandler<RuntimeException> handler = Maybe.fromEffect(noOp);

        handler.orElse(consumerSpy);

        verify(consumerSpy, never()).accept(any());
      }
    }
  }

  @Nested class orThrow {
    @Nested class when_the_error_is_present {
      @Test void throws_the_error() {
        final RuntimeException anotherError = new RuntimeException("OTHER");
        final Function<FileSystemException, RuntimeException> functionSpy = spyLambda(err -> anotherError);
        final EffectHandler<FileSystemException> handler = Maybe.fromEffect(throwingOp);

        assertThatThrownBy(handler::orThrow).isEqualTo(FAIL_EXCEPTION);
        assertThatThrownBy(() -> handler.orThrow(functionSpy)).isEqualTo(anotherError);

        verify(functionSpy, times(1)).apply(FAIL_EXCEPTION);
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void no_exception_is_thrown() {
        final Function<RuntimeException, FileSystemException> functionSpy = spyLambda(err -> FAIL_EXCEPTION);
        final EffectHandler<RuntimeException> handler = Maybe.fromEffect(noOp);

        assertThatCode(handler::orThrow).doesNotThrowAnyException();
        assertThatCode(() -> handler.orThrow(functionSpy)).doesNotThrowAnyException();

        verify(functionSpy, never()).apply(any());
      }
    }
  }

  @Nested class toMaybe {
    @Test void returns_a_maybe_with_nothing() {
      assertThat(Maybe.fromEffect(throwingOp).toMaybe().value())
        .isEmpty();
    }
  }
}

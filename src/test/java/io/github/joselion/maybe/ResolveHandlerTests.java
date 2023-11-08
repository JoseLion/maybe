package io.github.joselion.maybe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.INPUT_STREAM;
import static org.assertj.core.api.InstanceOfAssertFactories.THROWABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.maybe.util.function.ThrowingConsumer;
import io.github.joselion.maybe.util.function.ThrowingFunction;
import io.github.joselion.maybe.util.function.ThrowingSupplier;
import io.github.joselion.testing.Spy;
import io.github.joselion.testing.UnitTest;

@UnitTest class ResolveHandlerTests {

  private static final String OK = "OK";

  private static final String OTHER = "OTHER";

  private static final FileSystemException FAIL_EXCEPTION = new FileSystemException("FAIL");

  private final ThrowingSupplier<String, FileSystemException> throwingOp = () -> {
    throw FAIL_EXCEPTION;
  };

  private final ThrowingSupplier<String, RuntimeException> okOp = () -> OK;

  @Nested class doOnSuccess {
    @Nested class when_the_value_is_present {
      @Test void calls_the_effect_callback() {
        final var consumerSpy = Spy.<String>consumer(v -> { });

        Maybe.fromResolver(okOp)
          .doOnSuccess(consumerSpy);

        verify(consumerSpy, times(1)).accept(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Test void never_calls_the_effect_callback() {
        final var consumerSpy = Spy.<String>consumer(v -> { });

        Maybe.fromResolver(throwingOp)
          .doOnSuccess(consumerSpy);

        verify(consumerSpy, never()).accept(any());
      }
    }
  }

  @Nested class doOnError {
    @Nested class when_the_error_is_present {
      @Nested class and_the_error_type_is_provided {
        @Nested class and_the_error_is_an_instance_of_the_provided_type {
          @Test void calls_the_effect_callback() {
            final var consumerSpy = Spy.<FileSystemException>consumer(error -> { });

            Maybe.fromResolver(throwingOp)
              .doOnError(FileSystemException.class, consumerSpy);

            verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
          }
        }

        @Nested class and_the_error_is_not_an_instance_of_the_provided_type {
          @Test void never_calls_the_effect_callback() {
            final var consumerSpy = Spy.<RuntimeException>consumer(error -> { });

            Maybe.fromResolver(throwingOp)
              .doOnError(RuntimeException.class, consumerSpy);

            verify(consumerSpy, never()).accept(any());
          }
        }
      }

      @Nested class and_the_error_type_is_not_provided {
        @Test void calls_the_effect_callback() {
          final var consumerSpy = Spy.<FileSystemException>consumer(error -> { });

          Maybe.fromResolver(throwingOp)
            .doOnError(consumerSpy);

          verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
        }
      }
    }

    @Nested class when_the_value_is_present {
      @Test void never_calls_the_effect_callback() {
        final var cunsumerSpy = Spy.<RuntimeException>consumer(error -> { });

        Maybe.fromResolver(okOp)
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
            final var functionSpy = Spy.function((FileSystemException e) -> OK);
            final var handler = Maybe.fromResolver(throwingOp)
              .catchError(FileSystemException.class, functionSpy);

            assertThat(handler.success()).contains(OK);
            assertThat(handler.error()).isEmpty();

            verify(functionSpy, times(1)).apply(FAIL_EXCEPTION);
          }
        }

        @Nested class and_the_error_is_not_an_instance_of_the_provided_type {
          @Test void never_calls_the_handler_function() {
            final var functionSpy = Spy.function((AccessDeniedException e) -> OK);
            final var handler = Maybe.fromResolver(throwingOp)
              .catchError(AccessDeniedException.class, functionSpy);

            assertThat(handler.success()).isEmpty();
            assertThat(handler.error()).contains(FAIL_EXCEPTION);

            verify(functionSpy, never()).apply(any());
          }
        }
      }

      @Nested class and_the_error_type_is_not_provided {
        @Test void calls_the_handler_function() {
          final var handlerSpy = Spy.function((FileSystemException e) -> OK);
          final var resolver = Maybe.fromResolver(throwingOp)
            .catchError(handlerSpy);

          assertThat(resolver.success()).contains(OK);
          assertThat(resolver.error()).isEmpty();

          verify(handlerSpy, times(1)).apply(FAIL_EXCEPTION);
        }
      }
    }

    @Nested class when_the_value_is_present {
      @Test void never_calls_the_handler_function() {
        final var functionSpy = Spy.function((RuntimeException e) -> OK);
        final var resolvers = List.of(
          Maybe.fromResolver(okOp).catchError(RuntimeException.class, functionSpy),
          Maybe.fromResolver(okOp).catchError(functionSpy)
        );

        assertThat(resolvers).isNotEmpty().allSatisfy(resolver -> {
          assertThat(resolver.success()).contains(OK);
          assertThat(resolver.error()).isEmpty();
        });

        verify(functionSpy, never()).apply(any());
      }
    }
  }

  @Nested class resolve {
    @Nested class when_the_value_is_present {
      @Test void calls_the_resolver_callback_and_returns_a_new_handler() {
        final var resolverSpy = Spy.<ThrowingFunction<String, Integer, RuntimeException>>lambda(String::length);
        final var successSpy = Spy.<ThrowingFunction<String, Integer, RuntimeException>>lambda(String::length);
        final var errorSpy = Spy.<ThrowingFunction<RuntimeException, Integer, RuntimeException>>lambda(e -> -1);
        final var handlers = List.of(
          Maybe.fromResolver(okOp).resolve(resolverSpy),
          Maybe.fromResolver(okOp).resolve(successSpy, errorSpy)
        );

        assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
          assertThat(handler.success()).contains(OK.length());
          assertThat(handler.error()).isEmpty();
        });

        verify(resolverSpy, times(1)).apply(OK);
        verify(successSpy, times(1)).apply(OK);
        verify(errorSpy, never()).apply(any());
      }
    }

    @Nested class when_the_error_is_present {
      @Nested class and_the_error_resolver_is_not_provided {
        @Test void never_calls_the_resolver_callback_and_returns_a_handler_with_the_error() {
          final var successSpy = Spy.<ThrowingFunction<String, Integer, RuntimeException>>lambda(String::length);
          final var handler = Maybe.fromResolver(throwingOp)
            .resolve(successSpy);

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error())
            .get(THROWABLE)
            .isExactlyInstanceOf(FAIL_EXCEPTION.getClass())
            .isEqualTo(FAIL_EXCEPTION);

          verify(successSpy, never()).apply(any());
        }
      }

      @Nested class and_the_error_resolver_is_provided {
        @Test void call_only_the_error_callback_and_returns_a_new_effect_handler() {
          final var successSpy = Spy.<ThrowingFunction<String, Integer, RuntimeException>>lambda(String::length);
          final var errorSpy = Spy.<ThrowingFunction<FileSystemException, Integer, RuntimeException>>lambda(e -> -1);
          final var handler = Maybe.fromResolver(throwingOp)
            .resolve(successSpy, errorSpy);

          assertThat(handler.success()).contains(-1);
          assertThat(handler.error()).isEmpty();

          verify(successSpy, never()).apply(any());
          verify(errorSpy, times(1)).apply(FAIL_EXCEPTION);
        }
      }
    }
  }

  @Nested class runEffect {
    @Nested class when_the_value_is_present {
      @Test void calls_the_resolver_callback_and_returns_a_new_handler() throws FileSystemException {
        final var effectSpy = Spy.<ThrowingConsumer<String, FileSystemException>>lambda(v -> throwingOp.get());
        final var successSpy = Spy.<ThrowingConsumer<String, FileSystemException>>lambda(v -> throwingOp.get());
        final var errorSpy = Spy.<ThrowingConsumer<RuntimeException, FileSystemException>>lambda(err -> { });
        final var handler = Maybe.fromResolver(okOp);
        final var newHandlers = List.of(
          handler.runEffect(effectSpy),
          handler.runEffect(successSpy, errorSpy)
        );

        assertThat(newHandlers).isNotEmpty().allSatisfy(newHandler -> {
          assertThat(newHandler.error()).contains(FAIL_EXCEPTION);
        });

        verify(effectSpy, times(1)).accept(OK);
        verify(successSpy, times(1)).accept(OK);
        verify(errorSpy, never()).accept(any());
      }
    }

    @Nested class when_the_error_is_present {
      @Nested class and_the_error_callback_is_provided {
        @Test void calls_only_the_error_callback_and_returns_a_new_handler() throws FileSystemException {
          final var successSpy = Spy.<ThrowingConsumer<String, FileSystemException>>lambda(v -> { });
          final var errorSpy = Spy.<ThrowingConsumer<FileSystemException, FileSystemException>>lambda(
            err -> throwingOp.get()
          );
          final var handler = Maybe.fromResolver(throwingOp);
          final var newHandler = handler.runEffect(successSpy, errorSpy);

          assertThat(newHandler.error()).contains(FAIL_EXCEPTION);

          verify(successSpy, never()).accept(any());
          verify(errorSpy, times(1)).accept(FAIL_EXCEPTION);
        }
      }

      @Nested class and_the_error_callback_is_not_provided {
        @Test void never_calls_the_effect_callback_and_returns_a_handler_with_the_error() throws FileSystemException {
          final var effectSpy = Spy.<ThrowingConsumer<String, FileSystemException>>lambda(v -> throwingOp.get());
          final var handler = Maybe.fromResolver(throwingOp);
          final var newHandler = handler.runEffect(effectSpy);

          assertThat(newHandler.error())
            .get(THROWABLE)
            .isExactlyInstanceOf(FAIL_EXCEPTION.getClass())
            .isEqualTo(FAIL_EXCEPTION);

          verify(effectSpy, never()).accept(any());
        }
      }
    }
  }

  @Nested class map {
    @Nested class when_the_value_is_present {
      @Test void returns_a_new_handler_applying_the_mapper_function() {
        final var handler = ResolveHandler.ofSuccess("Hello world!")
          .map(String::length);

        assertThat(handler.success()).contains(12);

        assertThat(handler.error()).isEmpty();
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_new_handler_with_the_previous_error() {
        final var handler = ResolveHandler.ofError(FAIL_EXCEPTION)
          .map(Object::toString);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).contains(FAIL_EXCEPTION);
      }
    }
  }

  @Nested class cast {
    @Nested class when_the_value_is_present {
      @Nested class and_the_object_can_be_cast {
        @Test void returns_a_new_handler_with_the_cast_value() {
          final var anyValue = (Object) "Hello";
          final var handler = ResolveHandler.ofSuccess(anyValue)
            .cast(String.class);

          assertThat(handler.success()).contains("Hello");
          assertThat(handler.error()).isEmpty();
        }
      }

      @Nested class and_the_object_can_not_be_cast {
        @Test void returns_a_new_handler_with_the_cast_exception() {
          final var handler = ResolveHandler.ofSuccess(3)
            .cast(String.class);

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error())
            .get(THROWABLE)
            .isExactlyInstanceOf(ClassCastException.class)
            .hasMessage("Cannot cast java.lang.Integer to java.lang.String");
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_new_handler_with_a_cast_exception() {
        final var handler = ResolveHandler.ofError(FAIL_EXCEPTION)
            .cast(String.class);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(ClassCastException.class)
          .hasMessage(FAIL_EXCEPTION.getMessage());
      }
    }
  }

  @Nested class orElse {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() {
        final var handler = Maybe.fromResolver(okOp);

        assertThat(handler.orElse(OTHER)).isEqualTo(OK);
        assertThat(handler.orElse(RuntimeException::getMessage)).isEqualTo(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_the_provided_value() {
        final var handler = Maybe.fromResolver(throwingOp);

        assertThat(handler.orElse(OTHER)).isEqualTo(OTHER);
        assertThat(handler.orElse(FileSystemException::getMessage)).isEqualTo(FAIL_EXCEPTION.getMessage());
      }
    }
  }

  @Nested class orElseGet {
    @Nested class when_the_value_is_present {
      @Test void never_evaluates_the_supplier_and_returns_the_value() {
        final var supplierSpy = Spy.supplier(() -> OTHER);
        final var handler = Maybe.fromResolver(okOp);

        assertThat(handler.orElseGet(supplierSpy)).isEqualTo(OK);

        verify(supplierSpy, never()).get();
      }
    }

    @Nested class when_the_error_is_present {
      @Test void evaluates_the_supplier_and_returns_the_produced_value() {
        final var supplierSpy = Spy.supplier(() -> OTHER);
        final var handler = Maybe.fromResolver(throwingOp);

        assertThat(handler.orElseGet(supplierSpy)).isEqualTo(OTHER);

        verify(supplierSpy, times(1)).get();
      }
    }
  }

  @Nested class orNull {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() {
        final var handler = Maybe.fromResolver(okOp);

        assertThat(handler.orNull()).isEqualTo(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_null() {
        final var handler = Maybe.fromResolver(throwingOp);

        assertThat(handler.orNull()).isNull();
      }
    }
  }

  @Nested class orThrow {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() throws FileSystemException {
        final var functionSpy = Spy.function((RuntimeException error) -> FAIL_EXCEPTION);
        final var handler = Maybe.fromResolver(okOp);

        assertThat(handler.orThrow()).isEqualTo(OK);
        assertThat(handler.orThrow(functionSpy)).isEqualTo(OK);

        verify(functionSpy, never()).apply(any());
      }
    }

    @Nested class when_the_error_is_present {
      @Test void throws_the_error() {
        final var anotherError = new RuntimeException(OTHER);
        final var functionSpy = Spy.function((FileSystemException error) -> anotherError);
        final var handler = Maybe.fromResolver(throwingOp);

        assertThatThrownBy(handler::orThrow).isEqualTo(FAIL_EXCEPTION);
        assertThatThrownBy(() -> handler.orThrow(functionSpy)).isEqualTo(anotherError);

        verify(functionSpy, times(1)).apply(FAIL_EXCEPTION);
      }
    }
  }

  @Nested class toMaybe {
    @Nested class when_the_value_is_present {
      @Test void returns_a_maybe_with_the_value() {
        assertThat(Maybe.fromResolver(okOp).toMaybe().value())
          .contains(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_maybe_with_nothing() {
        assertThat(Maybe.fromResolver(throwingOp).toMaybe().value())
          .isEmpty();
      }
    }
  }

  @Nested class toOptional {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value_wrapped_in_an_optinal() {
        assertThat(Maybe.fromResolver(okOp).toOptional()).contains(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_an_empty_optional() {
        assertThat(Maybe.fromResolver(throwingOp).toOptional()).isEmpty();
      }
    }
  }

  @Nested class toEither {
    @Nested class when_the_value_is_present {
      @Test void returns_an_Either_with_the_value_on_its_right_side() {
        final var either = Maybe.fromResolver(okOp).toEither();

        assertThat(either.isLeft()).isFalse();
        assertThat(either.isRight()).isTrue();
        assertThat(either.leftOrNull()).isNull();
        assertThat(either.rightOrNull()).isEqualTo(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_an_Either_with_the_error_on_its_left_side() {
        final var either = Maybe.fromResolver(throwingOp).toEither();

        assertThat(either.isLeft()).isTrue();
        assertThat(either.isRight()).isFalse();
        assertThat(either.leftOrNull()).isEqualTo(FAIL_EXCEPTION);
        assertThat(either.rightOrNull()).isNull();
      }
    }
  }

  @Nested class mapToResource {
    @Nested class when_the_resource_is_present {
      @Test void returns_a_resource_holder_with_the_mapped_value() {
        final var path = "./src/test/resources/readTest.txt";
        final var holder = Maybe.just(path)
          .resolve(FileInputStream::new)
          .mapToResource(Function.identity());

        assertThat(holder.resource())
          .get(INPUT_STREAM)
          .hasContent("foo");
        assertThat(holder.error()).isEmpty();
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_resource_holder_with_the_propagated_error() {
        final var holder = Maybe.just("invalidFile.txt")
          .resolve(FileInputStream::new)
          .mapToResource(Function.identity());

        assertThat(holder.resource()).isEmpty();
        assertThat(holder.error())
          .get(THROWABLE)
          .isExactlyInstanceOf(FileNotFoundException.class)
          .hasMessageStartingWith("invalidFile.txt");
      }
    }
  }

  @Nested class solveResource {
    @Nested class when_the_value_is_present {
      @Nested class and_the_solver_does_not_throw {
        @Test void returns_a_ResourceHolder_with_the_resource() {
          final var path = "./src/test/resources/readTest.txt";
          final var holder = Maybe
            .just(path)
            .resolve(ThrowingFunction.identity())
            .solveResource(FileInputStream::new);

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
        @Test void returns_a_ResourceHolder_with_the_thrown_exception() {
          final var holder = Maybe
            .just("invalid.txt")
            .resolve(ThrowingFunction.identity())
            .solveResource(FileInputStream::new);

          assertThat(holder.resource()).isEmpty();
          assertThat(holder.error())
            .isPresent()
            .containsInstanceOf(FileNotFoundException.class);
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_ResourceHolder_with_the_propagated_error() {
        final var holder = Maybe
          .fromResolver(throwingOp)
          .solveResource(FileInputStream::new);

        assertThat(holder.resource()).isEmpty();
        assertThat(holder.error())
          .isPresent()
          .containsInstanceOf(FileSystemException.class);
      }
    }
  }
}

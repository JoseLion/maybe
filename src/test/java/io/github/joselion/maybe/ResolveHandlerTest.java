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
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.maybe.exceptions.WrappingException;
import io.github.joselion.maybe.util.ConsumerChecked;
import io.github.joselion.maybe.util.FunctionChecked;
import io.github.joselion.maybe.util.SupplierChecked;
import io.github.joselion.testing.Spy;
import io.github.joselion.testing.UnitTest;

@UnitTest class ResolveHandlerTest {

  private static final String OK = "OK";

  private static final String OTHER = "OTHER";

  private static final FileSystemException FAIL_EXCEPTION = new FileSystemException("FAIL");

  private final SupplierChecked<String, FileSystemException> throwingOp = () -> {
    throw FAIL_EXCEPTION;
  };

  private final SupplierChecked<String, RuntimeException> okOp = () -> OK;

  @Nested class doOnSuccess {
    @Nested class when_the_value_is_present {
      @Test void calls_the_effect_callback() {
        final var consumerSpy = Spy.<Consumer<String>>lambda(v -> { });

        Maybe.fromResolver(okOp)
          .doOnSuccess(consumerSpy);

        verify(consumerSpy, times(1)).accept(OK);
      }
    }

    @Nested class when_the_error_is_present {
      @Test void never_calls_the_effect_callback() {
        final var consumerSpy = Spy.<Consumer<String>>lambda(v -> { });

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
            final var consumerSpy = Spy.<Consumer<FileSystemException>>lambda(error -> { });

            Maybe.fromResolver(throwingOp)
              .doOnError(FileSystemException.class, consumerSpy);

            verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
          }
        }

        @Nested class and_the_error_is_NOT_an_instance_of_the_provided_type {
          @Test void never_calls_the_effect_callback() {
            final var consumerSpy = Spy.<Consumer<RuntimeException>>lambda(error -> { });

            Maybe.fromResolver(throwingOp)
              .doOnError(RuntimeException.class, consumerSpy);

            verify(consumerSpy, never()).accept(any());
          }
        }
      }

      @Nested class and_the_error_type_is_NOT_provided {
        @Test void calls_the_effect_callback() {
          final var consumerSpy = Spy.<Consumer<FileSystemException>>lambda(error -> { });

          Maybe.fromResolver(throwingOp)
            .doOnError(consumerSpy);

          verify(consumerSpy, times(1)).accept(FAIL_EXCEPTION);
        }
      }
    }

    @Nested class when_the_value_is_present {
      @Test void never_calls_the_effect_callback() {
        final var cunsumerSpy = Spy.<Consumer<RuntimeException>>lambda(error -> { });

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
            final var functionSpy = Spy.<Function<FileSystemException, String>>lambda(e -> OK);
            final var handler = Maybe.fromResolver(throwingOp)
              .catchError(FileSystemException.class, functionSpy);

            assertThat(handler.success()).contains(OK);
            assertThat(handler.error()).isEmpty();

            verify(functionSpy, times(1)).apply(FAIL_EXCEPTION);
          }
        }

        @Nested class and_the_error_is_NOT_an_instance_of_the_provided_type {
          @Test void never_calls_the_handler_function() {
            final var functionSpy = Spy.<Function<AccessDeniedException, String>>lambda(e -> OK);
            final var handler = Maybe.fromResolver(throwingOp)
              .catchError(AccessDeniedException.class, functionSpy);

            assertThat(handler.success()).isEmpty();
            assertThat(handler.error()).contains(FAIL_EXCEPTION);

            verify(functionSpy, never()).apply(any());
          }
        }
      }

      @Nested class and_the_error_type_is_NOT_provided {
        @Test void calls_the_handler_function() {
          final var handlerSpy = Spy.<Function<FileSystemException, String>>lambda(e -> OK);
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
        final var functionSpy = Spy.<Function<RuntimeException, String>>lambda(e -> OK);
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
        final var resolverSpy = Spy.<FunctionChecked<String, Integer, RuntimeException>>lambda(String::length);
        final var successSpy = Spy.<FunctionChecked<String, Integer, RuntimeException>>lambda(String::length);
        final var errorSpy = Spy.<FunctionChecked<RuntimeException, Integer, RuntimeException>>lambda(e -> -1);
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
      @Nested class and_the_error_resolver_is_NOT_provided {
        @Test void never_calls_the_resolver_callback_and_creates_a_handler_with_nothing() {
          final var successSpy = Spy.<FunctionChecked<String, Integer, RuntimeException>>lambda(String::length);
          final var handler = Maybe.fromResolver(throwingOp)
            .resolve(successSpy);

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error()).isEmpty();

          verify(successSpy, never()).apply(any());
        }
      }

      @Nested class and_the_error_resolver_is_provided {
        @Test void call_only_the_error_callback_and_returns_a_new_effect_handler() {
          final var successSpy = Spy.<FunctionChecked<String, Integer, RuntimeException>>lambda(String::length);
          final var errorSpy = Spy.<FunctionChecked<FileSystemException, Integer, RuntimeException>>lambda(e -> -1);
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
        final var effectSpy = Spy.<ConsumerChecked<String, FileSystemException>>lambda(v -> throwingOp.get());
        final var successSpy = Spy.<ConsumerChecked<String, FileSystemException>>lambda(v -> throwingOp.get());
        final var errorSpy = Spy.<ConsumerChecked<RuntimeException, FileSystemException>>lambda(err -> { });
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
          final var successSpy = Spy.<ConsumerChecked<String, FileSystemException>>lambda(v -> { });
          final var errorSpy = Spy.<ConsumerChecked<FileSystemException, FileSystemException>>lambda(
            err -> throwingOp.get()
          );
          final var handler = Maybe.fromResolver(throwingOp);
          final var newHandler = handler.runEffect(successSpy, errorSpy);

          assertThat(newHandler.error()).contains(FAIL_EXCEPTION);

          verify(successSpy, never()).accept(any());
          verify(errorSpy, times(1)).accept(FAIL_EXCEPTION);
        }
      }

      @Nested class and_the_error_callback_is_NOT_provided {
        @Test void never_calls_the_effect_callback_and_returns_a_new_empty_handler() throws FileSystemException {
          final var effectSpy = Spy.<ConsumerChecked<String, FileSystemException>>lambda(v -> throwingOp.get());
          final var handler = Maybe.fromResolver(throwingOp);
          final var newHandler = handler.runEffect(effectSpy);

          assertThat(newHandler.error()).isEmpty();

          verify(effectSpy, never()).accept(any());
        }
      }
    }
  }

  @Nested class map {
    @Nested class when_the_value_is_present {
      @Test void returns_a_new_handler_applying_the_mapper_function() {
        final var handler = ResolveHandler.withSuccess("Hello world!")
          .map(String::length);

        assertThat(handler.success()).contains(12);

        assertThat(handler.error()).isEmpty();
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_new_handler_with_the_previous_error() {
        final var handler = ResolveHandler.withError(FAIL_EXCEPTION)
          .map(Object::toString);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).contains(FAIL_EXCEPTION);
      }
    }

    @Nested class when_neither_the_value_nor_the_error_is_present {
      @Test void returns_an_empty_handler() {
        final var handler = ResolveHandler.withNothing()
          .map(Object::toString);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).isEmpty();
      }
    }
  }

  @Nested class filter {
    @Nested class when_the_value_is_present {
      @Nested class and_the_predicate_matches {
        @Test void returns_a_new_handler_with_the_value() {
          final var handler = ResolveHandler.withSuccess("Hello world!")
            .filter(it -> it.contains("world"));

          assertThat(handler.success()).contains("Hello world!");
          assertThat(handler.error()).isEmpty();
        }
      }

      @Nested class and_the_predicate_does_NOT_match {
        @Test void returns_an_empty_handler() {
          final var handler = ResolveHandler.withSuccess("Hello world!")
            .filter(it -> it.contains("planet"));

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error()).isEmpty();
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_new_handler_with_the_previous_error() {
        final var handler = ResolveHandler.withError(FAIL_EXCEPTION)
          .filter(Objects::isNull);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).contains(FAIL_EXCEPTION);
      }
    }

    @Nested class when_neither_the_value_nor_the_error_is_present {
      @Test void returns_an_empty_handler() {
        final var handler = ResolveHandler.withNothing()
          .filter(Objects::isNull);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).isEmpty();
      }
    }
  }

  @Nested class cast {
    @Nested class when_the_value_is_present {
      @Nested class and_the_object_can_be_cast {
        @Test void returns_a_new_handler_with_the_cast_value() {
          final var anyValue = (Object) "Hello";
          final var handler = ResolveHandler.withSuccess(anyValue)
            .cast(String.class);

          assertThat(handler.success()).contains("Hello");
          assertThat(handler.error()).isEmpty();
        }
      }

      @Nested class and_the_object_can_NOT_be_cast {
        @Test void returns_a_new_handler_with_the_cast_exception() {
          final var anyValue = (Object) 3;
          final var handler = ResolveHandler.withSuccess(anyValue)
            .cast(String.class);

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error())
            .map(WrappingException::getCause)
            .containsInstanceOf(ClassCastException.class)
            .isNotEmpty();
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_new_handler_with_a_cast_exception() {
        final var handler = ResolveHandler.withError(FAIL_EXCEPTION)
            .cast(String.class);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error())
          .map(WrappingException::getCause)
          .containsInstanceOf(FileSystemException.class)
          .isNotEmpty();
      }
    }

    @Nested class when_neither_the_value_nor_the_error_is_present {
      @Test void returns_an_empty_handler() {
        final var handler = ResolveHandler.withNothing()
          .cast(String.class);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).isEmpty();
      }
    }
  }

  @Nested class orElse {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() {
        final var handler = Maybe.fromResolver(okOp);

        assertThat(handler.orElse(OTHER)).isEqualTo(OK);
        assertThat(handler.orElse(Exception::getMessage)).isEqualTo(OK);
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
        final var supplierSpy = Spy.<Supplier<String>>lambda(() -> OTHER);
        final var handler = Maybe.fromResolver(okOp);

        assertThat(handler.orElseGet(supplierSpy)).isEqualTo(OK);

        verify(supplierSpy, never()).get();
      }
    }

    @Nested class when_the_error_is_present {
      @Test void evaluates_the_supplier_and_returns_the_produced_value() {
        final var supplierSpy = Spy.<Supplier<String>>lambda(() -> OTHER);
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
        final var functionSpy = Spy.<Function<RuntimeException, FileSystemException>>lambda(error -> FAIL_EXCEPTION);
        final var handler = Maybe.fromResolver(okOp);

        assertThat(handler.orThrow()).isEqualTo(OK);
        assertThat(handler.orThrow(functionSpy)).isEqualTo(OK);

        verify(functionSpy, never()).apply(any());
      }
    }

    @Nested class when_the_error_is_present {
      @Test void throws_the_error() {
        final var anotherError = new RuntimeException(OTHER);
        final var functionSpy = Spy.<Function<FileSystemException, RuntimeException>>lambda(error -> anotherError);
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

    @Nested class when_neither_the_resource_nor_the_error_is_present {
      @Test void returns_an_empty_resource_holder() {
        final var path = "./src/test/resources/readTest.txt";
        final var holder = Maybe.just(path)
          .resolve(FileInputStream::new)
          .map(file -> null)
          .mapToResource(file -> null);

        assertThat(holder.resource()).isEmpty();
        assertThat(holder.error()).isEmpty();
      }
    }
  }
}

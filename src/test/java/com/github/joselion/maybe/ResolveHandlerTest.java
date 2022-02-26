package com.github.joselion.maybe;

import static com.github.joselion.maybe.helpers.Helpers.spyLambda;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.INPUT_STREAM;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.FileInputStream;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.joselion.maybe.exceptions.WrappingException;
import com.github.joselion.maybe.helpers.UnitTest;
import com.github.joselion.maybe.util.SupplierChecked;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
        final Consumer<String> consumerSpy = spyLambda(v -> { });
        final Runnable runnableSpy = spyLambda(() -> { });

        Maybe.fromSupplier(okOp)
          .doOnSuccess(consumerSpy)
          .doOnSuccess(runnableSpy);

        verify(consumerSpy, times(1)).accept(OK);
        verify(runnableSpy, times(1)).run();
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void never_calls_the_effect_callback() {
        final Consumer<String> consumerSpy = spyLambda(v -> { });
        final Runnable runnableSpy = spyLambda(() -> { });

        Maybe.fromSupplier(throwingOp)
          .doOnSuccess(consumerSpy)
          .doOnSuccess(runnableSpy);

        verify(consumerSpy, never()).accept(any());
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
            final Runnable runnableSpy = spyLambda(() -> { });

            Maybe.fromSupplier(throwingOp)
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

            Maybe.fromSupplier(throwingOp)
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

          Maybe.fromSupplier(throwingOp)
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

        Maybe.fromSupplier(okOp)
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
            final Function<FileSystemException, String> functionSpy = spyLambda(e -> OK);
            final Supplier<String> supplierSpy = spyLambda(() -> OK);
            final List<ResolveHandler<String, FileSystemException>> handlers = List.of(
              Maybe.fromSupplier(throwingOp).catchError(FileSystemException.class, functionSpy),
              Maybe.fromSupplier(throwingOp).catchError(FileSystemException.class, supplierSpy)
            );

            assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
              assertThat(handler.success()).contains(OK);
              assertThat(handler.error()).isEmpty();
            });

            verify(functionSpy, times(1)).apply(FAIL_EXCEPTION);
            verify(supplierSpy, times(1)).get();
          }
        }

        @Nested class and_the_error_is_NOT_an_instance_of_the_provided_type {
          @Test void never_calls_the_handler_function() {
            final Function<AccessDeniedException, String> functionSpy = spyLambda(e -> OK);
            final Supplier<String> supplierSpy = spyLambda(() -> OK);
            final List<ResolveHandler<String, FileSystemException>> handlers = List.of(
              Maybe.fromSupplier(throwingOp).catchError(AccessDeniedException.class, functionSpy),
              Maybe.fromSupplier(throwingOp).catchError(AccessDeniedException.class, supplierSpy)
            );

            assertThat(handlers).isNotEmpty().allSatisfy(handler -> {
              assertThat(handler.success()).isEmpty();
              assertThat(handler.error()).contains(FAIL_EXCEPTION);
            });

            verify(functionSpy, never()).apply(any());
            verify(supplierSpy, never()).get();
          }
        }
      }

      @Nested class and_the_error_type_is_NOT_provided {
        @Test void calls_the_handler_function() {
          final Function<FileSystemException, String> handlerSpy = spyLambda(e -> OK);
          final Supplier<String> supplierSpy = spyLambda(() -> OK);
          final List<ResolveHandler<String, FileSystemException>> resolvers = List.of(
            Maybe.fromSupplier(throwingOp).catchError(handlerSpy),
            Maybe.fromSupplier(throwingOp).catchError(supplierSpy)
          );

          assertThat(resolvers).isNotEmpty().allSatisfy(resolver -> {
            assertThat(resolver.success()).contains(OK);
            assertThat(resolver.error()).isEmpty();
          });

          verify(handlerSpy, times(1)).apply(FAIL_EXCEPTION);
          verify(supplierSpy, times(1)).get();
        }
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void never_calls_the_handler_function() {
        final Function<RuntimeException, String> functionSpy = spyLambda(e -> OK);
        final Supplier<String> supplierSpy = spyLambda(() -> OK);
        final List<ResolveHandler<String, RuntimeException>> resolvers = List.of(
          Maybe.fromSupplier(okOp).catchError(RuntimeException.class, functionSpy),
          Maybe.fromSupplier(okOp).catchError(RuntimeException.class, supplierSpy),
          Maybe.fromSupplier(okOp).catchError(functionSpy),
          Maybe.fromSupplier(okOp).catchError(supplierSpy)
        );

        assertThat(resolvers).isNotEmpty().allSatisfy(resolver -> {
          assertThat(resolver.success()).contains(OK);
          assertThat(resolver.error()).isEmpty();
        });

        verify(functionSpy, never()).apply(any());
        verify(supplierSpy, never()).get();
      }
    }
  }

  @Nested class map {
    @Nested class when_the_value_is_present {
      @Test void returns_a_new_handler_applying_the_mapper_function() {
        final ResolveHandler<Integer, ?> handler = ResolveHandler.withSuccess("Hello world!")
          .map(String::length);

        assertThat(handler.success()).contains(12);

        assertThat(handler.error()).isEmpty();
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_new_handler_with_the_previous_error() {
        final ResolveHandler<?, FileSystemException> handler = ResolveHandler.withError(FAIL_EXCEPTION)
          .map(Object::toString);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).contains(FAIL_EXCEPTION);
      }
    }

    @Nested class when_neither_the_value_nor_the_error_is_present {
      @Test void returns_an_empty_handler() {
        final ResolveHandler<?, ?> handler = ResolveHandler.withNothing()
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
          final ResolveHandler<String, ?> handler = ResolveHandler.withSuccess("Hello world!")
            .filter(it -> it.contains("world"));

          assertThat(handler.success()).contains("Hello world!");
          assertThat(handler.error()).isEmpty();
        }
      }

      @Nested class and_the_predicate_does_NOT_match {
        @Test void returns_an_empty_handler() {
          final ResolveHandler<String, ?> handler = ResolveHandler.withSuccess("Hello world!")
            .filter(it -> it.contains("planet"));

          assertThat(handler.success()).isEmpty();
          assertThat(handler.error()).isEmpty();
        }
      }
    }

    @Nested class when_the_error_is_present {
      @Test void returns_a_new_handler_with_the_previous_error() {
        final ResolveHandler<?, FileSystemException> handler = ResolveHandler.withError(FAIL_EXCEPTION)
          .filter(Objects::isNull);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).contains(FAIL_EXCEPTION);
      }
    }

    @Nested class when_neither_the_value_nor_the_error_is_present {
      @Test void returns_an_empty_handler() {
        final ResolveHandler<?, ?> handler = ResolveHandler.withNothing()
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
          final Object anyValue = "Hello";
          final ResolveHandler<String, WrappingException> handler = ResolveHandler.withSuccess(anyValue)
            .cast(String.class);

          assertThat(handler.success()).contains("Hello");
          assertThat(handler.error()).isEmpty();
        }
      }

      @Nested class and_the_object_can_NOT_be_cast {
        @Test void returns_a_new_handler_with_the_cast_exception() {
          final Object anyValue = 3;
          final ResolveHandler<String, WrappingException> handler = ResolveHandler.withSuccess(anyValue)
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
        final ResolveHandler<String, WrappingException> handler = ResolveHandler.withError(FAIL_EXCEPTION)
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
        final ResolveHandler<?, ?> handler = ResolveHandler.withNothing()
          .cast(String.class);

        assertThat(handler.success()).isEmpty();
        assertThat(handler.error()).isEmpty();
      }
    }
  }

  @Nested class orElse {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() {
        final ResolveHandler<String, ?> handler = Maybe.fromSupplier(okOp);

        assertThat(handler.orElse(OTHER)).isEqualTo(OK);
        assertThat(handler.orElse(Exception::getMessage)).isEqualTo(OK);
        assertThat(handler.orElse(() -> OTHER)).isEqualTo(OK);
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_the_default_value() {
        final ResolveHandler<String, FileSystemException> handler = Maybe.fromSupplier(throwingOp);

        assertThat(handler.orElse(OTHER)).isEqualTo(OTHER);
        assertThat(handler.orElse(FileSystemException::getMessage)).isEqualTo(FAIL_EXCEPTION.getMessage());
        assertThat(handler.orElse(() -> OTHER)).isEqualTo(OTHER);
      }
    }
  }

  @Nested class orThrow {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value() throws FileSystemException {
        final Function<RuntimeException, FileSystemException> functionSpy = spyLambda(error -> FAIL_EXCEPTION);
        final ResolveHandler<String, RuntimeException> handler = Maybe.fromSupplier(okOp);

        assertThat(handler.orThrow()).isEqualTo(OK);
        assertThat(handler.orThrow(functionSpy)).isEqualTo(OK);

        verify(functionSpy, never()).apply(any());
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void throws_the_error() {
        final RuntimeException anotherError = new RuntimeException(OTHER);
        final Function<FileSystemException, RuntimeException> functionSpy = spyLambda(error -> anotherError);
        final ResolveHandler<?, FileSystemException> handler = Maybe.fromSupplier(throwingOp);

        assertThatThrownBy(handler::orThrow).isEqualTo(FAIL_EXCEPTION);
        assertThatThrownBy(() -> handler.orThrow(functionSpy)).isEqualTo(anotherError);

        verify(functionSpy, times(1)).apply(FAIL_EXCEPTION);
      }
    }
  }

  @Nested class toMaybe {
    @Nested class when_the_value_is_present {
      @Test void returns_a_maybe_with_the_value() {
        assertThat(Maybe.fromSupplier(okOp).toMaybe().value())
          .contains(OK);
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_a_maybe_with_nothing() {
        assertThat(Maybe.fromSupplier(throwingOp).toMaybe().value())
          .isEmpty();
      }
    }
  }

  @Nested class toOptional {
    @Nested class when_the_value_is_present {
      @Test void returns_the_value_wrapped_in_an_optinal() {
        assertThat(Maybe.fromSupplier(okOp).toOptional()).contains(OK);
      }
    }

    @Nested class when_the_value_is_NOT_present {
      @Test void returns_an_empty_optional() {
        assertThat(Maybe.fromSupplier(throwingOp).toOptional()).isEmpty();
      }
    }
  }

  @Nested class mapToResource {
    @Nested class when_the_value_is_present {
      @Test void returns_a_resource_holder_with_the_mapped_value() {
        final ResourceHolder<FileInputStream> holder = Maybe.just("./src/test/resources/readTest.txt")
          .resolve(FileInputStream::new)
          .mapToResource(Function.identity());

        assertThat(holder.resource())
          .get(as(INPUT_STREAM))
          .hasContent("foo");
      }
    }

    @Nested class when_the_error_is_NOT_present {
      @Test void returns_an_empty_resource_holder() {
        final ResourceHolder<FileInputStream> holder = Maybe.just("invalidFile.txt")
          .resolve(FileInputStream::new)
          .mapToResource(Function.identity());

        assertThat(holder.resource()).isEmpty();
      }
    }
  }
}

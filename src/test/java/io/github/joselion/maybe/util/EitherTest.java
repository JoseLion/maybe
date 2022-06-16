package io.github.joselion.maybe.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.type;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.testing.Spy;
import io.github.joselion.testing.UnitTest;

@UnitTest class EitherTest {

  @Nested class ofLeft {
    @Nested class when_the_value_is_not_null {
      @Test void creates_a_Left_instance_with_the_value() {
        final var either = Either.ofLeft("foo");

        assertThat(either)
          .asInstanceOf(type(Either.Left.class))
          .extracting(Either.Left::value)
          .isEqualTo("foo");
      }
    }

    @Nested class when_the_value_is_null {
      @Test void throws_a_null_pointer_exception() {
        assertThatThrownBy(() -> Either.ofLeft(null))
          .isExactlyInstanceOf(NullPointerException.class)
          .hasMessage("An Either cannot be created with a null value");
      }
    }
  }

  @Nested class ofRight {
    @Nested class when_the_value_is_not_null {
      @Test void creates_a_Right_instance_with_the_value() {
        final var either = Either.ofRight("foo");

        assertThat(either)
          .asInstanceOf(type(Either.Right.class))
          .extracting(Either.Right::value)
          .isEqualTo("foo");
      }
    }

    @Nested class when_the_value_is_null {
      @Test void throws_a_null_pointer_exception() {
        assertThatThrownBy(() -> Either.ofRight(null))
          .isExactlyInstanceOf(NullPointerException.class)
          .hasMessage("An Either cannot be created with a null value");
      }
    }
  }

  @Nested class isLeft {
    @Nested class when_the_left_value_is_present {
      @Test void returns_true() {
        final var either = Either.ofLeft("foo");

        assertThat(either.isLeft()).isTrue();
      }
    }

    @Nested class when_the_right_value_is_present {
      @Test void returns_false() {
        final var either = Either.ofRight("foo");

        assertThat(either.isLeft()).isFalse();
      }
    }
  }

  @Nested class isRight {
    @Nested class when_the_left_value_is_present {
      @Test void returns_false() {
        final var either = Either.ofLeft("foo");

        assertThat(either.isRight()).isFalse();
      }
    }

    @Nested class when_the_right_value_is_present {
      @Test void returns_true() {
        final var either = Either.ofRight("foo");

        assertThat(either.isRight()).isTrue();
      }
    }
  }

  @Nested class doOnLeft {
    @Nested class when_the_left_value_is_present {
      @Test void runs_the_effect() {
        final var effectSpy = Spy.<Consumer<String>>lambda(value -> { });

        Either.ofLeft("foo").doOnLeft(effectSpy);

        verify(effectSpy).accept("foo");
      }
    }

    @Nested class when_the_right_value_is_present {
      @Test void does_not_run_the_effect() {
        final var effectSpy = Spy.<Consumer<Object>>lambda(value -> { });

        Either.ofRight("foo").doOnLeft(effectSpy);

        verify(effectSpy, never()).accept(any());
      }
    }
  }

  @Nested class doOnRight {
    @Nested class when_the_left_value_is_present {
      @Test void does_not_run_the_effect() {
        final var effectSpy = Spy.<Consumer<Object>>lambda(value -> { });

        Either.ofLeft("foo").doOnRight(effectSpy);

        verify(effectSpy, never()).accept(any());
      }
    }

    @Nested class when_the_right_value_is_present {
      @Test void runs_the_effect() {
        final var effectSpy = Spy.<Consumer<String>>lambda(value -> { });

        Either.ofRight("foo").doOnRight(effectSpy);

        verify(effectSpy).accept("foo");
      }
    }
  }

  @Nested class mapLeft {
    @Nested class when_the_left_value_is_present {
      @Test void applies_the_mapper_to_the_value() {
        final var mapperSpy = Spy.<Function<String, Integer>>lambda(Integer::parseInt);
        final var either = Either.ofLeft("1").mapLeft(mapperSpy);

        assertThat(either)
          .asInstanceOf(type(Either.Left.class))
          .extracting(Either.Left::value)
          .isExactlyInstanceOf(Integer.class)
          .isEqualTo(1);

        verify(mapperSpy).apply("1");
      }
    }

    @Nested class when_the_right_value_is_present {
      @Test void does_not_apply_the_mapper() {
        final var mapperSpy = Spy.<Function<Object, String>>lambda(Object::toString);
        final var either = Either.ofRight(1).mapLeft(mapperSpy);

        assertThat(either)
        .asInstanceOf(type(Either.Right.class))
        .extracting(Either.Right::value)
          .isExactlyInstanceOf(Integer.class)
          .isEqualTo(1);

        verify(mapperSpy, never()).apply(any());
      }
    }
  }

  @Nested class mapRight {
    @Nested class when_the_left_value_is_present {
      @Test void does_not_apply_the_mapper() {
        final var mapperSpy = Spy.<Function<Object, String>>lambda(Object::toString);
        final var either = Either.ofLeft(1).mapRight(mapperSpy);

        assertThat(either)
          .asInstanceOf(type(Either.Left.class))
          .extracting(Either.Left::value)
          .isExactlyInstanceOf(Integer.class)
          .isEqualTo(1);

        verify(mapperSpy, never()).apply(any());
      }
    }

    @Nested class when_the_right_value_is_present {
      @Test void applies_the_mapper_to_the_value() {
        final var mapperSpy = Spy.<Function<String, Integer>>lambda(Integer::parseInt);
        final var either = Either.ofRight("1").mapRight(mapperSpy);

        assertThat(either)
          .asInstanceOf(type(Either.Right.class))
          .extracting(Either.Right::value)
          .isExactlyInstanceOf(Integer.class)
          .isEqualTo(1);

        verify(mapperSpy).apply("1");
      }
    }
  }

  @Nested class leftOrElse {
    @Nested class when_the_left_value_is_present {
      @Test void returns_the_left_value() {
        final var either = Either.ofLeft("foo");

        assertThat(either.leftOrElse("bar")).isEqualTo("foo");
      }
    }

    @Nested class when_the_right_value_is_present {
      @Test void returns_the_fallback_value() {
        final var either = Either.ofRight("foo");

        assertThat(either.leftOrElse("bar")).isEqualTo("bar");
      }
    }
  }

  @Nested class rightOrElse {
    @Nested class when_the_left_value_is_present {
      @Test void returns_the_fallback_value() {
        final var either = Either.ofLeft("foo");

        assertThat(either.rightOrElse("bar")).isEqualTo("bar");
      }
    }

    @Nested class when_the_right_value_is_present {
      @Test void returns_the_right_value() {
        final var either = Either.ofRight("foo");

        assertThat(either.rightOrElse("bar")).isEqualTo("foo");
      }
    }
  }

  @Nested class unwrap {
    @Nested class when_the_left_value_is_present {
      @Test void returns_the_value_using_the_onLeft_handler() {
        final var onLeftSpy = Spy.<Function<String, String>>lambda("The value is: %s"::formatted);
        final var onRightSpy = Spy.<Function<Object, String>>lambda("The value is: %s"::formatted);
        final var value = Either.ofLeft("foo").unwrap(onLeftSpy, onRightSpy);

        assertThat(value).isEqualTo("The value is: foo");

        verify(onLeftSpy).apply("foo");
        verify(onRightSpy, never()).apply(any());
      }
    }

    @Nested class when_the_right_value_is_present {
      @Test void returns_the_value_using_the_onRight_handler() {
        final var onLeftSpy = Spy.<Function<Object, String>>lambda("The value is: %s"::formatted);
        final var onRightSpy = Spy.<Function<String, String>>lambda("The value is: %s"::formatted);
        final var value = Either.ofRight("foo").unwrap(onLeftSpy, onRightSpy);

        assertThat(value).isEqualTo("The value is: foo");

        verify(onLeftSpy, never()).apply(any());
        verify(onRightSpy).apply("foo");
      }
    }
  }

  @Nested class equals {
    @Nested class when_the_tested_object_is_the_same_as_the_value {
      @Test void returns_true() {
        final var left = Either.ofLeft("foo");
        final var right = Either.ofRight("bar");
        final var isLeftEqual = left.equals(left);
        final var isRightEqual = right.equals(right);

        assertThat(isLeftEqual).isTrue();
        assertThat(isRightEqual).isTrue();
      }
    }

    @Nested class when_the_tested_object_is_not_the_same_as_the_value {
      @Test void returns_false() {
        final var left = Either.ofLeft("foo");
        final var right = Either.ofRight("foo");
        final var isLeftEqual = left.equals(right);
        final var isRightEqual = right.equals(left);

        assertThat(isLeftEqual).isFalse();
        assertThat(isRightEqual).isFalse();
      }
    }

    @Nested class when_both_objects_are_of_the_same_instance {
      @Nested class and_the_wrapped_values_are_equal {
        @Test void returns_true() {
          final var leftA = Either.ofLeft("foo");
          final var leftB = Either.ofLeft("foo");
          final var rightA = Either.ofRight("bar");
          final var rightB = Either.ofRight("bar");
          final var isLeftEqual = leftA.equals(leftB);
          final var isRightEqual = rightA.equals(rightB);

          assertThat(isLeftEqual).isTrue();
          assertThat(isRightEqual).isTrue();
        }
      }

      @Nested class and_the_wrapped_values_are_not_equal {
        @Test void returns_false() {
          final var leftA = Either.ofLeft("foo");
          final var leftB = Either.ofLeft("bar");
          final var rightA = Either.ofRight("foo");
          final var rightB = Either.ofRight("bar");
          final var isLeftEqual = leftA.equals(leftB);
          final var isRightEqual = rightA.equals(rightB);

          assertThat(isLeftEqual).isFalse();
          assertThat(isRightEqual).isFalse();
        }
      }
    }

    @Nested class when_both_objects_are_not_of_the_same_instance {
      @Test void returns_false() {
        final var left = Either.ofLeft("foo");
        final var right = Either.ofRight("bar");
        final var foo = (Object) "foo";
        final var bar = (Object) "bar";
        final var isLeftEqual = left.equals(foo);
        final var isRightEqual = right.equals(bar);

        assertThat(isLeftEqual).isFalse();
        assertThat(isRightEqual).isFalse();
      }
    }
  }

  @Nested class hashCode {
    @Test void returns_the_hash_code_of_the_value() {
      final var foo = "foo";
      final var bar = "bar";
      final var left = Either.ofLeft(foo);
      final var right = Either.ofRight(bar);

      assertThat(left).hasSameHashCodeAs(foo);
      assertThat(right).hasSameHashCodeAs(bar);
    }
  }

  @Nested class toString {
    @Test void returns_the_string_representation_of_the_instance() {
      final var left = Either.ofLeft("foo");
      final var right = Either.ofRight("bar");

      assertThat(left).hasToString("Either[Left: foo]");
      assertThat(right).hasToString("Either[Right: bar]");
    }
  }
}

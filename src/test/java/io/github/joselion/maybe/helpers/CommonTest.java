package io.github.joselion.maybe.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.joselion.testing.UnitTest;

@UnitTest class CommonTest {

  @Nested class helper {
    @Nested class when_the_class_is_instantiated {
      @Test void throws_an_UnsupportedOperationException() {
        assertThatCode(Common::new)
          .isInstanceOf(UnsupportedOperationException.class)
          .hasMessage("Cannot instantiate a helper class");
      }
    }
  }

  @Nested class cast {
    @Nested class when_the_value_can_be_cast {
      @Test void returns_the_value_as_the_parameter_type() {
        final Number value = 3;

        assertThat(Common.<Integer>cast(value)).isInstanceOf(Integer.class);
      }
    }

    @Nested class when_the_value_cannot_be_cast {
      @Test void throws_a_ClassCastException() {
        assertThatCode(() -> Common.<Integer>cast("3").intValue()) // NOSONAR
          .isInstanceOf(ClassCastException.class);
      }
    }
  }
}

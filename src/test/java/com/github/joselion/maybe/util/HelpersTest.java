package com.github.joselion.maybe.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.joselion.maybe.helpers.UnitTest;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class HelpersTest {

  @Nested class shallowInstance {
    @Nested class when_a_shallow_instance_is_possible {
      @Test void it_returns_a_new_instance_of_the_class() {
        assertThat(Helpers.shallowInstance(Void.class))
          .isExactlyInstanceOf(Void.class)
          .isNotNull();
      }
    }

    @Nested class when_a_shallow_instance_is_NOT_possible {
      @Test void throws_an_unsupported_operation_exception() {
        assertThrows(
          UnsupportedOperationException.class,
          () -> Helpers.shallowInstance(NotShallow.class),
          "Unable to instanciate shallow instance of NotShallow"
        );
      }
    }
  }

  static class NotShallow {

    final boolean x;

    NotShallow(final boolean x) {
      this.x = x;
    }
  }
}

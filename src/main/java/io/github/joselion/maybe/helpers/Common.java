package io.github.joselion.maybe.helpers;

public class Common {

  Common() {
    throw new UnsupportedOperationException("Cannot instantiate a helper class");
  }

  @SuppressWarnings("unchecked")
  public static <T> T cast(final Object value) {
    return (T) value;
  }
}

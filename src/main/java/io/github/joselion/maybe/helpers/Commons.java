package io.github.joselion.maybe.helpers;

/**
 * Common helpers.
 */
public final class Commons {

  protected Commons() {
    throw new UnsupportedOperationException("Cannot instantiate a helper class");
  }

  /**
   * Casts a value to the generic type {@code T} of the method.
   *
   * @param <T> the type to cast the value
   * @param value the value to cast
   * @return the {@code value} cast to {@code T} type
   */
  @SuppressWarnings("unchecked")
  public static <T> T cast(final Object value) {
    return (T) value;
  }
}

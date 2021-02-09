package com.github.joselion.maybe.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Internal use helpe functions
 */
public interface Helpers {

  /**
   * Creates a shallow instance of the {@code T} class. Usefull to represent
   * that nothing is expected to have in a lambda argument when this argument
   * is of type {@link Void}.
   * 
   * @param <T> the type of the shallow instance to create
   * @param type the {@link Class} object that represents the {@code T} type
   * @return a shallow instance of type {@code T}
   */
  static <T> T shallowInstance(final Class<T> type) {
    try {
      final Constructor<T> constructor = type.getDeclaredConstructor();
      constructor.setAccessible(true); // NOSONAR

      return constructor.newInstance();
    } catch (
      NoSuchMethodException
      | SecurityException
      | InstantiationException
      | IllegalAccessException
      | IllegalArgumentException
      | InvocationTargetException error
    ) {
      throw new UnsupportedOperationException(
        "Unable to instanciate shallow instance of " + type.getSimpleName(),
        error
      );
    }
  }
}

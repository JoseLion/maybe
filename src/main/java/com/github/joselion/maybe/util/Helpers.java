package com.github.joselion.maybe.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public interface Helpers {

  static <T> T shallowInstance(final Class<T> type) {
    try {
      final Constructor<T> constructor = type.getDeclaredConstructor();
      constructor.setAccessible(true);

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

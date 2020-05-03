package com.github.joselion.maybe.exceptions;

import com.github.joselion.maybe.Maybe;

/**
 * {@code MaybeFailedException} extends from {@link RuntimeException}, making
 * it an unchecked exception. When thrown it ussually means that the
 * {@link Maybe} operation failed and the value was unsafely unboxed from the
 * monad.
 * 
 * @author Jose Luis Leon
 * @since v0.1.0
 */
public class MaybeFailedException extends RuntimeException {

  private static final long serialVersionUID = -8376569137323694868L;

  /**
   * Constructs a new {@code MaybeFailedException} with a generic message
   */
  public MaybeFailedException() {
    super("Cannot unbox the success value. The operation failed to resolve or execute");
  }
}
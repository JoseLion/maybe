package com.github.joselion.maybe.exceptions;

import com.github.joselion.maybe.Maybe;

/**
 * {@code MaybeFailureException} extends from {@link RuntimeException}, making
 * it an unchecked exception. When thrown it ussually means that the
 * {@link Maybe} operation failed and the value was unsafely unboxed from the
 * monad, leaving only the checked exception.
 * 
 * @author Jose Luis Leon
 * @since v0.1.0
 */
public class MaybeFailureException extends RuntimeException {

  private static final long serialVersionUID = -8376569137323694868L;

  /**
   * Constructs a new {@code MaybeFailureException} with the actual cause from the
   * failed {@link Maybe} monad.
   * 
   * @param cause the exception in the {@code Maybe} monad
   */
  public MaybeFailureException(Throwable cause) {
    super("The Maybe oparation failed with an excpetion!", cause);
  }
}
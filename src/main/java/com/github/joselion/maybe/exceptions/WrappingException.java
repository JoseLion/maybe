package com.github.joselion.maybe.exceptions;

/**
 * A {@code WrappingException} is sort of a generic exception that simply wraps
 * another. This class is used when something needs to define that it may throw
 * more than one type of exception. The wrapped exception can be retrieved from
 * either the {@link #wrapped()} or the {@link #getCause()} method.
 * 
 * The goal of this class is for users to know that the exception has changed
 * types, and now it's wrapped to preserve it's information.
 * 
 * @author Jose Luis Leon
 * @since v1.6.0
 */
public class WrappingException extends Exception {

  /**
   * The exception this class is wrapping
   */
  private final Throwable wrapped;

  private WrappingException(final Throwable wrapped) {
    super(wrapped);
    this.wrapped = wrapped;
  }

  /**
   * Creates a new {@link WrappingException} instance using the provided
   * exception.
   * 
   * @param wrapped the exception to wrap in the new instance
   * @return a new {@link WrappingException} instance with the passed exception
   */
  public static WrappingException of(final Throwable wrapped) {
    return new WrappingException(wrapped);
  }

  /**
   * Returns the wrapped exception of this instance. The same result can be
   * obtained by using the inherited method {@link #getCause()}.
   * 
   * @return the wrapped expection of this instance
   */
  public final Throwable wrapped() {
    return this.wrapped;
  }
}

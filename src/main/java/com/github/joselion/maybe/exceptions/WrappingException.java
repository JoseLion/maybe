package com.github.joselion.maybe.exceptions;

/**
 * A {@code WrappingException} is sort of a generic exception that simply wraps
 * another. This class is used when something needs to define that it may throw
 * more than one type of exception. The wrapped exception can be retrieved from
 * either the {@link #wrapped()} or the {@link #getCause()} method.
 * 
 * @author Jose Luis Leon
 * @since v1.6.0
 */
public class WrappingException extends Exception {

  private final Throwable wrapped;

  private WrappingException(final Throwable wrapped) {
    this.wrapped = wrapped;
  }

  public static WrappingException of(final Throwable wrapped) {
    return new WrappingException(wrapped);
  }

  public final Throwable wrapped() {
    return this.wrapped;
  }

  @Override
  public synchronized Throwable getCause() {
    return this.wrapped;
  }
}

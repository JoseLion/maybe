package io.github.joselion.maybe.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

/**
 * Either is a monadic wrapper that contains one of two possible values which
 * are represented as {@code Left} or {@code Right}. the values can be of
 * different types, and the API allows to safely transform an unwrap the value.
 * 
 * The sealed interface implementation ensures only one of the two can be
 * present at the same time.
 * 
 * @param <L> the {@code Left} data type
 * @param <R> the {@code Right} data type
 * 
 * @author Jose Luis Leon
 * @since v3.0.0
 */
public sealed interface Either<L, R> {

  /**
   * Factory method to create an {@code Either} instance that contains a
   * {@code Left} value.
   *
   * @param <L> the type of the left value
   * @param <R> the type of the right value
   * @param value the value to use as left in the {@code Either} instance
   * @return an {@code Either} instance with a left value
   */
  static <L, R> Either<L, R> ofLeft(final L value) {
    return new Left<>(value);
  }

  /**
   * Factory method to create an {@code Either} instance that contains a
   * {@code Right} value.
   *
   * @param <L> the type of the left value
   * @param <R> the type of the right value
   * @param value the value to use as right in the {@code Either} instance
   * @return an {@code Either} instance with a right value
   */
  static <L, R> Either<L, R> ofRight(final R value) {
    return new Right<>(value);
  }

  /**
   * Returns true if the {@code Left} value is present, false otherwise.
   *
   * @return true if left is present, false otherwise
   */
  boolean isLeft();

  /**
   * Returns true if the {@code Right} value is present, false otherwise.
   *
   * @return true if right is present, false otherwise
   */
  boolean isRight();

  /**
   * Run an effect if the {@code Left} value is present. Does nothing otherwise.
   *
   * @param effect a consumer function that receives the left value
   * @return the same {@code Either} instance
   */
  Either<L, R> doOnLeft(Consumer<L> effect);

  /**
   * Run an effect if the {@code Right} value is present. Does nothing otherwise.
   *
   * @param effect effect a consumer function that receives the right value
   * @return the same {@code Either} instance
   */
  Either<L, R> doOnRight(Consumer<R> effect);

  /**
   * Map the {@code Left} value to another if present. Does nothing otherwise.
   *
   * @param <T> the type the left value will be mapped to
   * @param mapper a function that receives the left value and returns another
   * @return an {@code Either} instance with the mapped left value
   */
  <T> Either<T, R> mapLeft(Function<L, T> mapper);

  /**
   * Map the {@code Right} value to another if present. Does nothing otherwise.
   *
   * @param <T> the type the right value will be mapped to
   * @param mapper a function that receives the right value and returns another
   * @return an {@code Either} instance with the mapped right value
   */
  <T> Either<L, T> mapRight(Function<R, T> mapper);

  /**
   * Terminal operator. Returns the {@code Left} value if present. Otherwise,
   * it returns the provided fallback value.
   *
   * @param fallback the value to return if left is not present
   * @return the left value or a fallback
   */
  L leftOrElse(L fallback);

  /**
   * Terminal operator. Returns the {@code Right} value if present. Otherwise,
   * it returns the provided fallback value.
   *
   * @param fallback the value to return if right is not present
   * @return the right value or a fallback
   */
  R rightOrElse(R fallback);

  /**
   * Terminal operator. Unwraps the {@code Either} to obtain the wrapped value.
   * Since there's no possible way for the compiler to know which one is
   * present ({@code Left} or {@code Right}), you need to provide a handler for
   * both cases. Only the handler with the value present is used to unwrap and
   * return the value.
   *
   * @param <T> the type of the returned value
   * @param onLeft a function to handle the left value if present
   * @param onRight a function to handle the right value if present
   * @return either the left or the right handled value
   */
  <T> T unwrap(Function<L, T> onLeft, Function<R, T> onRight);

  /**
   * Terminal operator. Returns the {@code Left} value if present. Otherwise,
   * it returns {@code null}.
   *
   * @return the left value or null
   */
  default @Nullable L leftOrNull() {
    return unwrap(Function.identity(), rigth -> null);
  }

  /**
   * Terminal operator. Returns the {@code Right} value if present. Otherwise,
   * it returns {@code null}.
   *
   * @return the right value or null
   */
  default @Nullable R rightOrNull() {
    return unwrap(left -> null, Function.identity());
  }

  /**
   * Terminal operator. Transforms the {@code Left} value to an {@link Optional},
   * which contains the value if present or is {@link Optional#empty()} otherwise.
   *
   * @return an {@code Optional<L>} instance
   */
  default Optional<L> leftToOptional() {
    return Optional.ofNullable(leftOrNull());
  }

  /**
   * Terminal operator. Transforms the {@code Right} value to an {@link Optional},
   * which contains the value if present or is {@link Optional#empty()} otherwise.
   *
   * @return an {@code Optional<R>} instance
   */
  default Optional<R> rightToOptional() {
    return Optional.ofNullable(rightOrNull());
  }

  /**
   * The {@code Left} implementation of {@link Either}
   */
  record Left<L, R>(L value) implements Either<L, R> {

    /**
     * Compact constructor to validate the value is not null.
     *
     * @param value the value of the instance
     */
    public Left {
      Objects.requireNonNull(value, "An Either cannot be created with a null value");
    }

    @Override
    public boolean isLeft() {
      return true;
    }

    @Override
    public boolean isRight() {
      return false;
    }

    @Override
    public Either<L, R> doOnLeft(final Consumer<L> effect) {
      effect.accept(this.value);

      return this;
    }

    @Override
    public Either<L, R> doOnRight(final Consumer<R> effect) {
      return this;
    }

    @Override
    public <T> Either<T, R> mapLeft(final Function<L, T> mapper) {
      final var mappedLeft = mapper.apply(this.value);

      return new Left<>(mappedLeft);
    }

    @Override
    public <T> Either<L, T> mapRight(final Function<R, T> mapper) {
      return new Left<>(this.value);
    }

    @Override
    public L leftOrElse(final L fallback) {
      return this.value;
    }

    @Override
    public R rightOrElse(final R fallback) {
      return fallback;
    }

    @Override
    public <T> T unwrap(final Function<L, T> onLeft, final Function<R, T> onRight) {
      return onLeft.apply(this.value);
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }

      if (obj instanceof final Left<?, ?> left) {
        return this.value.equals(left.leftOrNull());
      }

      return false;
    }

    @Override
    public int hashCode() {
      return this.value.hashCode();
    }

    @Override
    public String toString() {
      return "Either[Left: %s]".formatted(this.value);
    }
  }

  /**
   * The {@code Right} implementation of {@link Either}
   */
  record Right<L, R>(R value) implements Either<L, R> {

    /**
     * Compact constructor to validate the value is not null.
     *
     * @param value the value of the instance
     */
    public Right {
      Objects.requireNonNull(value, "An Either cannot be created with a null value");
    }

    @Override
    public boolean isLeft() {
      return false;
    }

    @Override
    public boolean isRight() {
      return true;
    }

    @Override
    public Either<L, R> doOnLeft(final Consumer<L> effect) {
      return this;
    }

    @Override
    public Either<L, R> doOnRight(final Consumer<R> effect) {
      effect.accept(this.value);

      return this;
    }

    @Override
    public <T> Either<T, R> mapLeft(final Function<L, T> mapper) {
      return new Right<>(this.value);
    }

    @Override
    public <T> Either<L, T> mapRight(final Function<R, T> mapper) {
      final var mappedRight = mapper.apply(this.value);

      return new Right<>(mappedRight);
    }

    @Override
    public L leftOrElse(final L fallback) {
      return fallback;
    }

    @Override
    public R rightOrElse(final R fallback) {
      return this.value;
    }

    @Override
    public <T> T unwrap(final Function<L, T> onLeft, final Function<R, T> onRight) {
      return onRight.apply(this.value);
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
        return true;
      }

      if (obj instanceof final Right<?, ?> right) {
        return this.value.equals(right.rightOrNull());
      }

      return false;
    }

    @Override
    public int hashCode() {
      return this.value.hashCode();
    }

    @Override
    public String toString() {
      return "Either[Right: %s]".formatted(this.value);
    }
  }
}

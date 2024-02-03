package io.github.joselion.maybe.util;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.jdt.annotation.Nullable;

import io.github.joselion.maybe.helpers.Commons;

/**
 * Either is a monadic wrapper that contains one of two possible values which
 * are represented as {@code Left} or {@code Right}. the values can be of
 * different types, and the API allows to safely transform an unwrap the value.
 *
 * <p>The sealed interface implementation ensures only one of the two can be
 * present at the same time.
 *
 * @param <L> the {@code Left} data type
 * @param <R> the {@code Right} data type
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
  <T> T unwrap(Function<? super L, ? extends T> onLeft, Function<? super R, ? extends T> onRight);

  /**
   * Returns true if the {@code Left} value is present, false otherwise.
   *
   * @return true if left is present, false otherwise
   */
  default boolean isLeft() {
    return this.unwrap(left -> true, right -> false);
  }

  /**
   * Returns true if the {@code Right} value is present, false otherwise.
   *
   * @return true if right is present, false otherwise
   */
  default boolean isRight() {
    return this.unwrap(left -> false, rigth -> true);
  }

  /**
   * Run an effect if the {@code Left} value is present. Does nothing otherwise.
   *
   * @param effect a consumer function that receives the left value
   * @return the same {@code Either} instance
   */
  default Either<L, R> doOnLeft(final Consumer<? super L> effect) {
    return this.unwrap(
      left -> {
        effect.accept(left);
        return Either.ofLeft(left);
      },
      Either::ofRight
    );
  }

  /**
   * Run an effect if the {@code Right} value is present. Does nothing otherwise.
   *
   * @param effect effect a consumer function that receives the right value
   * @return the same {@code Either} instance
   */
  default Either<L, R> doOnRight(final Consumer<? super R> effect) {
    return this.unwrap(
      Either::ofLeft,
      right -> {
        effect.accept(right);
        return Either.ofRight(right);
      }
    );
  }

  /**
   * Map the {@code Left} value to another if present. Does nothing otherwise.
   *
   * @param <T> the type the left value will be mapped to
   * @param mapper a function that receives the left value and returns another
   * @return an {@code Either} instance with the mapped left value
   */
  default <T> Either<T, R> mapLeft(final Function<? super L, ? extends T> mapper) {
    return this.unwrap(
      left -> Either.ofLeft(mapper.apply(left)),
      Either::ofRight
    );
  }

  /**
   * Map the {@code Right} value to another if present. Does nothing otherwise.
   *
   * @param <T> the type the right value will be mapped to
   * @param mapper a function that receives the right value and returns another
   * @return an {@code Either} instance with the mapped right value
   */
  default <T> Either<L, T> mapRight(final Function<? super R, ? extends T> mapper) {
    return this.unwrap(
      Either::ofLeft,
      right -> Either.ofRight(mapper.apply(right))
    );
  }

  /**
   * Shortcut method which does a {@link #mapLeft(Function)} and a
   * {@link #mapRight(Function)} in a single operation. The first argument
   * maps the left value if present. Otherwise, the second argument maps the
   * right value.
   *
   * @param <T> the type the left value will be mapped to
   * @param <S> the type the right value will be mapped to
   * @param leftMapper a function that receives the left value and returns another
   * @param rigthMapper a function that receives the right value and returns another
   * @return an {@code Either} instance with the mapped left or right value
   */
  default <T, S> Either<T, S> map(
    final Function<? super L, ? extends T> leftMapper,
    final Function<? super R, ? extends S> rigthMapper
  ) {
    return this.unwrap(
      left -> Either.ofLeft(leftMapper.apply(left)),
      right -> Either.ofRight(rigthMapper.apply(right))
    );
  }

  /**
   * Map the {@code Left} value to another if present. Does nothing otherwise.
   *
   * <p>This method is similar to {@link #mapLeft(Function)}, but the mapping
   * function can return another {@code Either} without wrapping the left value
   * within an additional {@code Either}.
   *
   * @param <T> the type the left value will be mapped to
   * @param mapper a function that receives the left value and returns an {@code Either}
   * @return an {@code Either} instance with the mapped left value
   */
  default <T> Either<T, R> flatMapLeft(final Function<? super L, ? extends Either<? extends T, ? extends R>> mapper) {
    return this
      .mapLeft(mapper)
      .unwrap(Commons::cast, Either::ofRight);
  }

  /**
   * Map the {@code Right} value to another if present. Does nothing otherwise.
   *
   * <p>This method is similar to {@link #mapRight(Function)}, but the mapping
   * function can return another {@code Either} without wrapping the right
   * value within an additional {@code Either}.
   *
   * @param <T> the type the right value will be mapped to
   * @param mapper a function that receives the right value and returns an {@code Either}
   * @return an {@code Either} instance with the mapped right value
   */
  default <T> Either<L, T> flatMapRight(final Function<? super R, ? extends Either<? extends L, ? extends T>> mapper) {
    return this
      .mapRight(mapper)
      .unwrap(Either::ofLeft, Commons::cast);
  }

  /**
   * Shortcut method which does a {@link #flatMapLeft(Function)} and a
   * {@link #flatMapRight(Function)} in a single operation. The first argument
   * maps the left value if present. Otherwise, the second argument maps the
   * right value. In both cases, the mapped left/right values are never wrapped
   * within an additional {@code Either}.
   *
   * @param <T> the type the left value will be mapped to
   * @param <S> the type the right value will be mapped to
   * @param leftMapper a function that receives the left value and returns an {@code Either}
   * @param rigthMapper a function that receives the right value and returns an {@code Either}
   * @return an {@code Either} instance with the mapped left or right value
   */
  default <T, S> Either<T, S> flatMap(
    final Function<? super L, ? extends Either<? extends T, ? extends S>> leftMapper,
    final Function<? super R, ? extends Either<? extends T, ? extends S>> rigthMapper
  ) {
    return this
      .mapLeft(leftMapper)
      .mapRight(rigthMapper)
      .unwrap(Commons::cast, Commons::cast);
  }

  /**
   * Terminal operator. Returns the {@code Left} value if present. Otherwise,
   * it returns the provided fallback value.
   *
   * @param fallback the value to return if left is not present
   * @return the left value or a fallback
   */
  default L leftOrElse(final L fallback) {
    return this.unwrap(Function.identity(), rigth -> fallback);
  }

  /**
   * Terminal operator. Returns the {@code Right} value if present. Otherwise,
   * it returns the provided fallback value.
   *
   * @param fallback the value to return if right is not present
   * @return the right value or a fallback
   */
  default R rightOrElse(final R fallback) {
    return this.unwrap(left -> fallback, Function.identity());
  }

  /**
   * Terminal operator. Returns the {@code Left} value if present. Otherwise,
   * it returns {@code null}.
   *
   * @return the left value or null
   */
  default @Nullable L leftOrNull() {
    return this.unwrap(Function.identity(), rigth -> null);
  }

  /**
   * Terminal operator. Returns the {@code Right} value if present. Otherwise,
   * it returns {@code null}.
   *
   * @return the right value or null
   */
  default @Nullable R rightOrNull() {
    return this.unwrap(left -> null, Function.identity());
  }

  /**
   * Terminal operator. Transforms the {@code Left} value to an {@link Optional},
   * which contains the value if present or is {@link Optional#empty()} otherwise.
   *
   * @return an {@code Optional<L>} instance
   */
  default Optional<L> leftToOptional() {
    return Optional.ofNullable(this.leftOrNull());
  }

  /**
   * Terminal operator. Transforms the {@code Right} value to an {@link Optional},
   * which contains the value if present or is {@link Optional#empty()} otherwise.
   *
   * @return an {@code Optional<R>} instance
   */
  default Optional<R> rightToOptional() {
    return Optional.ofNullable(this.rightOrNull());
  }

  /**
   * The {@code Left} implementation of {@link Either}.
   *
   * @param <L> the {@code Left} data type
   * @param <R> the {@code Right} data type
   * @param value the left value
   */
  record Left<L, R>(L value) implements Either<L, R> {

    /**
     * Compact constructor to validate the value is not null.
     */
    public Left {
      Objects.requireNonNull(value, "An Either cannot be created with a null value");
    }

    @Override
    public <T> T unwrap(final Function<? super L, ? extends T> onLeft, final Function<? super R, ? extends T> onRight) {
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
   * The {@code Right} implementation of {@link Either}.
   *
   * @param <L> the {@code Left} data type
   * @param <R> the {@code Right} data type
   * @param value the right value
   */
  record Right<L, R>(R value) implements Either<L, R> {

    /**
     * Compact constructor to validate the value is not null.
     */
    public Right {
      Objects.requireNonNull(value, "An Either cannot be created with a null value");
    }

    @Override
    public <T> T unwrap(final Function<? super L, ? extends T> onLeft, final Function<? super R, ? extends T> onRight) {
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

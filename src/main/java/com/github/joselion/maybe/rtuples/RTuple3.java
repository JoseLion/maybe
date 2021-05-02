package com.github.joselion.maybe.rtuples;

import java.util.stream.Stream;

public class RTuple3<
  T1 extends AutoCloseable,
  T2 extends AutoCloseable,
  T3 extends AutoCloseable
> extends RTuple2<T1, T2> {

  private final T3 t3;

  public RTuple3(final T1 t1, final T2 t2, final T3 t3) {
    super(t1, t2);
    this.t3 = t3;
  }

  public static <
    T1 extends AutoCloseable,
    T2 extends AutoCloseable,
    T3 extends AutoCloseable
  > RTuple3<T1, T2, T3> of(final T1 t1, final T2 t2, final T3 t3) {
    return new RTuple3<>(t1, t2, t3);
  }

  public T3 getT3() {
    return this.t3;
  }

  @Override
  public Stream<AutoCloseable> toStream() {
    return Stream.concat(
      super.toStream(),
      Stream.of(this.t3)
    );
  }
}

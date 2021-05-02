package com.github.joselion.maybe.rtuples;

import java.util.stream.Stream;

public class RTuple4<
  T1 extends AutoCloseable,
  T2 extends AutoCloseable,
  T3 extends AutoCloseable,
  T4 extends AutoCloseable
> extends RTuple3<T1, T2, T3> {

  private final T4 t4;

  public RTuple4(final T1 t1, final T2 t2, final T3 t3, final T4 t4) {
    super(t1, t2, t3);
    this.t4 = t4;
  }

  public static <
    T1 extends AutoCloseable,
    T2 extends AutoCloseable,
    T3 extends AutoCloseable,
    T4 extends AutoCloseable
  > RTuple4<T1, T2, T3, T4> of(final T1 t1, final T2 t2, final T3 t3, final T4 t4) {
    return new RTuple4<>(t1, t2, t3, t4);
  }

  public T4 getT4() {
    return this.t4;
  }

  @Override
  public Stream<AutoCloseable> toStream() {
    return Stream.concat(
      super.toStream(),
      Stream.of(this.t4)
    );
  }
}

package com.github.joselion.maybe.rtuples;

import java.util.stream.Stream;

public class RTuple2<T1 extends AutoCloseable, T2 extends AutoCloseable> implements ResourceTuple {

  private final T1 t1;

  private final T2 t2;

  protected RTuple2(final T1 t1, final T2 t2) {
    this.t1 = t1;
    this.t2 = t2;
  }

  public static <T1 extends AutoCloseable, T2 extends AutoCloseable> RTuple2<T1, T2> of(final T1 t1, final T2 t2) {
    return new RTuple2<>(t1, t2);
  }

  public T1 getT1() {
    return this.t1;
  }

  public T2 getT2() {
    return this.t2;
  }

  @Override
  public Stream<AutoCloseable> toStream() {
    return Stream.of(this.t1, this.t2);
  }
}

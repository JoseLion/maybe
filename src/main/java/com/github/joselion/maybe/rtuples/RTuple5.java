package com.github.joselion.maybe.rtuples;

import java.util.stream.Stream;

public class RTuple5<
T1 extends AutoCloseable,
T2 extends AutoCloseable,
T3 extends AutoCloseable,
T4 extends AutoCloseable,
T5 extends AutoCloseable
> extends RTuple4<T1, T2, T3, T4> {

  private final T5 t5;

  public RTuple5(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5) {
    super(t1, t2, t3, t4);
    this.t5 = t5;
  }

  public static <
    T1 extends AutoCloseable,
    T2 extends AutoCloseable,
    T3 extends AutoCloseable,
    T4 extends AutoCloseable,
    T5 extends AutoCloseable
  > RTuple5<T1, T2, T3, T4, T5> of(final T1 t1, final T2 t2, final T3 t3, final T4 t4, final T5 t5) {
    return new RTuple5<>(t1, t2, t3, t4, t5);
  }

  public T5 getT5() {
    return this.t5;
  }

  @Override
  public Stream<AutoCloseable> toStream() {
    return Stream.concat(
      super.toStream(),
      Stream.of(this.t5)
    );
  }
}

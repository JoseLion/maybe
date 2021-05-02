package com.github.joselion.maybe.rtuples;

public interface RTuples {

  static <
    T1 extends AutoCloseable,
    T2 extends AutoCloseable
  > RTuple2<T1, T2> of(
    final T1 t1,
    final T2 t2
  ) {
    return RTuple2.of(t1, t2);
  }

  static <
    T1 extends AutoCloseable,
    T2 extends AutoCloseable,
    T3 extends AutoCloseable
  > RTuple3<T1, T2, T3> of(
    final T1 t1,
    final T2 t2,
    final T3 t3
  ) {
    return RTuple3.of(t1, t2, t3);
  }

  static <
    T1 extends AutoCloseable,
    T2 extends AutoCloseable,
    T3 extends AutoCloseable,
    T4 extends AutoCloseable
  > RTuple4<T1, T2, T3, T4> of(
    final T1 t1,
    final T2 t2,
    final T3 t3,
    final T4 t4
  ) {
    return RTuple4.of(t1, t2, t3, t4);
  }

  static <
    T1 extends AutoCloseable,
    T2 extends AutoCloseable,
    T3 extends AutoCloseable,
    T4 extends AutoCloseable,
    T5 extends AutoCloseable
  > RTuple5<T1, T2, T3, T4, T5> of(
    final T1 t1,
    final T2 t2,
    final T3 t3,
    final T4 t4,
    final T5 t5
  ) {
    return RTuple5.of(t1, t2, t3, t4, t5);
  }
}

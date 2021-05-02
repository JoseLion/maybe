package com.github.joselion.maybe.rtuples;

import java.util.stream.Stream;

public interface ResourceTuple extends AutoCloseable {

  Stream<AutoCloseable> toStream();

  @Override
  default void close() throws Exception {
    this.toStream().close();
  }
}

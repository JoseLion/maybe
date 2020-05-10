package com.github.joselion.maybe.helpers;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;

@Retention(RUNTIME)
@Target(TYPE)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public @interface UnitTest {
  
}

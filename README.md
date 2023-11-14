[![CI](https://github.com/JoseLion/maybe/actions/workflows/ci.yml/badge.svg)](https://github.com/JoseLion/maybe/actions/workflows/ci.yml)
[![CodeQL](https://github.com/JoseLion/maybe/actions/workflows/codeql.yml/badge.svg)](https://github.com/JoseLion/maybe/actions/workflows/codeql.yml)
[![Release](https://github.com/JoseLion/maybe/actions/workflows/release.yml/badge.svg)](https://github.com/JoseLion/maybe/actions/workflows/release.yml)
[![Pages](https://github.com/JoseLion/maybe/actions/workflows/pages.yml/badge.svg)](https://github.com/JoseLion/maybe/actions/workflows/pages.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.joselion/maybe?logo=sonatype)](https://central.sonatype.com/artifact/io.github.joselion/maybe)
[![javadoc](https://javadoc.io/badge2/io.github.joselion/maybe/javadoc.svg)](https://javadoc.io/doc/io.github.joselion/maybe)
[![codecov](https://codecov.io/gh/JoseLion/maybe/branch/main/graph/badge.svg)](https://codecov.io/gh/JoseLion/maybe)
[![License](https://img.shields.io/github/license/JoseLion/maybe)](https://github.com/JoseLion/maybe/blob/main/LICENSE)
[![Known Vulnerabilities](https://snyk.io/test/github/JoseLion/maybe/badge.svg)](https://snyk.io/test/github/JoseLion/maybe)

# Maybe - Safely handle exceptions

`Maybe<T>` is a monadic wrapper similar to `java.util.Optional`, but with a different intention. By leveraging `Optional<T>` benefits, it provides a functional API that safely allows to work with operations that throw checked (and unchecked) exceptions.

The motivation of `Maybe<T>` is to help developers avoid imperative _try/catch_ blocks while promoting safe exception handling which lives by functional programming principles.

## Features

* Type-safe differentiation between resolving a value vs. runnning effects.
* Rich and intuitive API based on `java.util.Optional`.
* Full interoperability with `java.util.Optional`.
* Includes a safe `Either<L, R>` type where only one side can be present at a time.
* Method reference friendly - The API provides methods with overloads that makes it easier to use [method reference](https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html) syntax.

## Presentations

- [JCon 2021 - Handling exception, the functional way](https://youtu.be/vaRjOukcIDA)

## Compatibility

As of **`v3.3.0`**, this library is compatible with JDK11+ by using [Multi-Release JARs](https://openjdk.org/jeps/238). However, using at least JDK17 to enjoy the new Java enhancements and features is highly recommended.

For example, the JDK17+ version of `Either<L, R>` uses a combination of [sealed classes](https://docs.oracle.com/en/java/javase/17/language/sealed-classes-and-interfaces.html) and [records classes](https://docs.oracle.com/en/java/javase/17/language/records.html), effectively making it an [algebraic data type](https://en.wikipedia.org/wiki/Algebraic_data_type) in Java. It means that `Either<L, R>` is an interface no other class can implement. The only implementations are the `Left` and `Right` records, which live within the interface. In short, it's a composite type created by combining other types. 

> If you need a JDK8-compatible version of `Maybe<T>`, you can use v2 instead. However, much like Java 8, v2 has reached its end-of-life, so it will not get any more features, patches, or updates.

## Breaking Changes (from v2 to v3)

- Due to changes on GitHub policies (and by consequence on Maven), it's no longer allowed to use `com.github` as a valid group ID prefix. To honor that and maintain consistency, **as of v3**, the artifact ID was renamed to `io.github.joselion.maybe`. If you want to use a version **before v3**, you can still find it using `com.github.joselion:maybe` artifact.
- A `SolveHandler` can no longer be empty. It either has the solved value or an error.
- The method `SolveHandler#filter` was removed to avoid the posibility of an inconsitent empty handler.
- The `WrapperException` type was removed. Errors now propagate downstream with the API.
- The method `EffectHandler#toMaybe` was removed as it didn't make sense for effects.
- All `*Checked.java` functions were renamed to `Throwing*.java`
  - For example, `FunctionChecked<T, R, E>` was renamed to `ThrowingFunction<T, R, E>`

## Install

[![Maven Central](https://img.shields.io/maven-central/v/io.github.joselion/maybe?logo=sonatype)](https://central.sonatype.com/artifact/io.github.joselion/maybe)

Maybe is available in [Maven Central](https://central.sonatype.com/artifact/io.github.joselion/maybe). You can checkout the latest version with the badge above.

**Gradle**

```gradle
implementation('io.github.joselion:maybe:x.y.z')
```

**Maven**

```xml
<dependency>
  <groupId>io.github.joselion</groupId>
  <artifactId>maybe</artifactId>
  <version>x.y.z</version>
</dependency>
```

## Basics

We'd use `Maybe<T>` for 3 different cases:
- **Solve:** When we need to obtain a value from a throwing operation.
- **Effects:** When we need to run effects that may throw exception(s), so no value is returned.
- **Closeables:** When we need to use a closeable resources on another operation (as in `try-with-resource` blocks)

We can create simple instances of Maybe using `Maybe.of(value)` or `Maybe.empty()` so we can chain throwing operations to it that will create the **handlers**. We also provide the convenience static methods `.from(ThrowingSupplier)` and `.from(ThrowingRunnable)` to create **handlers** directly from lambda expressions. Given the built-in lambda expression do not allow checked exception, we provide a few basic functional interfaces like `ThrowingFunction<T, R, E>`, that are just like the built-in ones, but with a `throws E` declaration. You can find them all in the [util packages][util-package-ref] of the library.

### Solve handler

Once a solver operation runs we'll get a [SolveHandler][solve-handler-ref] instance. This is the API that lets you handle the possible exception and produce a final value, or chain more operations to it.

```java
final Path path = Paths.get("foo.txt");

final List<String> fooLines = Maybe.from(() -> Files.readAllLines(path))
  .doOnError(error -> log.error("Fail to read the file", error)) // where `error` has type IOException
  .orElse(List.of());

// or we could use method reference

final List<String> fooLines = Maybe.of(path)
  .solve(Files::readAllLines)
  .doOnError(error -> log.error("Fail to read the file", error)) // where `error` has type IOException
  .orElseGet(List::of); // the else value is lazy now
```

The method `.readAllLines(..)` on example above reads from a file, which may throw a `IOException`. With the solver API we can run an effect if the exception was thrown. The we use `.orElse(..)` to safely unwrap the resulting value or another one in case of failure.

### Effect handler

When an effect operation runs we'll get a [EffectHandler][effect-handler-ref] instences. Likewise, this is the API to handle any possinble exception the effect may throw. This handler is very similar to the [SolveHandler][solve-handler-ref], but given an effect will never solve a value, it does not have any of the methods related to manipulating or unwrapping the value.

```java
Maybe
  .from(() -> {
    final String to = ...
    final String from = ...
    final String message = ...
    
    MailService.send(message, to, from);
  })
  .doOnError(error -> { // the `error` has type `MessagingException`
    MailService.report(error.getMessage());
  });
```

In the example above the `.send(..)` methods may throw a `MessagingException`. With the effect API we handle the error running another effect, i.e. reporting the error to another service.

### Closeable handler

Maybe also offers a way to work with [AutoCloseable](https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html) resources in a similar way the `try-with-resource` statement does, but with a more functional approach. We do this by creating a [CloseableHandler][closeable-handler-ref] instance from an autoclosable value, which will hold on to the value to close it at the end. The resource API lets you solve values or run effects using a closable resource, so we can ultimately handle the throwing operation with either the [SolveHandler][solve-handler-ref] or the [EffectHandler][effect-handler-ref].

```java
Maybe.withResource(myResource)
  .solve(res -> {
    // Return something using `res`
  });

Maybe.withResource(myResource)
  .effect(res -> {
    // do somthing with `res`
  });
```

In many cases, the resource you need will also throw an exception when we obtain it. We encourage you to first handle the exception that obtaining the resource may throw, and then map the value to a [CloseableHandler][closeable-handler-ref] to handle the next operation. For this [SolveHandler][solve-handler-ref] provides a `.mapToResource(..)` method so you can map solved values to resources.

```java
public Properties parsePropertiesFile(final String filePath) {
  return Maybe.of(filePath)
    .solve(FileInputStream::new)
    .catchError(err -> /* Handle the error */)
    .mapToResource(Function.identity())
    .solve(inputStream -> {
      final Properties props = new Properties();
      props.load(inputStream);

      return props;
    })
    .orElseGet(Properties::new);
}
```

> We know the first solved value extends from `AutoCloseable`, but the compiler doesn't. We need to explicitly map the value with `Function.identity()` so the compiler can safely ensure that the resource can be closed.

### Catching multiple exceptions

Some operation may throw multiple type of exceptions. We can choose how to handle each one using one of the `.catchError(..)` matcher overloads. This method can be chained one after another, meaning the first one to match the exception type will handle the error. However, the compiler cannot ensure exhaustive matching of the error types (for now), so we'll always need to handle a default case with a terminal operator.

```java
Maybe.of(path)
  .solve(Files::readAllLines) // throws IOException
  .catchError(FileNotFoundException.class, err -> ...)
  .catchError(FileSystemException.class, err -> ...)
  .catchError(EOFException.class, err -> ...)
  .orElse(err -> ...);
```

### Ambiguous overloads

The API has overloads where parameters can be passed as lambda expressions. Even though the parameter types are not ambiguous, there's one particular case where a lambda expression may not be inferred as the expected type:

```java
() -> {
  throw new Exception("...");
}

arg -> {
  throw new Exception("...");
} 
```

If the lambda expression finishes throwing an exception, the compile will not be able to tell the difference between a type that returns a value and a type that doesn't. For example, the first lambda expression above can be either a `ThrowingRunnable<Exception>` or a `ThrowingSupplier<Object, Exception>`. Similarly, the second expression can either be a `ThrowingConsumer<Object, Exception>` or a `ThrowingFunction<Object, Object, Exception>`.

If you ever run into ambiguity issues in the Maybe API, you have 2 easy options to solve the problem:

1. (Recommended) Explicity setting the generic type(s) of the method will deambiguate the overload you expect to be used.

```java
Maybe.<IOException>from(() -> { // param is a `ThrowingRunnable<IOException>`
  throw new IOException("...");
});

Maybe.<String, IOException>from(() -> { // param is a `ThrowingSupplier<String, IOException>`
  throw new IOException("...");
});
```

2. Casting the lambda expression to the specific `@FunctionalInterface` type you want to use.

```java
Maybe.from((ThrowingRunnable<IOException>) () -> {
  throw new IOException("...");
});

Maybe.from((ThrowingSupplier<String, IOException>) () -> {
  throw new IOException("...");
});
```

## The `Either<L, R>` type

An awesome extra of `Maybe`, is that it provides a useful [Either<L, R>][either-ref] type which guarantees that the only one of the sides (left `L` or right `R`) is present per instance. That is possible thanks to:

1. `Either<L, R>` is a [sealed interface](https://docs.oracle.com/en/java/javase/15/language/sealed-classes-and-interfaces.html). It cannot be implemented by any class nor anonimously instantiated in any way.
2. There only exist 2 implementations of `Either<L, R>`: `Either.Left` and `Either.Right`. In those implementations, only one field is used to store the instance value.
3. It's not possible to create an `Either<L, R>` instance of a `null` value.

The `Either<L, R>` makes a lot of sense when resolving values from throwing operations. At the end of the day, you can end up with either the solved value (`Rigth`) or the thrown exception (`Left`). You can convert from a `SolveHandler<T, E>` to an `Either<E, T>` usong the `SolveHandler#toEither` terminal operator.

To use `Either` on its own, use the factory methods to create an instance and the API to handle/unwrap the value:

```java
public Either<String, Integer> fizzOrNumber(final int value) {
  return value % 7 == 0
    ? Either.ofLeft("fizz")
    : Either.ofRight(value);
}

public static void main (final String[] args) {
  final var sum = IntStream.range(1, 25)
    .boxed()
    .map(this::fizzOrNumber)
    .map(either ->
      either
        .onLeft(fizz -> log.info("Multiple of 7: {}", fizz))
        .onRight(value -> log.info("Value: {}", value))
        .unwrap(
          fizz -> 0,
          Function.identity()
        )
    )
    .reduce(0, Integer::sum);

  log.info("The sum of non-fizz values is: {}", sum);
}
```

Take a look at the [documentation][either-ref] to see all the methods available in the `Either<L, R>` API.

## Optional interoperability

The API provides full interoperability with Java's `Optional`. You can use `Maybe.from(Optional)` overload to create an instance from an optional value, or you can use the terminal operator `.toOptional()` to unwrap the value to an optional too. However, there's a change you might want to create a `Maybe<T>` withing the Optional API or another library like [Project Reactor](https://projectreactor.io/), like from a `.map(..)` method. To make this esier the API provides overloads to that create partial applications, and when fully applied return the specific handler.

So instead of having nested lambdas like this:

```java
Optional.ofNullable(rawValue)
  .map(str -> Maybe.from(() -> Base64.getDecoder().decode(str)))
  .map(decoded -> decoded.catchError(...));
```

You can use the partial application overload and use method reference syntax:

```java
Optional.ofNullable(rawValue)
  .map(Maybe.partial(Base64.getDecoder()::decode))
  .map(decoded -> decoded.catchError(...));
```

## API Reference

You can find more details of the API in the [latest Javadocs](https://javadoc.io/doc/io.github.joselion/maybe).If you need to check the Javadocs of an older version you can also use the full URL as shown below. Just replace `<x.y.z>` with the version you want to see:

```
https://javadoc.io/doc/io.github.joselion/maybe/<x.y.z>
```

## Something's missing?

Suggestions are always welcome! Please create an [issue](https://github.com/JoseLion/maybe/issues/new) describing the request, feature, or bug. I'll try to look into it as soon as possible 🙂

## Contributions

Contributions are very welcome! To do so, please fork this repository and open a Pull Request to the `main` branch.

## License

[Apache License 2.0](https://github.com/JoseLion/maybe/blob/main/LICENSE)

<!-- References -->
[solve-handler-ref]: https://javadoc.io/doc/io.github.joselion/maybe/latest/com/github/joselion/maybe/SolveHandler.html
[effect-handler-ref]: https://javadoc.io/doc/io.github.joselion/maybe/latest/com/github/joselion/maybe/EffectHandler.html
[closeable-handler-ref]: https://javadoc.io/doc/io.github.joselion/maybe/latest/com/github/joselion/maybe/CloseableHandler.html
[util-package-ref]: https://javadoc.io/doc/io.github.joselion/maybe/latest/com/github/joselion/maybe/util/package-summary.html
[either-ref]: https://javadoc.io/doc/io.github.joselion/maybe/latest/com/github/joselion/maybe/Either.html

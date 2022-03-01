[![JoseLion](https://circleci.com/gh/JoseLion/maybe.svg?style=svg)](https://app.circleci.com/pipelines/github/JoseLion/maybe)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.joselion/maybe.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.joselion%22%20AND%20a:%22maybe%22)
[![javadoc](https://javadoc.io/badge2/com.github.joselion/maybe/javadoc.svg)](https://javadoc.io/doc/com.github.joselion/maybe)
[![codecov](https://codecov.io/gh/JoseLion/maybe/branch/master/graph/badge.svg)](https://codecov.io/gh/JoseLion/maybe)

# Maybe - Safely handle exceptions

`Maybe<T>` is a monadic wrapper similar `java.util.Optional`, but with a different intention. By leveraging `Optional<T>` benefits, it provides a functional API that safely allows us to perform operations that may or may not throw checked and unchecked exceptions.

The wrapper intends to help us avoid the imperative _try/catch_ syntax, while promoting safe exception handling and functional programming principles.

## Features

* Type-safe differentiation between resolving a value vs. runnning effects.
* Easy and rich API similar to Java's `Optional`.
* Full interoperability with `java.util.Optional`.
* Method reference friendly - The API provides methods with overloads that makes it easier to use [method reference](https://docs.oracle.com/javase/tutorial/java/javaOO/methodreferences.html) syntax.

## Requirements

> The only requirement to use this library is Java 8 or higher.

## Install

[![Maven Central](https://img.shields.io/maven-central/v/com.github.joselion/maybe.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.joselion%22%20AND%20a:%22maybe%22)

Maybe is available in [Maven Central](https://mvnrepository.com/artifact/com.github.joselion/maybe). You can checkout the latest version with the badge above.

**Gradle**

```gradle
implementation('com.github.joselion:maybe:x.y.z')
```

**Maven**

```xml
<dependency>
  <groupId>com.github.joselion</groupId>
  <artifactId>maybe</artifactId>
  <version>x.y.z</version>
</dependency>
```

## Basics

We'd use `Maybe<T>` for 2 defferent casses:
- **Resolve:** When we need to obtain a value from a throwing operation.
- **Effects:** When we need to run an effect from a throwing operation, so no value is returned.

We can create simple instances of Maybe using `Maybe.just(value)` or `Maybe.nothing()` so we can chain throwing operations to it that will create the **handlers**. We also provide the convenience static methods `.fromResolver(..)` and `.fromEffect(..)` that let us create **handlers** directly from lambda expressions. Given the built-in lambda expression do not allow checked exception, we provide a few basic functional interfaces like `FunctionChecked<T, R, E>`, that are just like the built-in ones, but with a `throws E` declaration. You can find them all [here](src\main\java\com\github\joselion\maybe\util)

### Resolve handler

Once a resolver operation runs we'll get a [ResolveHandler][resolve-handler-ref] instance. This is the API that let us handle the possible exception and produce a final value, or chain more operations to it.

```java
final Path path = Paths.get("foo.txt");

final List<String> fooLines = Maybe.fromResolver(() -> Files.readAllLines(path))
  .doOnError(error -> log.error("Fail to read the file", error)) // where `error` has type IOException
  .orElse(List.of());

// or we could use method reference

final List<String> fooLines = Maybe.just(path)
  .resolver(Files::readAllLines)
  .doOnError(error -> log.error("Fail to read the file", error)) // where `error` has type IOException
  .orElse(List.of());
```

The method `.readAllLines(..)` on example above reads from a file, which may throw a `IOException`. With the resolver API we can run an effect if the exception was thrown. The we use `.orElse(..)` to safely unwrap the resulting value or another one in case of failure.

### Effect handler

When an effect operation runs we'll get a [EffectHandler][effect-handler-ref] instences. Likewise, this is the API that allow us to handle any possinble exception the effect may throw. This handler is very similar to the [ResolveHandler][resolve-handler-ref], but given an effect will never resolve a value, it does not have any of the methods related to manipulating or unwrapping the value.

```java
Maybe.fromEffect(() -> {
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

### Catching multiple exceptions

Some operation may throw multiple type of exceptions. We can choose how to handle each one using one of the `.catchError(..)` matcher overloads. This method can be chained one after another, meaning the first one to match the exception type will handle the error. However, the compiler cannot ensure exhaustive matching of the error types (for now), so we'll always need to handle a default case with a terminal operator.

```java
Maybe.just(path)
  .resolve(Files::readAllLines) // throws IOException
  .catchError(FileNotFoundException.class, () -> ...)
  .catchError(FileSystemException.class, () -> ...)
  .catchError(EOFException.class, () -> ...)
  .orElse(() -> ...);
```

## Optional interoperability

The API provides full interoperability with Java's `Optional`. You can use `Maybe.fromOptional(..)` to create an instance from an optional value, or you can use the terminal operator `.toOptional()` to unwrap the value to an optional too. However, there's a change you might want to create a `Maybe<T>` withing the Optional API or another library like [Project Reactor](https://projectreactor.io/), like from `.map(..)` method. To make this esier the API provides overloads to that create partial applications, and when fully applied return the specific handler.

So instead of having nested lambdas like this:

```java
Optional.ofNullable(rawValue)
  .map(str -> Maybe.fromResolver(() -> Base64.getDecoder().decode(str)))
  .map(decoded -> decoded.catchError(...));
```

You can use the partial application overload and use method reference syntax:

```java
Optional.ofNullable(rawValue)
  .map(Maybe.fromResolver(Base64.getDecoder()::decode))
  .map(decoded -> decoded.catchError(...));
```

## Autocloseable resources

Maybe also offers a way to work with [AutoCloseable](https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html) resources in a similar way the `try-with-resource` statement does, but with a more functional approach. We do this by creating a [ResourceHolder][resource-holder-ref] instance from an autoclosable value, which will hold on to the value to close it at the end. The resource API let us resolve or run effects using the resource, so we can ultimately handle the throwing operation with either the [ResolveHandler][resolve-handler-ref] or the [EffectHandler][effect-handler-ref].

```java
Maybe.withResource(myResource)
  .resolveClosing(res -> {
    // Return something using `res`
  });

Maybe.withResource(myResource)
  .runEffectClosing(res -> {
    // do somthing with `res`
  });
```

In many cases, the resource you need will also throw an exception when we obtain it. We encourage you to first handle the exception that obtaining the resource may throw, and then map the value to a [ResourceHolder][resource-holder-ref] to handle the next operation. For this [ResolveHandler][resolve-handler-ref] provides a `.mapToResource(..)` method so you can map resolved values to resources.

```java
public Properties parsePropertiesFile(final String filePath) {
  return Maybe.just(filePath)
    .resolve(FileInputStream::new)
    .catchError(() -> /* Handle the error */)
    .mapToResource(Function.identity())
    .resolveClosing(inputStream -> {
      final Properties props = new Properties();
      props.load(inputStream);

      return props;
    })
    .orElse(new Properties());
}
```

> We know the first resolved value extends from `AutoCloseable`, but the compiler doesn't. We need to explicitly map the value with `Function.identity()` so the compiler can safely ensure that the resource can be closed.

## API Reference

You can find more details of the API in the [latest Javadocs](https://javadoc.io/doc/com.github.joselion/maybe).If you need to check the Javadocs of an older version you can also use the full URL as shown below. Just replace `<x.y.z>` with the version you want to see:

```http
https://javadoc.io/doc/com.github.joselion/maybe/<x.y.z>
```

## Something's missing?

Suggestions are always welcome! Please create an [issue](https://github.com/JoseLion/maybe/issues/new) describing the request, feature, or bug. I'll try to look into it as soon as possible 🙂

## Contributions

Contributions are very welcome! To do so, please fork this repository and open a Pull Request to the `master` branch.

## License

[Apache License 2.0](LICENSE)

<!-- References -->
[resolve-handler-ref]: src/main/java/com/github/joselion/maybe/ResolveHandler.java
[effect-handler-ref]: src/main/java/com/github/joselion/maybe/EffectHandler.java
[resource-holder-ref]: src/main/java/com/github/joselion/maybe/ResourceHolder.java

[![Maven Central](https://img.shields.io/maven-central/v/com.github.joselion/maybe.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.joselion%22%20AND%20a:%22maybe%22)
[![JoseLion](https://circleci.com/gh/JoseLion/maybe.svg?style=svg)](https://app.circleci.com/pipelines/github/JoseLion/maybe)
[![codecov](https://codecov.io/gh/JoseLion/maybe/branch/master/graph/badge.svg)](https://codecov.io/gh/JoseLion/maybe)

# Maybe for Java

Maybe for Java is not a replacement of `java.util.Optional`. Instead, it leverages its benefits to create a functional API that allows to run operations that may throw an exception. The intention is not only to avoid the imperative try/catch, but also to promote safe exception handling and functional programming in Java.

## Requirements

> The only requirement to use this librarary is Java 8 or higher.

## Install

[![Maven Central](https://img.shields.io/maven-central/v/com.github.joselion/maybe.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.joselion%22%20AND%20a:%22maybe%22)

Maybe is available in [Maven Central](https://mvnrepository.com/artifact/com.github.joselion/maybe). You can checkout the latest version with the badge above.

**Gradle**

```gradle
implementation(group: 'com.github.joselion', name: 'maybe', version: 'X.X.X')
```

**Maven**

```xml
<dependency>
  <groupId>com.github.joselion</groupId>
  <artifactId>maybe</artifactId>
  <version>X.X.X</version>
</dependency>
```

## Usage

The API has two basic usages:
- **Resolve:** When we need to get a value/result from a throwing operation.
- **RunEffect:** When we need to only run some effect so no value will be returned from the throwing operation.

It's also posibble to create simple intances of Maybe using `Maybe.just(value)` or `Maybe.nothing()`. As the monad may or may not contain a value, a quick way to unbox the value is with the method `.toOptional()`, which will turn the Maybe into an Optional that may or may not contain a value. Some other helpful methods are availble wichi are listed bellow in this document.

### Resolve

To resolve a value we can use `Maybe.resolve(..)`, which expects a [SupplierChecked][supplier-checked-ref] function as argument. This function is like a regular `Supplier` but its content is allowed to throw exceptions. The result of this function will be a [ResolveHandler][resolve-handler-ref] which works as an API to handle/catch the possible error and then return the value or a default, throw an exception, or return to the Maybe API to chain other operations.

```java
Path path = Paths.get("foo.txt");

List<String> fooLines = Maybe.resolve(() -> Files.readAllLines(path, UTF_8))
  .onError(error -> List.of("<Error reading file>")) // the `error` arg here is of type `IOException`
  .orDefault(List.of());
```

In the example above we want to read from a file, which may fail because of a `IOException`. Using the API we set a value to return in case of error, and to safely unbox the value we also give a default value in case the error was not handled.

### RunEffect

To only run an effect we can use `Maybe.runEffect(..)`, which expects a [RunnableChecked][runnable-checked-ref] function as argumant. Aswell, this function is like a regular `Runnable` but its content is allowed to throw exceptions. The result of this function will be an [EffectHandler][effect-handler-ref] which works as an API to handle/catch the possible error, throw an exception, or return to the Maybe API to chain other operations.

```java
Maybe.runEffect(() -> {
  final String to: ...
  final String from: ...
  final String message: ...
  
  MailService.send(message, to, from);
})
.onError(error -> { // the `error` arg here is of type `MessagingException`
  MailService.report(error.getMessage());
});
```

In the example above we want to send an email using a mail service which may throw a `MessagingException` in case of failure. Using the API we handle the error and report the failure. An effect will not have a value, so if we use the `.and()` operation after this the result on the maybe will be of type `Void`.

### Catching multiple exceptions

If an operation may throw multiple type of exceptions, they can be catched using the `.catchError(..)` method in the handlers. They can be declared one after another and the first catch the exception type will handle the error. As the method `.onError(..)` catches all exceptions, it's important not to use it before `.catchError(..)`.

```java
// BAD
Maybe.resolve(() -> ...)
  .catchError(ErrorA.class, () -> ...)
  .onError(() -> ...)
  .catchError(ErrorB.class, () -> ...)
  .catchError(ErrorC.class, () -> ...);

// GOOD
Maybe.resolve(() -> ...)
  .catchError(ErrorA.class, () -> ...)
  .catchError(ErrorB.class, () -> ...)
  .catchError(ErrorC.class, () -> ...)
  .onError(() -> ...);
```

## API

### Maybe:

| Method                    | Description |
| ------------------------- | ----------- |
| `Maybe.just(value)`       | Returns a `Maybe` monad wrapping the given value. If the value is `null`, it returns a `nothing()`. |
| `Maybe.nothing()`         | Returns a `Maybe` monad with nothing on it. This means the monad does not contains a value because an exception may have occurred. |
| `Maybe.resolve(resolver)` | Resolves the value of a throwing operation using a `SupplierChecked` expression. Returning then a `ResolveHandler` which allows to handle the possible error and return a safe value. |
| `Maybe.runEffect(effect)` | Runs an effect that may throw an exception using a `RunnableChecked` expression. Returning then an `EffectHandler` which allows to handle the possible error. |
| `.map(mapper)`            | Maps the current success value of the monad to another value using the provided `Function` mapper. |
| `.flatMap(mapper)`        | Maps the current success value of the monad to another value using the provided `Function` mapper. |
| `.thenResolve(resolver)`  | Chain the `Maybe` with another resolver, if and only if the previous operation was handled with no errors. The value of the previous operation is passed as argument of the `FunctionChecked`. |
| `.thenRunEffect(effect)`  | Chain the `Maybe` with another effect, if and only if the previous operation was handled with no errors. |
| `.cast(type)`             | If the value is present in the monad, casts the value to another type. In case of any exception during the cast, a Maybe with `nothing` is returned. |
| `.hasValue()`             | Checks if the `Maybe` has a value. |
| `.hasNothing()`           | Checks if the `Maybe` has nothing. That is, when no value is present. |
| `.toOptional()`           | Safely unbox the value of the monad as a `java.util.Optional` which may or may not contain a value. |

### ResolveHandler

| Method                            | Description |
| --------------------------------- | ----------- |
| `.onError(handler)`               | If an error exits, handle the error and return a new value. The error is passed in the argunment of to the `handler` function. |
| `.catchError(errorType, handler)` | Catch an error if it's instance of the `errorType` passed, then handle the error and return a new value. The catched error is passed in the argument of the `handler` function. |
| `.and()`                          | Allows the ResolveHandler API to go back to the Maybe API. This is useful to continue chaining more Maybe operations. |
| `.orDefault(defaultValue)`        | Returns the value resolved/handled if present. A default value otherwise. |
| `.orThrow()`                      | Returns the value resolved/handled if present. Throws the error otherwise. |
| `.orThrow(errorMapper)`           | Returns the value resolved/handled if present. Throws another error otherwise. |

### EffectHandler

| Method                            | Description |
| --------------------------------- | ----------- |
| `.onError(handler)`               | Handle an error if present or if was not already handled. The error is passed in the argument of the `handler` function. |
| `.catchError(errorType, handler)` | Catch an error if it's instance of the `errorType` passed and it was not already handled. The catched error is passed in the argument of the `handler` function. |
| `.and()`                          | Allows the EffectHandler API to go back to the Maybe API. This is useful to continue chaining more Maybe operations. |
| `.onErrorThrow()`                 | Throws the error if present. Does nothing otherwise. |
| `.onErrorThrow(errorMapper)`      | If an error is present, map the error to another exception and throw it. Does nothing otherwise. |

[supplier-checked-ref]: src/main/java/com/github/joselion/maybe/util/SupplierChecked.java
[resolve-handler-ref]: src/main/java/com/github/joselion/maybe/ResolveHandler.java
[runnable-checked-ref]: src/main/java/com/github/joselion/maybe/util/RunnableChecked.java
[effect-handler-ref]: src/main/java/com/github/joselion/maybe/EffectHandler.java

## Something's missing?

Please create an [issue](https://github.com/JoseLion/maybe/issues/new) describing your request, feature or bug. I'll try to look into it as soon as possible 🙂

## Contributions

Contributions are very welcome! To do so, please fork this repository and open a Pull Request to the `master` branch.

## License

[Apache License 2.0](LICENSE)

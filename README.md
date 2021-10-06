[![Maven Central](https://img.shields.io/maven-central/v/com.github.joselion/maybe.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.joselion%22%20AND%20a:%22maybe%22)
[![JoseLion](https://circleci.com/gh/JoseLion/maybe.svg?style=svg)](https://app.circleci.com/pipelines/github/JoseLion/maybe)
[![codecov](https://codecov.io/gh/JoseLion/maybe/branch/master/graph/badge.svg)](https://codecov.io/gh/JoseLion/maybe)

# Maybe for Java

Maybe for Java is not a replacement of `java.util.Optional`. Instead, it leverages its benefits to create a functional API that allows to run operations that may throw an exception. The intention is not only to avoid the imperative try/catch, but also to promote safe exception handling and functional programming in Java.

## Requirements

> The only requirement to use this library is Java 8 or higher.

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

It's also possible to create simple instances of Maybe using `Maybe.just(value)` or `Maybe.nothing()`. As the monad may or may not contain a value, a quick way to unbox the value is with the method `.toOptional()`, which will turn the Maybe into an Optional that may or may not contain a value. Some other helpful methods are available which are listed bellow in this document.

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

To only run an effect we can use `Maybe.runEffect(..)`, which expects a [RunnableChecked][runnable-checked-ref] function as argument. As well, this function is like a regular `Runnable` but its content is allowed to throw exceptions. The result of this function will be an [EffectHandler][effect-handler-ref] which works as an API to handle/catch the possible error, throw an exception, or return to the Maybe API to chain other operations.

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

If an operation may throw multiple type of exceptions, they can be handle using the `.catchError(..)` method in the handlers. This method can be chained one after another, so the first one to match the exception type will handle the error. As the method `.onError(..)` catches all exceptions, it's important not to use it before `.catchError(..)`.

```java
// BAD
Maybe.resolve(() -> ...)
  .catchError(ErrorA.class, () -> ...)
  .onError(() -> ...)
  .catchError(ErrorB.class, () -> ...) // The error will not be present at this point
  .catchError(ErrorC.class, () -> ...);

// GOOD
Maybe.resolve(() -> ...)
  .catchError(ErrorA.class, () -> ...)
  .catchError(ErrorB.class, () -> ...)
  .catchError(ErrorC.class, () -> ...)
  .onError(() -> ...);
```

## AutoCloseable Resources

The library also offers a way to easily handle [AutoCloseable](https://docs.oracle.com/javase/8/docs/api/java/lang/AutoCloseable.html) resources in a similar way the `try-with-resource` statement does, but again, with a more functional approach. The idea is not only to avoid imperative code, but also to promote the correct handle of all throwing operations step by step, instead of handling them all in a single catch for instance.

To use a resource with `Maybe`, we first need to pass it to the API, which will hold the value until its used in some operations, and finally closed. The resource API allows to resolve or run effects with the provided resource. Just like the usual `resolve` and `runEffect`, with the difference that the resource will be closed when the operation finishes.

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

**Note:** For increased type safety, the parameter that `.withResource(..)` takes should extend from `AutoCloseable`.

The methods `.resolveClosing(..)` and `.runEffectClosing(..)` will return a [ResolveHandler][resolve-handler-ref] and [EffectHandler][effect-handler-ref] instance respectively, so you can handle them just like any other `Maybe` operation. However, it's most likely that the resource you need to use will also throw it's own exception. As a good practice, the library promotes first handling the exception that the resource may throw and then doing any operation with it. To enable this, the [ResolveHandler][resolve-handler-ref] provides a method `.mapToResource(..)` that allows to map any resolved value to a resource:

```java
public Properties parsePropertiesFile(final String filePath) {
  return Maybe.just(filePath)
    .thenResolve(InputFileStream::new)
    .doOnError(error -> /* Handle the error appropiately */)
    .mapToResource(Function.identity())
    .resolveClosing(inputStream -> {
      final Properties props = new Properties();
      props.load(inputStream);

      return props;
    })
    .onError(error -> /* Handle the error appropiately */)
    .orDefault(new Properties());
}
```

**Note:** Even though the first resolved value already extends from `AutoCloseable`, we need to map it so the type constraint is honored. An identity function will suffice in these cases. Specifically, for `Maybe<T>` it's impossible to know at compile-time that the type `T` already extends from `AutoCloseable`, so we'll always need a map function the ensures this constraint.

## API Description

### Maybe

| Method                    | Description |
| ------------------------- | ----------- |
| `Maybe.just(value)`       | Returns a `Maybe` monad wrapping the given value. If the value is `null`, it returns a `nothing()`. |
| `Maybe.nothing()`         | Returns a `Maybe` monad with nothing on it. This means the monad does not contains a value because an exception may have occurred. |
| `Maybe.resolve(resolver)` | Resolves the value of a throwing operation using a `SupplierChecked` expression. Returning then a `ResolveHandler` which allows to handle the possible error and return a safe value. |
| `Maybe.runEffect(effect)` | Runs an effect that may throw an exception using a `RunnableChecked` expression. Returning then an `EffectHandler` which allows to handle the possible error. |
| `.withResource(resource)` | Creates a [ResourceHolder](#ResourceHolder) from which you can resolve or run effects using the provided resource, which will be automatically closed when the operation finishes |
| `.map(mapper)`            | Maps the current success value of the monad to another value using the provided `Function` mapper. |
| `.flatMap(mapper)`        | Maps the current success value of the monad to another value using the provided `Function` mapper. |
| `.thenResolve(resolver)`  | Chain the `Maybe` with another resolver, if and only if the previous operation was handled with no errors. The value of the previous operation is passed as argument of the `FunctionChecked`. |
| `.thenRunEffect(effect)`  | Chain the `Maybe` with another effect, if and only if the previous operation was handled with no errors. |
| `.cast(type)`             | If the value is present in the monad, casts the value to another type. In case of any exception during the cast, a Maybe with `nothing` is returned. |
| `.hasValue()`             | Checks if the `Maybe` has a value. |
| `.hasNothing()`           | Checks if the `Maybe` has nothing. That is, when no value is present. |
| `.toOptional()`           | Safely unbox the value of the monad as a `java.util.Optional` which may or may not contain a value. |

### ResolveHandler

| Method                                   | Description |
| ---------------------------------------- | ----------- |
| `.doOnError(handler)`                    | Run an effect if an error is present. The error is passed in the argument of to the `effect` consumer. |
| `.onError(handler)`                      | If an error is present, handle the error and return a new value. The error is passed in the argument of to the `handler` function. |
| `.catchError(errorType, handler)`        | Catch an error if it's instance of the `errorType` passed, then handle the error and return a new value. The caught error is passed in the argument of the `handler` function. |
| `.and()`                                 | Allows the ResolveHandler API to go back to the Maybe API. This is useful to continue chaining more Maybe operations. |
| `.orDefault(defaultValue)`               | Returns the value resolved/handled if present. A default value otherwise. |
| `.orSupplyDefault(defaultValueSupplier)` | Returns the value resolved/handled if present. A default value otherwise supplied by the supplier. |
| `.orThrow()`                             | Returns the value resolved/handled if present. Throws the error otherwise. |
| `.orThrow(errorMapper)`                  | Returns the value resolved/handled if present. Throws another error otherwise. |
| `.mapToResource(mapper)`                 | Maps the value to an `AutoCloseable` resource if present, returning a [ResourceHolder](#ResourceHolder) with the mapped value. Otherwise, returns an empty [ResourceHolder](#ResourceHolder).

### EffectHandler

| Method                            | Description |
| --------------------------------- | ----------- |
| `.onError(handler)`               | Handle an error if present or if was not already handled. The error is passed in the argument of the `handler` function. |
| `.catchError(errorType, handler)` | Catch an error if it's instance of the `errorType` passed and it was not already handled. The caught error is passed in the argument of the `handler` function. |
| `.and()`                          | Allows the EffectHandler API to go back to the Maybe API. This is useful to continue chaining more Maybe operations. |
| `.onErrorThrow()`                 | Throws the error if present. Does nothing otherwise. |
| `.onErrorThrow(errorMapper)`      | If an error is present, map the error to another exception and throw it. Does nothing otherwise. |

### ResourceHolder

| Method                      | Description |
| --------------------------- | ----------- |
| `.resolveClosing(resolver)` | Same as `Maybe.resolve(resolver)`, with the difference that it takes a `FunctionChecked` which argument contains the resource if present, if not, the operation will not run. The resource will always be closed at the end. |
| `.runEffectClosing(effect)` |Same as `Maybe.runEffect(effect)`, with the difference that it takes a `ConsumerChecked` which argument contains the resource if present, if not, the operation will not run. The resource will always be closed at the end. |

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

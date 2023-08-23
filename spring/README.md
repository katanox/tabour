## Tabour Spring

Tabour spring provides functionality to reduce the work of setting up a tabour container in a Spring boot application

## Requirements

- Spring boot > 3.x.x

## Usage

Tabour Spring provides the annotation: `AutoconfigureTabour` which needs to be placed on your Spring Boot application
class

Then you need to setup your registries as Beans

Tabour Spring will collect these registries, register them and start tabour

## Configuration

You can configure the num of threads for tabour using the:
` tabour.config.num-of-threads` configuration option

## Example

```kotlin
@Bean
fun sqsRegistry(): SqsRegistry<String> {
    val registry =
        sqsRegistry("registry1", EnvironmentVariableCredentialsProvider.create(), Region.EU_WEST_1)

    // replace with your consumers
    val consumers = emptyList()

    return registry.apply { addConsumers(consumers) }
}
```

Add the annotation:

```kotlin
@AutoconfigureTabour
class MyApplication 
```

And Tabour will start with `registry1` registry available and also create a new bean which you can inject the tabour
container to produce messages:

```kotlin
class MyClass(private val tabour: Tabour)
```
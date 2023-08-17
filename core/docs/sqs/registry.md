## SqsRegistry

The SqsRegistry is a registry where we can register the following classes

- [SqsProducer](producer.md)
- [SqsConsumer](consumer.md)

## Create a new registry

```kotlin
// using credentials form the environment
val registry =
    sqsRegistry("registry-key", EnvironmentVariableCredentialsProvider.create(), Region.EU_WEST_1)

// using username password

val registry =
    sqsRegistry(
        "registry-key",
        StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)),
        Region.EU_WEST_1
    )
```

After creating the registry, we can register consumers and producers to it.

```kotlin
val producer = sqsProducer(URL("https://queue-url.com"), "key-1") {}
val producer2 = sqsProducer(URL("https://queue-url.com"), "key-2") {}
val consumer = sqsConsumer(URL("https://queue-url.com")) {}
val consumer2 = sqsConsumer(URL("https://queue-url-2.com")) {}

registry.addConsumer(consumer).addConsumer(consumer2).addProducer(producer).addProducer(producer2)

// or register multiple consumers and producers at once

register.addConsumers(listOf(consumer, consumer2)).addProducers(listOf(producer, producer2))
```

Then we need to register the registry itself:

```kotlin
tabour.register(registry)
```

And then we can start tabour and the consumers and producers will be able to work. Consumers will start consuming
messages and producers will be available to produce a message when called

```kotlin
tabour.start()
```
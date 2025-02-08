## SqsProducer

## Create a producer

```kotlin
val producer =
    sqsProducer(URI("https://queue-url.com"), "key-1") {
        onError = { error: ProductionError -> println(error) }
        config = { retries = 1 }
    }
```

The snippet above, creates a new producer to produce sqs messages to the queue in url `https://queue-url.com`

### onError

The `onError` handler is called after the producer fails to produce the message `config.retries` times.
In this case after 1 failed attempt the `onError` handler will be used

### Available configuration options and default values

```kotlin
/** How many times the producer will try to produce a message */
var retries: Int = 1
```

### Producing a message

Production of messages works differently than consumption.
First we need to configure and register the producer in a registry.
When we want to produce a message, we need to use the `Tabour`
container (after registering the registry to the `Tabour` container)

Example

```kotlin
val tabourContainer = tabour {}
// setup and register registries...
tabourContainer.produceMessage("registry-key", "producer-key") {
    FifoQueueData("message", "message-group-id")
    // or
    NonFifoQueueData("message")
}

```

We can produce a batch of message using `BatchDataForProduction` which contains a list of `FifoQueueData` and `NonFifoQueueData`

> [!IMPORTANT]
> If a registry with the given key is not found, nothing happens

> [!IMPORTANT]
> If a producer with the given key is not found, nothing happens

> [!IMPORTANT]
> If the producer lambda returns a null as its first part, the message is not produced


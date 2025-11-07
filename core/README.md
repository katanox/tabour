# Tabour Core

## Concept

You need to register the registry to the Tabour instance which get activated when the instance starts.

## Registry

A registry is a collection of `Consumers` and `Producers`.

### Available registries

- [SqsRegistry](docs/sqs/registry.md)

## Consumer

A consumer is a handler which we use to retrieve messages from SQS

## Producer

A producer is a handler that we use to trigger when we want to produce a message

A producer is a class that has been configured to produce messages to a specific queue/topic depending on the
messaging system.

## Examples

## Create a consumer

```kotlin
// create the container using the default AWS credentials chain
val container = tabour { numOfThreads = 1 }
val config = sqsRegistryConfiguration("test-registry", "eu-west-1")
val sqsRegistry = sqsRegistry(config)

val consumer =
    sqsConsumer(
        URL.of(URI.create("https://aws.111.fifo-queue-url.fifo"), null),
        key = "my-consumer",
        onSuccess = { message ->
            println(message)
            true
        },
        onError = ::println,
    ) { config = sqsConsumerConfiguration { sleepTime = 200.milliseconds } }


// register and start the container
container.register(sqsRegistry.addConsumer(consumer)).start()
```

Here we specify a consumer that reads from `https://aws.111.fifo-queue-url.fifo` every 200 ms.
We can also specify different aws credentials using the `credentialsProvider` field:

``` kotlin
val config =
    sqsRegistryConfiguration("test-registry", "eu-west-1") {
        credentialsProvider = StaticCredentialsProvider {
            accessKeyId = "test"
            secretAccessKey = "test"
        }
    }
```

## Create a producer

The idea of registering a producer is the same:

```kotlin
val producer =
    sqsProducer(URL.of(URI.create("https://aws.111.fifo-queue-url.fifo"), null), "test-producer", ::println)
```

Where we use `::println` as the `onError` handler.

Registering a producer is the same with registering a consumer:

```kotlin
container.register(sqsRegistry.addProducer(producer)).start()
```

Which then we can trigger using:

```kotlin
val sqsProducerConfiguration =
    DataProductionConfiguration<SqsProductionData>(
        produceData = {
            SqsProductionData.Single {
                messageBody = "this is a fifo test message"
                messageGroupId = "group_1"
            }
        },
        resourceNotFound = { _ -> println("Resource not found") },
    )
container.produceMessage("test-registry", "test-producer", sqsProducerConfiguration)
```

We use `produceData` to return an instance
of [SqsProductionData](src/main/kotlin/com/katanox/tabour/sqs/production/SqsProducer.kt) which represents either:

1. A Single message
2. Or a Batch of messages
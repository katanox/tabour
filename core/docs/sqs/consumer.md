## SqsConsumer

## Create a consumer

```kotlin
val myConsumer =
    sqsConsumer(URL("https://sqs.eu-west-1.amazonaws.com/000000000000/my-queue")) {
        onSuccess = { message: Message -> println(message) }
        onError = { error -> println(error) }
        config = sqsConsumerConfiguration {
            waitTime = Duration.ofSeconds(10)
            concurrency = 2
            retries = 2
            maxMessages = 10
            sleepTime = Duration.ofSeconds(2)
        }
    }

```

The snippet above, creates a new consumer to consume sqs messages from the queue in
url `https://sqs.eu-west-1.amazonaws.com/000000000000/my-queue`

### onSuccess

The `onSuccess` function will be called for each message fetched after tabour fetches messages from that queue
successfully

### onError

The `onError` handler is called after the conumser fails to consume the message `config.retries` times.

### config

Each consumer can be configured independently using the `config` attribute

The configuration of the snippet above means:

- Try to consume a message 2 times
- Try to fetch 10 messages from SQS on each call
- Spawn 2 coroutines to fetch messages from the queue
- Wait 10 seconds for a message to arrive if there are no messages in the queue
- Make a call every 2 seconds

Increasing the throughput of messages is achieved by combining 2 things

- Concurrency
- Max messages

Concurrency indicates the number of coroutines that will be spawned in parallel

### Available configuration options and default values

```kotlin
/** The number of max messages to fetch. Default is 10 which is also the max numbers allowed */
var maxMessages: Int = 10

/**
 * Unit of concurrency. In combination with [maxMessages] determines the max number of messages that
 * can be received every [sleepTime]. The upper bound of messages every [sleepTime] is
 * [maxMessages] * [concurrency]
 */
var concurrency: Int = 1

/**
 * The delay between subsequent requests
 *
 * Default is 10 seconds
 */
var sleepTime: Duration = Duration.of(10L, ChronoUnit.SECONDS)

/**
 * The duration for which the call waits for a message to arrive in the queue before returning. If a
 * message is available, the call returns sooner than WaitTimeSeconds. If no messages are available
 * and the wait time expires, the call returns successfully with an empty list of messages.
 *
 * Default is 10 seconds
 */
var waitTime: Duration

/**
 * The number of attempts to receive a message if an exception occurs
 *
 * Default is 1
 */
var retries: Int = 1

/**
 * Used to dynamically enable or disable a consumer.
 *
 * Default is true, which means that the consumer will start normally by default
 */
var consumeWhile: () -> Boolean = { true }

```
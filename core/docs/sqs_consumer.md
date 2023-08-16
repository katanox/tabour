## SqsConsumer

## Create a new consumer

```kotlin
val myConsumer = sqsConsumer {
    queueUrl = URI("https://queue-url.com")
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

The snippet above, creates a new conumser to consume sqs messages from the queue in url `https://queue-url.com`

## onSuccess

The `onSuccess `function will be called after the tabour fetches successfully messages from that queue, for each message
fetched

## onError

Depending on your configuration, the `onError` function will be called if your `onSuccess` function can not finish
gracefully

Based on the snippet above, if `onSuccess` fails to consume the message 2 times then `onError` will be called

## config

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
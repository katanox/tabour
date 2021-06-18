# Tabour

Kotlin's library to make working with queues/topics much easier.

### Usage:

## Installation

First you need to get started is to add a dependency to `Tabour` library.
Then adding these to the main application of spring:
```kotlin
    @ConfigurationPropertiesScan
    @ComponentScan(basePackages = ["com.katanox.tabour"])
    class RandomApplication
```

#### Gradle/maven dependency
<table>
<thead><tr><th>Approach</th><th>Instruction</th></tr></thead>
<tr>
<td><img src="docs/maven.png" alt="Maven"/></td>
<td>
<pre>&lt;dependency&gt;
    &lt;groupId&gt;com.katanox&lt;/groupId&gt;
    &lt;artifactId&gt;tabour&lt;/artifactId&gt;
    &lt;version&gt;{version}&lt;/version&gt;
&lt;/dependency&gt;</pre>
    </td>
</tr>
</table>

## Supported Types
- SQS

## Publisher example
```kotlin
class BookingEventPublisher: EventPublisher<Booking>() {
    
    override fun getBusType(): BusType {
        return BusType.SQS
    }
}
```
Then
```kotlin
    bookingEventPublisher.publish(createBookingRequest.booking, "BUS_URL")
```

## Consumer example
In this example a protobuf message has been used.
```kotlin
class BookingEventConsumer : EventConsumer() {

    override fun consume(message: ByteArray) {
        val input = ByteArrayInputStream(message)
        val bookingBuilder = Booking.newBuilder()
        TextFormat.merge(input.reader(), bookingBuilder)
        val booking = bookingBuilder.build()
        logger.info { booking }
    }

    override fun getBusURL(): String {
        return "BUS_URL"
    }

    override fun getBusType(): BusType {
        return BusType.SQS

    }
}
```

## Configurations
<table>
<thead><tr><th>path</th><th>default value</th><th>explanation</th></tr></thead>
<tr>
<td><pre>tabour.retry-max-count</pre></td>
<td><pre>1</pre></td>
<td>Number of times that is gonna retry to publish  or consume an event</td>
</tr>
<tr>
<tr>
<td><pre>tabour.handler.thread-pool-size</pre></td>
<td><pre>3</pre></td>
<td>Size of the thread pool the the handler actors will use to consume the messages</td>
</tr>
<tr>
<td><pre>tabour.handler.queue-size</pre></td>
<td><pre>10</pre></td>
<td>Size of the internal queue that the threads will use to pull from once a message is consumed</td>
</tr>
<tr>
<td><pre>tabour.handler.thread-name-prefix</pre></td>
<td><pre>""</pre></td>
<td>Prefix of the threads that are created by the thread pool</td>
</tr>
<tr>
<td><pre>tabour.poller.poll-delay</pre></td>
<td><pre>1 Second</pre></td>
<td>Delay the poller should wait for the next poll after the previous poll has finished</td>
</tr>
<tr>
<td><pre>tabour.poller.wait-time</pre></td>
<td><pre>20 Seconds</pre></td>
<td>The duration (in seconds) for which the call waits for a message to arrive in the queue before returning. If a message is available, the call returns sooner than WaitTimeSeconds. If no messages are available and the wait time expires, the call returns successfully with an empty list of messages.</td>
</tr>
<tr>
<td><pre>tabour.poller.visibility-timeout</pre></td>
<td><pre>360 Seconds</pre></td>
<td>Visibility timeout is the time-period or duration you specify for the queue item which when is fetched and processed by the consumer is made hidden from the queue and other consumers.</td>
</tr>
<tr>
<td><pre>tabour.poller.batch-size</pre></td>
<td><pre>10</pre></td>
<td>The maximum number of messages to pull from the even bus each poll:

     *  event bus:
        - SQS allows is maximum 10</td>
</tr>
<tr>
<td><pre>tabour.poller.polling-threads</pre></td>
<td><pre>1</pre></td>
<td>The number of threads that should poll for new messages. Each of those threads will poll a batch of batchSize messages and then wait for the pollDelay interval until polling the next batch.</td>
</tr>
<tr>
<td><pre>tabour.sqs.access-key</pre></td>
<td><pre>NA</pre></td>
<td>The AWS access key.</td>
</tr>
<tr>
<td><pre>tabour.sqs.secret-key</pre></td>
<td><pre>NA</pre></td>
<td>The AWS secret key.</td>
</tr>
<tr>
<td><pre>tabour.sqs.region</pre></td>
<td><pre>NA</pre></td>
<td>The AWS region</td>
</tr>
<tr>
<td><pre>tabour.sqs.auto-startup</pre></td>
<td><pre>true</pre></td>
<td>Configures if this listening container should be automatically started.</td>
</tr>
<tr>
<td><pre>tabour.sqs.max-number-of-messages</pre></td>
<td><pre>10</pre></td>
<td>Configure the maximum number of messages that should be retrieved during one poll to the Amazon SQS system. This number must be a positive, non-zero number that has a maximum number of 10. Values higher then 10 are currently not supported by the queueing system.</td>
</tr>
<tr>
<td><pre>tabour.sqs.core-pool-size</pre></td>
<td><pre>1</pre></td>
<td>Set the ThreadPoolExecutor's core pool size, that is being used by SQS</td>
</tr>
<tr>
<td><pre>tabour.sqs.max-pool-size</pre></td>
<td><pre>Int.MAX_VALUE</pre></td>
<td>Set the ThreadPoolExecutor's maximum pool size, that is being used by SQS</td>
</tr>
<tr>
<td><pre>tabour.sqs.queue-capacity</pre></td>
<td><pre>Int.MAX_VALUE</pre></td>
<td>Set the capacity for the ThreadPoolExecutor's BlockingQueue, that is being used by SQS Any positive value will lead to a LinkedBlockingQueue instance; Any other value will lead to a SynchronousQueue instance</td>
</tr>
<tr>
<td><pre>tabour.sqs.enable-consumption</pre></td>
<td><pre>false</pre></td>
<td>Configures if this the sqs listeners should be starting</td>
</tr>
</table>
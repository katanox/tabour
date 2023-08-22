# Tabour Core

## Concept

Tabour follows a modular architecture and the core concept of it's architecture is a `Registry`

You need to register the registry to the `Tabour container` and start the `Tabour container`.
After that step, the consumers that you have configured and registered will start consuming messages from the queues
they have been configured to consume.

## Registry

A registry is a collection of `Consumers` and `Producers` for a specific messaging system. Tabour comes with a handful
of functions that create `consumers` and `producers` which you need to **attach** (`register`) to a specific registry

### Available registries

- [SqsRegistry](docs/sqs/registry.md)

## Consumer

A consumer is a class that has been configured to consume messages from a specific queue/topic depending on the
messaging system.

### Available consumers

- [SqsConsumer](docs/sqs/consumer.md)

## Producer

A producer is a class that has been configured to produce messages to a specific queue/topic depending on the
messaging system.

### Available consumers

- [SqsProducer](docs/sqs/producer.md)

### Creating a tabour instance

```kotlin
val container = tabour { numOfThreads = 2 }
```

### Registering registries to the tabour instance

```kotlin
// check docs/sqs/registry.md about the creation of a registry
container.register(registry)
```

### starting tabour

```kotlin
tabour.start()
```

# Installation

<table>
<thead><tr><th>Build tool</th><th>Instruction</th></tr></thead>
<tr>
<td><img src="../docs/maven.png" alt="Maven"/></td>
<td>
<pre>&lt;dependency&gt;
    &lt;groupId&gt;com.katanox.tabour&lt;/groupId&gt;
    &lt;artifactId&gt;core&lt;/artifactId&gt;
    &lt;version&gt;{version}&lt;/version&gt;
&lt;/dependency&gt;</pre>
</td>
</tr>
<tr>
<td><img src="../docs/gradle_groovy.png" alt="Gradle Groovy DSL"/></td>
<td>
<pre>implementation 'com.katanox.tabour:core:{version}'</pre>
</td>
</tr>
<tr>
<td><img src="../docs/gradle_kotlin.png" alt="Gradle Kotlin DSL"/></td>
<td>
<pre>implementation("com.katanox.tabour:core:{version}")</pre>
</td>
</tr>
<tr>
<td><img src="../docs/sbt.png" alt="Scala SBT"/></td>
<td>
<pre>libraryDependencies += "com.katanox.tabour" % "core" % "{version}"</pre>
</td>
</tr>
</table>

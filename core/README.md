# Tabour Core

## Concept

Tabour follows a modular architecture and the core concept of its architecture is a `Registry`.

You need to register the registry to the `Tabour container` and start the `Tabour container`.
After that step, the consumers that you have configured and registered will start consuming messages from the queues
they have been configured for.

## Registry

A registry is a collection of `Consumers` and `Producers` for a specific messaging system. Tabour comes with a handful
of functions that create `consumers` and `producers` which you need to **attach** (`register`) to a specific registry

### Available registries

- [SqsRegistry](docs/sqs/registry.md)

## Consumer

A consumer is a class that has been configured to consume messages from a specific queue/topic depending on the
messaging system.

## Producer

A producer is a class that has been configured to produce messages to a specific queue/topic depending on the
messaging system.

### Creating a tabour instance

```kotlin
val container = tabour { numOfThreads = 2 }
```

### Adding registries to the tabour instance

```kotlin
// check docs/sqs/registry.md about the creation of a registry
container.register(registry)
```

### Starting tabour

```kotlin
tabour.start()
```

# Installation

<table>
<thead><tr><th>Build tool</th><th>Instruction</th></tr></thead>
<tr>
<td>Maven</td>
<td>
<pre>&lt;dependency&gt;
    &lt;groupId&gt;com.katanox.tabour&lt;/groupId&gt;
    &lt;artifactId&gt;core&lt;/artifactId&gt;
    &lt;version&gt;{version}&lt;/version&gt;
&lt;/dependency&gt;</pre>
</td>
</tr>
<tr>
<td>Gradle Groovy DSL</td>
<td>
<pre>implementation 'com.katanox.tabour:core:{version}'</pre>
</td>
</tr>
<tr>
<td>Gradle Kotlin DSL</td>
<td>
<pre>implementation("com.katanox.tabour:core:{version}")</pre>
</td>
</tr>
<tr>
</table>

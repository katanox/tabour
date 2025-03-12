# Ktor

The ktor module allows you to use tabour in a Ktor project using KtorPlugin

The Ktor plugin allows you to register your tabour instance and makes sure that it gets started and stopped based on the application's lifecycle.

You can install it using:

```kotlin
val myTabourInstance = tabour { }

install(TabourPlugin) {
  enabled = true
  tabour = myTabourInstance
}
```

The plugin will start and stop the instance **only if** `enabled` is true.
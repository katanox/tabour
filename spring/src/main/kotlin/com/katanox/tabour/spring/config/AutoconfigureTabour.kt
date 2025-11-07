package com.katanox.tabour.spring.config

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
/**
 * Instructs Tabour to look for registries that are spring beans and start them. Use this annotation
 * on your main application class to enable the tabour scanning:
 * ```kotlin
 * @AutoconfigureTabour
 * @ComponentScan(basePackages = ["com.katanox.tabour"])
 * class MyApplication
 * ```
 */
annotation class AutoconfigureTabour

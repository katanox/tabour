package com.katanox.tabour.spring.config

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class TabourAutoConfiguration(val enable: Boolean = false)
package com.katanox.tabour.spring.config

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
/** Instructs Tabour to look for registries that are spring beans start them. */
annotation class AutoconfigureTabour

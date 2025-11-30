package io.github.mdpearce.sonicswitcher.di

import javax.inject.Qualifier

/**
 * Qualifier for IO-bound CoroutineDispatcher.
 * Used for disk I/O, network operations, and other blocking operations.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

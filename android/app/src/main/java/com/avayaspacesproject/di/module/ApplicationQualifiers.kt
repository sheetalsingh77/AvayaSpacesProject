package com.avayaspacesproject.di.module

import android.content.SharedPreferences
import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*

/**
 * Qualifier annotation used to mark injected instances of [android.content.Context]
 * where the single application context is desired.
 */
@Qualifier
@Retention(RUNTIME)
annotation class ApplicationContext

/**
 * Qualifier annotation used to mark injected instances of
 * [android.content.res.Resources] so that the application context is
 * used to get the `Resources` instance.
 */
@Qualifier
@Retention(RUNTIME)
annotation class ApplicationResources

/**
 * Qualifier annotation used to mark injected instances of [SharedPreferences]
 * where the desired instance is the default.
 */
@Qualifier
@Retention(RUNTIME)
@Target(FIELD, VALUE_PARAMETER, FUNCTION, PROPERTY_GETTER, PROPERTY_SETTER, PROPERTY)
annotation class DefaultSharedPreferences

/**
 * Qualifier annotation used to mark injected instances of [android.os.Handler]
 * where an instance for the application's main thread is desired.
 */
@Qualifier
@Retention(RUNTIME)
annotation class MainLooperHandler

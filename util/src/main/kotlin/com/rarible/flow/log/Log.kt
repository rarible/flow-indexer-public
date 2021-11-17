package com.rarible.flow.log

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.companionObject

class Log<in R : Any> : ReadOnlyProperty<R, Logger> {
    override fun getValue(thisRef: R, property: KProperty<*>): Logger
        = LoggerFactory.getLogger(getClassForLogging(thisRef.javaClass))

    private fun <T : Any> getClassForLogging(javaClass: Class<T>): Class<*> {
        return javaClass.enclosingClass?.takeIf {
            it.kotlin.companionObject?.java == javaClass
        } ?: javaClass
    }
}

@ExperimentalCoroutinesApi
suspend fun <T> logTime(label: String, f: suspend () -> T): T {
    val start = System.currentTimeMillis()
    return try {
        f()
    } finally {
        println("____ $label time: ${System.currentTimeMillis() - start}ms")
    }
}

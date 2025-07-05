package com.demich.cps.utils

import kotlin.reflect.KProperty

inline fun <T> writeOnlyProperty(crossinline set: (T) -> Unit): WriteOnlyProperty<T> =
    WriteOnlyProperty { _, _, value -> set(value) }

fun interface WriteOnlyProperty<T> {
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
}

// @Deprecated(level = DeprecationLevel.ERROR, message = "get is not allowed")
operator fun <T> WriteOnlyProperty<T>.getValue(thisRef: Any?, property: KProperty<*>): T {
    error("get in write-only property")
}

/*
operator fun WriteOnlyProperty<*>.getValue(thisRef: Any?, property: KProperty<*>): Nothing {
    error("get in write-only property")
}*/

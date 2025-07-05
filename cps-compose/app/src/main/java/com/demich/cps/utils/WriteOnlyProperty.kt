package com.demich.cps.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <T> writeOnlyProperty(crossinline set: (T) -> Unit): ReadWriteProperty<Any?, T> =
    object : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            error("get in write-only delegate")
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            set(value)
        }
    }
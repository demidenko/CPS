package com.demich.cps.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

inline fun <C, T> C.writeOnlyProperty(crossinline set: C.(T) -> Unit): ReadWriteProperty<C, T> =
    object : ReadWriteProperty<C, T> {
        override fun getValue(thisRef: C, property: KProperty<*>): T {
            error("get in write-only delegate")
        }

        override fun setValue(thisRef: C, property: KProperty<*>, value: T) {
            set(value)
        }
    }
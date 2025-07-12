package com.demich.kotlin_stdlib_boost

import java.util.EnumSet

inline fun <reified T: Enum<T>> emptyEnumSet(): EnumSet<T> =
    EnumSet.noneOf(T::class.java)

inline fun <reified T: Enum<T>> Collection<T>.toEnumSet(): EnumSet<T> =
    if (isEmpty()) emptyEnumSet()
    else EnumSet.copyOf(this)

inline fun <reified T: Enum<T>> buildEnumSet(block: (EnumSet<T>) -> Unit): EnumSet<T> =
    emptyEnumSet<T>().apply(block)
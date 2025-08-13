package com.hkapps.messagepro.utils

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long) = this.map { selector(it) }.sum()

inline fun <T> Iterable<T>.sumByInt(selector: (T) -> Int) = this.map { selector(it) }.sum()

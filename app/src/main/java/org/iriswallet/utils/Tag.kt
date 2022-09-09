package org.iriswallet.utils

val Any.TAG: String
    get() = if (!javaClass.isAnonymousClass) javaClass.simpleName else javaClass.name

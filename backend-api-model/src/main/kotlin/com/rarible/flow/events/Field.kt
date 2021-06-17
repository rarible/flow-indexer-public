package com.rarible.flow.events


data class Field<T>(
    val name: String,
    val value: T?
)

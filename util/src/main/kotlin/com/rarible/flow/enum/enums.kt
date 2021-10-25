package com.rarible.flow.enum

/**
 * Converts a enum name to enum value safely
 *
 * @return enum value, or default value (null if default is not passed)
 */
inline fun <reified T : Enum<T>> safeOf(value: String, default: T? = null): T? {
    return try {
        java.lang.Enum.valueOf(T::class.java, value)
    } catch (_: IllegalArgumentException) {
        default
    }
}

/**
 * Converts a list of enum names to enum values
 */
inline fun <reified T : Enum<T>> safeOf(vararg values: String, default: List<T> = emptyList()): List<T> {
    return safeOf(values.toList(), default)
}

/**
 * Converts a list of enum names to enum values
 */
inline fun <reified T : Enum<T>> safeOf(values: Collection<String>, default: List<T> = emptyList()): List<T> {
    return values
        .mapNotNull {
            safeOf<T>(it)
        }.ifEmpty {
            default
        }
}
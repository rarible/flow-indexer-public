package com.rarible.flow.enum

/**
 * Converts a enum name to enum value safely
 *
 * @return enum value, or default value (null if default is not passed)
 */
inline fun <reified T : Enum<T>> safeOf(value: String?, default: T? = null): T? {
    return if(value == null) {
        return default
    } else try {
        java.lang.Enum.valueOf(T::class.java, value)
    } catch (_: IllegalArgumentException) {
        default
    }
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

/**
 * Returns `true` if enum T contains an entry with the specified name.
 */
inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
    return enumValues<T>().any { it.name == name}
}

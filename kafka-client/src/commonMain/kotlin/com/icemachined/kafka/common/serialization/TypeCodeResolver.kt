/**
 * Type code resolvers
 */

package com.icemachined.kafka.common.serialization

/**
 * TypeCodeResolver
 */
interface TypeCodeResolver<T : Any> {
    fun resolve(payload: T): String
}

/**
 * ClassNameTypeCodeResolver
 */
class ClassNameTypeCodeResolver<T : Any> : TypeCodeResolver<T> {
    override fun resolve(payload: T): String = payload::class.qualifiedName!!
}

/**
 * defaultTypeCodeResolver
 *
 * @return ClassNameTypeCodeResolver
 */
fun <T : Any> defaultTypeCodeResolver() = ClassNameTypeCodeResolver<T>()

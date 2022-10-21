/**
 * Type resolvers
 */

package com.icemachined.kafka.common.serialization

import kotlin.reflect.KType

/**
 * TypeResolver
 */
interface TypeResolver {
    fun resolve(typeCode: String?, topic: String?): KType
}

/**
 * FixedTypesMapTypeResolver
 *
 * @param code2type type code to ktype map
 * @param topic2Type mab which will be used to resolve type by topic if there is no such type in code2type map
 *                    if null then exception will be thrown if code2type map doesn't contain a type
 */
class FixedTypesMapTypeResolver(
    private val code2type: Map<String, KType>,
    private val topic2Type: Map<String, KType>? = null
) : TypeResolver {
    override fun resolve(typeCode: String?, topic: String?): KType = typeCode?.let { code2type[it] } ?: topic?.let { topic2Type?.let { topic2Type[topic] } }
        ?: throw IllegalArgumentException("Can't get type for typeCode: $typeCode and topic:$topic, " +
                "only these type codes are available: ${code2type.keys}, " +
                "and default values for topics: ${topic2Type?.keys}")

    override fun toString(): String = "FixedTypesMapTypeResolver(code2type=$code2type, topic2Type=$topic2Type)"
}

/**
 * defaultTypeResolver
 *
 * @param types array of KType
 * @return [FixedTypesMapTypeResolver]
 */
fun defaultTypeResolver(
    vararg types: KType
) = FixedTypesMapTypeResolver(
    code2type = types.associateBy { it.toString() }
)

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
}

/**
 * defaultTypeResolver
 *
 * @param typesMap @see [FixedTypesMapTypeResolver.code2type]
 * @param topic2Type @see [FixedTypesMapTypeResolver.topic2Type]
 * @return [FixedTypesMapTypeResolver]
 */
fun defaultTypeResolver(
    typesMap: Map<String, KType>,
    topic2Type: Map<String, KType>? = null
) = FixedTypesMapTypeResolver(typesMap, topic2Type)

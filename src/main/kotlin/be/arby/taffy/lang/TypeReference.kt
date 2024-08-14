package be.arby.taffy.lang

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

abstract class TypeReference<T> : Comparable<TypeReference<T>> {
    val type: Type =
        (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]

    override fun compareTo(other: TypeReference<T>) = 0
}

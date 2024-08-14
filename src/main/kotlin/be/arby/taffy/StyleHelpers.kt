package be.arby.taffy

import be.arby.taffy.compute.grid.types.GridLine
import be.arby.taffy.geom.Line
import be.arby.taffy.geom.Size
import be.arby.taffy.lang.TypeReference
import be.arby.taffy.style.dimension.Dimension
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.dimension.LengthPercentageAuto
import be.arby.taffy.style.grid.*
import java.lang.reflect.ParameterizedType
import kotlin.reflect.KClass

fun minmax(min: MinTrackSizingFunction, max: MaxTrackSizingFunction): TrackSizingFunction {
    return TrackSizingFunction.from(NonRepeatedTrackSizingFunction(min, max))
}

inline fun <reified T> line(value: Int): T {
    if (T::class == Line::class) {
        return Line(GridPlacement.fromLineIndex(value), GridPlacement.fromLineIndex(value)) as T
    }
    return GridPlacement.fromLineIndex(value) as T
}

inline fun <reified T> span(value: Int): T {
    if (T::class == Line::class) {
        return Line(GridPlacement.fromSpan(value), GridPlacement.fromSpan(value)) as T
    }
    return GridPlacement.fromSpan(value) as T
}

inline fun <reified T> fr(value: Float): T {
    if (T::class == Size::class) {
        return Size.fromOne(_fr(value, _clz<T>())) as T
    }
    return _fr(value, T::class) as T
}

inline fun <reified T> auto(): T {
    if (T::class == Size::class) {
        return Size.fromOne(_auto(_clz<T>())) as T
    } else if (T::class == GenericGridPlacement::class) {
        return _auto(_clz<T>()) as T
    }
    return _auto(T::class) as T
}

inline fun <reified T> length(value: Float): T {
    if (T::class == Size::class) {
        return Size.fromOne(_length(value, _clz<T>())) as T
    }
    return _length(value, T::class) as T
}

inline fun <reified T> percent(value: Float): T {
    if (T::class == Size::class) {
        return Size.fromOne(_percent(value, _clz<T>())) as T
    }
    return _percent(value, T::class) as T
}

inline fun <reified T> _clz(): KClass<*> {
    val type: ParameterizedType = (object : TypeReference<T>() {}.type) as ParameterizedType
    return (type.actualTypeArguments[0] as Class<*>).kotlin
}

fun _fr(value: Float, type: KClass<*>): Any {
    if (type == MaxTrackSizingFunction::class) {
        return MaxTrackSizingFunction.fromFlex(value)
    } else if (type == NonRepeatedTrackSizingFunction::class) {
        return NonRepeatedTrackSizingFunction.fromFlex(value)
    } else if (type == TrackSizingFunction::class) {
        return TrackSizingFunction.fromFlex(value)
    }
    return null as Any
}

fun _length(value: Float, type: KClass<*>): Any {
    if (type == LengthPercentageAuto::class) {
        return LengthPercentageAuto.fromLength(value)
    } else if (type == LengthPercentage::class) {
        return LengthPercentage.fromLength(value)
    } else if (type == Dimension::class) {
        return Dimension.fromLength(value)
    } else if (type == MinTrackSizingFunction::class) {
        return MinTrackSizingFunction.fromLength(value)
    } else if (type == MaxTrackSizingFunction::class) {
        return MaxTrackSizingFunction.fromLength(value)
    } else if (type == NonRepeatedTrackSizingFunction::class) {
        return NonRepeatedTrackSizingFunction.fromLength(value)
    } else if (type == TrackSizingFunction::class) {
        return TrackSizingFunction.fromLength(value)
    }
    return null as Any
}

fun _percent(value: Float, type: KClass<*>): Any {
    if (type == LengthPercentageAuto::class) {
        return LengthPercentageAuto.fromPercent(value)
    } else if (type == LengthPercentage::class) {
        return LengthPercentage.fromPercent(value)
    } else if (type == Dimension::class) {
        return Dimension.fromPercent(value)
    } else if (type == MinTrackSizingFunction::class) {
        return MinTrackSizingFunction.fromPercent(value)
    } else if (type == MaxTrackSizingFunction::class) {
        return MaxTrackSizingFunction.fromPercent(value)
    } else if (type == NonRepeatedTrackSizingFunction::class) {
        return NonRepeatedTrackSizingFunction.fromPercent(value)
    }else if (type == TrackSizingFunction::class) {
        return TrackSizingFunction.fromPercent(value)
    }
    return null as Any
}

fun _auto(type: KClass<*>): Any {
    if (type == LengthPercentageAuto::class) {
        return LengthPercentageAuto.Auto
    } else if (type == Dimension::class) {
        return Dimension.Auto
    } else if (type == MinTrackSizingFunction::class) {
        return MinTrackSizingFunction.Auto
    } else if (type == MaxTrackSizingFunction::class) {
        return MaxTrackSizingFunction.Auto
    } else if (type == NonRepeatedTrackSizingFunction::class) {
        return NonRepeatedTrackSizingFunction(MinTrackSizingFunction.Auto, MaxTrackSizingFunction.Auto)
    } else if (type == GridLine::class) {
        return GenericGridPlacement.Auto<GridLine>()
    }
    return null as Any
}

fun rpt(
    repetitionKind: GridTrackRepetition,
    trackList: MutableList<NonRepeatedTrackSizingFunction>
): TrackSizingFunction {
    return TrackSizingFunction.Repeat(repetitionKind, trackList)
}

fun <T>vec(): MutableList<T> {
    return mutableListOf()
}

fun <T> vec(vararg elements: T): MutableList<T> {
    return mutableListOf(*elements)
}

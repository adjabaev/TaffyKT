package be.arby.taffy.util

import be.arby.taffy.lang.Option
import be.arby.taffy.style.dimension.LengthPercentage
import be.arby.taffy.style.dimension.LengthPercentage.Percent
import be.arby.taffy.style.dimension.LengthPercentage.Length

fun LengthPercentage.maybeResolve(context: Option<Float>): Option<Float> {
    return when (this) {
        is Length -> Option.Some(this.f)
        is Percent -> context.map { dim -> dim * this.f }
    }
}

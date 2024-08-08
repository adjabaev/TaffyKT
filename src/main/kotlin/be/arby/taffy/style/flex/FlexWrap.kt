package be.arby.taffy.style.flex

import be.arby.taffy.lang.Default

/**
 * Controls whether flex items are forced onto one line or can wrap onto multiple lines.
 *
 * Defaults to [`FlexWrap::NoWrap`]
 *
 * [Specification](https://www.w3.org/TR/css-flexbox-1/#flex-wrap-property)
 */
enum class FlexWrap {
    /**
     * Items will not wrap and stay on a single line
     */
    NO_WRAP,

    /**
     * Items will wrap according to this item's [`FlexDirection`]
     */
    WRAP,

    /**
     * Items will wrap in the opposite direction to this item's [`FlexDirection`]
     */
    WRAP_REVERSE;

    companion object: Default<FlexWrap> {
        override fun default(): FlexWrap {
            return NO_WRAP
        }
    }
}

package be.arby.taffy.style.block

/**
 * Used by block layout to implement the legacy behaviour of `<center>` and `<div align="left | right | center">`
 */
enum class TextAlign {
    /**
     * No special legacy text align behaviour.
     */
    AUTO,
    /**
     * Corresponds to `-webkit-left` or `-moz-left` in browsers
     */
    LEGACY_LEFT,
    /**
     * Corresponds to `-webkit-right` or `-moz-right` in browsers
     */
    LEGACY_RIGHT,
    /**
     * Corresponds to `-webkit-center` or `-moz-center` in browsers
     */
    LEGACY_CENTER
}

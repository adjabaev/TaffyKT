package be.arby.taffy.style

import be.arby.taffy.lang.Default

/**
 * An abstracted version of the CSS `display` property where any value other than "none" is represented by "normal"
 * See: <https://www.w3.org/TR/css-display-3/#box-generation>
 */
enum class BoxGenerationMode {
    /**
     * The node generates a box in the regular way
     */
    NORMAL,

    /**
     * The node and it's descendants generate no boxes (they are hidden)
     */
    NONE;

    companion object: Default<BoxGenerationMode> {
        /**
         * The default of BoxGenerationMode
         */
        val DEFAULT = BoxGenerationMode.NORMAL

        override fun default(): BoxGenerationMode {
            return NORMAL
        }
    }
}

package be.arby.taffy.compute.grid

/**
 * Whether it is a minimum or maximum size's space being distributed
 * This controls behaviour of the space distribution algorithm when distributing beyond limits
 * See "distributing space beyond limits" at https://www.w3.org/TR/css-grid-1/#extra-space
 */
enum class IntrinsicContributionType {
    /**
     * It's a minimum size's space being distributed
     */
    MINIMUM,

    /**
     * It's a maximum size's space being distributed
     */
    MAXIMUM
}

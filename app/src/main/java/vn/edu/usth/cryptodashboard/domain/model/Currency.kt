package vn.edu.usth.cryptodashboard.domain.model

/**
 * The displayed currency. Kept in the domain layer because repositories take it as a parameter
 * (architecture decision D18). C1 may add conversion helpers, but the two values are fixed.
 */
enum class Currency {
    USD,
    VND,
}

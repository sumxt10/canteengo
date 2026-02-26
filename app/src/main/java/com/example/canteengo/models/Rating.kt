package com.example.canteengo.models

/**
 * Represents a student's rating for a menu item.
 * A student can rate a food item once per order.
 * The ratingId is a composite of orderId_menuItemId to ensure uniqueness.
 */
data class Rating(
    val ratingId: String = "",           // Composite: {orderId}_{menuItemId}
    val studentId: String = "",
    val menuItemId: String = "",
    val orderId: String = "",
    val rating: Int = 0,                  // Rating out of 10
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val MIN_RATING = 1
        const val MAX_RATING = 10

        /**
         * Creates a unique rating ID from order and menu item
         */
        fun createRatingId(orderId: String, menuItemId: String): String {
            return "${orderId}_${menuItemId}"
        }
    }
}


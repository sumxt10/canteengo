package com.example.canteengo.models

import com.google.firebase.firestore.PropertyName

data class MenuItem(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val imageUrl: String = "",

    @get:PropertyName("isVeg")
    @set:PropertyName("isVeg")
    var isVeg: Boolean = true,

    @get:PropertyName("isAvailable")
    @set:PropertyName("isAvailable")
    var isAvailable: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),

    // Rating aggregates - stored in Firestore for efficient average calculation
    val totalRatingSum: Double = 0.0,
    val totalRatingCount: Int = 0
) {
    // Computed average rating (not stored in Firestore)
    val averageRating: Double
        get() = if (totalRatingCount > 0) totalRatingSum / totalRatingCount else 0.0
}

enum class FoodCategory(val displayName: String, val emoji: String) {
    SNACKS("Snacks", "🍿"),
    SOUTH_INDIAN("South Indian", "🍛"),
    SANDWICH("Sandwich", "🥪"),
    MAGGI("Maggi", "🍜"),
    BEVERAGES("Beverages", "☕"),
    MEALS("Meals", "🍱"),
    DESSERTS("Desserts", "🍨");

    companion object {
        fun fromString(value: String): FoodCategory? {
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }
    }
}


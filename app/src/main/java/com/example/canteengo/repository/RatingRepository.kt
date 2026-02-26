package com.example.canteengo.repository

import com.example.canteengo.models.Rating
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository for handling food ratings.
 * Uses Firestore transactions to atomically update both the rating document
 * and the menu item's aggregate rating values.
 */
class RatingRepository {

    private fun dbOrNull(): FirebaseFirestore? {
        return try {
            FirebaseApp.getInstance()
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    private fun authOrNull(): FirebaseAuth? {
        return try {
            FirebaseApp.getInstance()
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Submit a new rating or update an existing one.
     * Uses a transaction to atomically:
     * 1. Check if a rating already exists
     * 2. Update the rating document
     * 3. Update the menu item's aggregate values (totalRatingSum, totalRatingCount)
     *
     * @param orderId The order in which this item was purchased
     * @param menuItemId The menu item being rated
     * @param rating The rating value (1-10)
     * @throws Exception if the operation fails
     */
    suspend fun submitRating(orderId: String, menuItemId: String, rating: Int): Boolean {
        val db = dbOrNull() ?: throw Exception("Firebase not initialized")
        val auth = authOrNull() ?: throw Exception("Not authenticated")
        val studentId = auth.currentUser?.uid ?: throw Exception("Not logged in")

        if (rating < Rating.MIN_RATING || rating > Rating.MAX_RATING) {
            throw Exception("Rating must be between ${Rating.MIN_RATING} and ${Rating.MAX_RATING}")
        }

        val ratingId = Rating.createRatingId(orderId, menuItemId)
        val ratingDocRef = db.collection(RATINGS).document(ratingId)
        val menuItemDocRef = db.collection(MENU_ITEMS).document(menuItemId)

        return try {
            db.runTransaction { transaction ->
                // Check if rating already exists
                val existingRatingDoc = transaction.get(ratingDocRef)
                val existingRating = if (existingRatingDoc.exists()) {
                    (existingRatingDoc.getLong("rating") ?: 0L).toInt()
                } else {
                    null
                }

                // Get current menu item aggregates
                val menuItemDoc = transaction.get(menuItemDocRef)
                if (!menuItemDoc.exists()) {
                    throw Exception("Menu item not found")
                }

                val currentSum = (menuItemDoc.getDouble("totalRatingSum") ?: 0.0)
                val currentCount = (menuItemDoc.getLong("totalRatingCount") ?: 0L).toInt()

                // Calculate new aggregates
                val newSum: Double
                val newCount: Int

                if (existingRating != null) {
                    // Update existing rating: adjust sum, count stays same
                    newSum = currentSum - existingRating + rating
                    newCount = currentCount
                } else {
                    // New rating: add to sum and increment count
                    newSum = currentSum + rating
                    newCount = currentCount + 1
                }

                // Create or update rating document
                val ratingData = hashMapOf(
                    "studentId" to studentId,
                    "menuItemId" to menuItemId,
                    "orderId" to orderId,
                    "rating" to rating,
                    "updatedAt" to System.currentTimeMillis()
                )

                if (existingRating == null) {
                    ratingData["createdAt"] = System.currentTimeMillis()
                }

                transaction.set(ratingDocRef, ratingData)

                // Update menu item aggregates
                transaction.update(menuItemDocRef, mapOf(
                    "totalRatingSum" to newSum,
                    "totalRatingCount" to newCount
                ))

            }.await()
            true
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Get a student's existing rating for a specific order item.
     */
    suspend fun getExistingRating(orderId: String, menuItemId: String): Int? {
        val db = dbOrNull() ?: return null

        val ratingId = Rating.createRatingId(orderId, menuItemId)

        return try {
            val doc = db.collection(RATINGS).document(ratingId).get().await()
            if (doc.exists()) {
                (doc.getLong("rating") ?: 0L).toInt()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get all ratings submitted by the current student.
     */
    suspend fun getStudentRatings(): List<Rating> {
        val db = dbOrNull() ?: return emptyList()
        val auth = authOrNull() ?: return emptyList()
        val studentId = auth.currentUser?.uid ?: return emptyList()

        return try {
            val snapshot = db.collection(RATINGS)
                .whereEqualTo("studentId", studentId)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    Rating(
                        ratingId = doc.id,
                        studentId = doc.getString("studentId") ?: "",
                        menuItemId = doc.getString("menuItemId") ?: "",
                        orderId = doc.getString("orderId") ?: "",
                        rating = (doc.getLong("rating") ?: 0L).toInt(),
                        createdAt = (doc.getLong("createdAt") ?: 0L),
                        updatedAt = (doc.getLong("updatedAt") ?: 0L)
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Check if a student has already rated items from a specific order.
     * Returns a map of menuItemId to rating.
     */
    suspend fun getRatingsForOrder(orderId: String): Map<String, Int> {
        val db = dbOrNull() ?: return emptyMap()
        val auth = authOrNull() ?: return emptyMap()
        val studentId = auth.currentUser?.uid ?: return emptyMap()

        return try {
            val snapshot = db.collection(RATINGS)
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("orderId", orderId)
                .get()
                .await()

            snapshot.documents.associate { doc ->
                val menuItemId = doc.getString("menuItemId") ?: ""
                val rating = (doc.getLong("rating") ?: 0L).toInt()
                menuItemId to rating
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    companion object {
        private const val RATINGS = "ratings"
        private const val MENU_ITEMS = "menu_items"
    }
}


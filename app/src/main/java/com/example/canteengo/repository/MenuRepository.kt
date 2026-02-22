package com.example.canteengo.repository

import com.example.canteengo.models.FoodCategory
import com.example.canteengo.models.MenuItem
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class MenuRepository {

    private fun dbOrNull(): FirebaseFirestore? {
        return try {
            FirebaseApp.getInstance()
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getAllMenuItems(): List<MenuItem> {
        val db = dbOrNull() ?: return getSampleMenuItems()
        return try {
            val snapshot = db.collection(MENU_ITEMS).get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(MenuItem::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            getSampleMenuItems()
        }
    }

    suspend fun getMenuItemsByCategory(category: FoodCategory): List<MenuItem> {
        val db = dbOrNull() ?: return getSampleMenuItems().filter { it.category == category.name }
        return try {
            val snapshot = db.collection(MENU_ITEMS)
                .whereEqualTo("category", category.name)
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(MenuItem::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            getSampleMenuItems().filter { it.category == category.name }
        }
    }

    suspend fun addMenuItem(item: MenuItem): String {
        val db = dbOrNull() ?: error("Firebase not initialized")
        val docRef = db.collection(MENU_ITEMS).add(item).await()
        return docRef.id
    }

    suspend fun updateMenuItem(item: MenuItem) {
        val db = dbOrNull() ?: error("Firebase not initialized")
        db.collection(MENU_ITEMS).document(item.id).set(item).await()
    }

    suspend fun deleteMenuItem(itemId: String) {
        val db = dbOrNull() ?: error("Firebase not initialized")
        db.collection(MENU_ITEMS).document(itemId).delete().await()
    }

    suspend fun toggleAvailability(itemId: String, isAvailable: Boolean) {
        val db = dbOrNull() ?: error("Firebase not initialized")
        db.collection(MENU_ITEMS).document(itemId)
            .update("isAvailable", isAvailable)
            .await()
    }

    suspend fun seedMenuItems(): Boolean {
        val db = dbOrNull() ?: return false
        return try {
            // Check if menu already has items
            val existing = db.collection(MENU_ITEMS).limit(1).get().await()
            if (!existing.isEmpty) return true // Already seeded

            // Add all sample items to Firestore
            val batch = db.batch()
            getSampleMenuItems().forEach { item ->
                val docRef = db.collection(MENU_ITEMS).document()
                batch.set(docRef, item.copy(id = docRef.id))
            }
            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Sample data for demo/testing when Firebase is not set up
    private fun getSampleMenuItems(): List<MenuItem> {
        return listOf(
            // Snacks
            MenuItem("1", "Samosa", "Crispy fried pastry with spiced potato filling", 15.0, "SNACKS", "", true, true),
            MenuItem("2", "Vada Pav", "Mumbai style spicy potato fritter in bun", 20.0, "SNACKS", "", true, true),
            MenuItem("3", "Pav Bhaji", "Spiced mashed vegetables with buttered pav", 50.0, "SNACKS", "", true, true),
            MenuItem("4", "French Fries", "Crispy golden fries with seasoning", 40.0, "SNACKS", "", true, true),

            // South Indian
            MenuItem("5", "Masala Dosa", "Crispy dosa with spiced potato filling", 45.0, "SOUTH_INDIAN", "", true, true),
            MenuItem("6", "Idli Sambar", "Steamed rice cakes with sambar", 30.0, "SOUTH_INDIAN", "", true, true),
            MenuItem("7", "Medu Vada", "Crispy urad dal fritters", 25.0, "SOUTH_INDIAN", "", true, true),
            MenuItem("8", "Uttapam", "Thick pancake with vegetables", 40.0, "SOUTH_INDIAN", "", true, true),

            // Sandwich
            MenuItem("9", "Veg Sandwich", "Fresh vegetables with cheese", 35.0, "SANDWICH", "", true, true),
            MenuItem("10", "Grilled Sandwich", "Grilled with cheese and veggies", 45.0, "SANDWICH", "", true, true),
            MenuItem("11", "Club Sandwich", "Triple layer loaded sandwich", 60.0, "SANDWICH", "", true, true),

            // Maggi
            MenuItem("12", "Plain Maggi", "Classic maggi noodles", 25.0, "MAGGI", "", true, true),
            MenuItem("13", "Masala Maggi", "Spicy masala maggi", 35.0, "MAGGI", "", true, true),
            MenuItem("14", "Cheese Maggi", "Maggi loaded with cheese", 45.0, "MAGGI", "", true, true),
            MenuItem("15", "Schezwan Maggi", "Indo-Chinese style maggi", 40.0, "MAGGI", "", true, true),

            // Beverages
            MenuItem("16", "Tea", "Hot masala chai", 10.0, "BEVERAGES", "", true, true),
            MenuItem("17", "Coffee", "Hot filter coffee", 15.0, "BEVERAGES", "", true, true),
            MenuItem("18", "Cold Coffee", "Chilled coffee with ice cream", 40.0, "BEVERAGES", "", true, true),
            MenuItem("19", "Lassi", "Sweet punjabi lassi", 30.0, "BEVERAGES", "", true, true),
            MenuItem("20", "Lemon Soda", "Fresh lime soda", 20.0, "BEVERAGES", "", true, true),

            // Meals
            MenuItem("21", "Veg Thali", "Complete meal with roti, sabzi, dal, rice", 70.0, "MEALS", "", true, true),
            MenuItem("22", "Rajma Chawal", "Kidney beans curry with rice", 50.0, "MEALS", "", true, true),
            MenuItem("23", "Chole Bhature", "Spicy chickpeas with fried bread", 55.0, "MEALS", "", true, true),

            // Desserts
            MenuItem("24", "Gulab Jamun", "Sweet milk dumplings (2 pcs)", 25.0, "DESSERTS", "", true, true),
            MenuItem("25", "Ice Cream", "Vanilla/Chocolate/Strawberry", 30.0, "DESSERTS", "", true, true),
            MenuItem("26", "Kulfi", "Traditional Indian ice cream", 25.0, "DESSERTS", "", true, true),
        )
    }

    companion object {
        private const val MENU_ITEMS = "menu_items"
    }
}


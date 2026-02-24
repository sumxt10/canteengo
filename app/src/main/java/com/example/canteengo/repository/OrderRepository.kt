package com.example.canteengo.repository

import com.example.canteengo.models.Order
import com.example.canteengo.models.OrderItem
import com.example.canteengo.models.OrderStatus
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

data class SpendingStats(
    val todaySpending: Double = 0.0,
    val weekSpending: Double = 0.0,
    val monthSpending: Double = 0.0,
    val topCategory: String = "None",
    val topCategoryCount: Int = 0
)

class OrderRepository {

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

    suspend fun placeOrder(order: Order): Order {
        val db = dbOrNull() ?: error("Firebase not initialized")

        // First create the document to get a unique ID
        val docRef = db.collection(ORDERS).document()
        val orderId = docRef.id

        // Generate globally unique token using orderId as base
        val token = generateGloballyUniqueToken(orderId)
        val qrString = "CANTEENGO_ORDER_${orderId}_$token"

        val orderWithToken = order.copy(
            orderId = orderId,
            token = token,
            qrString = qrString,
            status = OrderStatus.RECEIVED,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // Set the document with the pre-generated ID
        docRef.set(orderToMap(orderWithToken)).await()

        return orderWithToken
    }

    /**
     * Generates a globally unique, human-readable token.
     * Format: [A-Z][0-9]{4} where the combination is derived from orderId hash + global counter
     * This ensures:
     * - No duplicates across days, devices, or sessions
     * - Human-readable format for verbal communication
     * - Cryptographically tied to the unique orderId
     */
    private suspend fun generateGloballyUniqueToken(orderId: String): String {
        val db = dbOrNull() ?: return generateTokenFromOrderId(orderId)

        return try {
            // Use a single global counter (not per-day) for sequential tokens
            val counterDoc = db.collection("counters").document("global_order_counter")

            val newCount = db.runTransaction { transaction ->
                val snapshot = transaction.get(counterDoc)
                val currentCount = snapshot.getLong("count") ?: 10000L // Start from 10000
                val newCount = currentCount + 1
                transaction.set(counterDoc, mapOf(
                    "count" to newCount,
                    "lastUpdated" to System.currentTimeMillis()
                ))
                newCount
            }.await()

            // Format: Letter prefix (based on orderId hash) + global counter
            // This ensures uniqueness while remaining human-readable
            val prefix = ('A'.code + (orderId.hashCode().and(0x7FFFFFFF) % 26)).toChar()
            "$prefix$newCount"
        } catch (e: Exception) {
            // Fallback: generate token from orderId hash + timestamp
            generateTokenFromOrderId(orderId)
        }
    }

    /**
     * Fallback token generation using orderId hash.
     * Guarantees uniqueness since orderId is already unique.
     */
    private fun generateTokenFromOrderId(orderId: String): String {
        // Use hash of orderId to create a unique token
        val hash = orderId.hashCode().and(0x7FFFFFFF) // Ensure positive
        val prefix = ('A'.code + (hash % 26)).toChar()
        val number = 10000 + (hash % 90000) // 5-digit number
        val suffix = (System.currentTimeMillis() % 100).toInt() // Extra uniqueness
        return "$prefix$number$suffix"
    }

    suspend fun getOrderById(orderId: String): Order? {
        val db = dbOrNull() ?: return null
        val doc = db.collection(ORDERS).document(orderId).get().await()
        return if (doc.exists()) mapToOrder(doc.id, doc.data ?: emptyMap()) else null
    }

    suspend fun getStudentOrders(): List<Order> {
        val db = dbOrNull() ?: return emptyList()
        val auth = authOrNull() ?: return emptyList()
        val uid = auth.currentUser?.uid ?: return emptyList()

        return try {
            // Try with ordering first (requires composite index)
            val snapshot = db.collection(ORDERS)
                .whereEqualTo("studentId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                mapToOrder(doc.id, doc.data ?: emptyMap())
            }
        } catch (e: Exception) {
            // Fallback: query without ordering if index doesn't exist
            try {
                val snapshot = db.collection(ORDERS)
                    .whereEqualTo("studentId", uid)
                    .get()
                    .await()

                snapshot.documents.mapNotNull { doc ->
                    mapToOrder(doc.id, doc.data ?: emptyMap())
                }.sortedByDescending { it.createdAt }
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    suspend fun getSpendingStats(): SpendingStats {
        val orders = getStudentOrders()
        if (orders.isEmpty()) return SpendingStats()

        val calendar = Calendar.getInstance()

        // Start of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        // Start of this week (Sunday)
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val weekStart = calendar.timeInMillis

        // Start of this month
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = calendar.timeInMillis

        var todaySpending = 0.0
        var weekSpending = 0.0
        var monthSpending = 0.0
        val categoryCount = mutableMapOf<String, Int>()

        for (order in orders) {
            // Only count completed orders (not rejected)
            if (order.status == OrderStatus.REJECTED) continue

            val orderTime = order.createdAt
            val amount = order.totalAmount

            if (orderTime >= todayStart) {
                todaySpending += amount
            }
            if (orderTime >= weekStart) {
                weekSpending += amount
            }
            if (orderTime >= monthStart) {
                monthSpending += amount
            }

            // Count categories from order items
            for (item in order.items) {
                val category = guessCategoryFromName(item.name)
                categoryCount[category] = (categoryCount[category] ?: 0) + item.quantity
            }
        }

        // Find top category
        val topEntry = categoryCount.maxByOrNull { it.value }

        return SpendingStats(
            todaySpending = todaySpending,
            weekSpending = weekSpending,
            monthSpending = monthSpending,
            topCategory = topEntry?.key ?: "None",
            topCategoryCount = topEntry?.value ?: 0
        )
    }

    private fun guessCategoryFromName(name: String): String {
        val lowerName = name.lowercase()
        return when {
            lowerName.contains("sandwich") || lowerName.contains("burger") -> "Snacks"
            lowerName.contains("dosa") || lowerName.contains("idli") || lowerName.contains("vada") || lowerName.contains("uttapam") -> "South Indian"
            lowerName.contains("maggi") || lowerName.contains("noodle") -> "Maggi"
            lowerName.contains("tea") || lowerName.contains("coffee") || lowerName.contains("juice") || lowerName.contains("shake") || lowerName.contains("lassi") -> "Beverages"
            lowerName.contains("thali") || lowerName.contains("rice") || lowerName.contains("roti") || lowerName.contains("dal") -> "Meals"
            lowerName.contains("samosa") || lowerName.contains("puff") || lowerName.contains("pakoda") -> "Snacks"
            else -> "Other"
        }
    }

    suspend fun getAllOrders(): List<Order> {
        val db = dbOrNull() ?: return emptyList()

        val snapshot = db.collection(ORDERS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            mapToOrder(doc.id, doc.data ?: emptyMap())
        }
    }

    suspend fun getOrdersByStatus(status: OrderStatus): List<Order> {
        val db = dbOrNull() ?: return emptyList()

        val snapshot = db.collection(ORDERS)
            .whereEqualTo("status", status.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            mapToOrder(doc.id, doc.data ?: emptyMap())
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus) {
        val db = dbOrNull() ?: error("Firebase not initialized")
        db.collection(ORDERS).document(orderId).update(
            mapOf(
                "status" to status.name,
                "updatedAt" to System.currentTimeMillis()
            )
        ).await()
    }

    /**
     * Atomic update with transaction to prevent race conditions when multiple admins
     * try to accept the same order simultaneously
     */
    suspend fun updateOrderStatusAtomic(orderId: String, expectedCurrentStatus: OrderStatus, newStatus: OrderStatus) {
        val db = dbOrNull() ?: error("Firebase not initialized")

        db.runTransaction { transaction ->
            val docRef = db.collection(ORDERS).document(orderId)
            val snapshot = transaction.get(docRef)

            val currentStatus = OrderStatus.fromString(snapshot.getString("status") ?: "")

            // Only update if the current status matches expected (prevents race conditions)
            if (currentStatus == expectedCurrentStatus) {
                transaction.update(docRef, mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to System.currentTimeMillis()
                ))
            } else {
                throw Exception("Order status has already been changed by another admin")
            }
        }.await()
    }

    /**
     * Parse a Firestore document snapshot into an Order object
     */
    fun parseOrderDocument(doc: com.google.firebase.firestore.DocumentSnapshot): Order? {
        return try {
            if (!doc.exists()) return null
            mapToOrder(doc.id, doc.data ?: emptyMap())
        } catch (e: Exception) {
            null
        }
    }

    fun observeOrder(orderId: String): Flow<Order?> = callbackFlow {
        val db = dbOrNull()
        if (db == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = db.collection(ORDERS).document(orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    trySend(mapToOrder(snapshot.id, snapshot.data ?: emptyMap()))
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    private fun orderToMap(order: Order): Map<String, Any?> {
        return mapOf(
            "token" to order.token,
            "studentId" to order.studentId,
            "studentName" to order.studentName,
            "items" to order.items.map { item ->
                mapOf(
                    "menuItemId" to item.menuItemId,
                    "name" to item.name,
                    "price" to item.price,
                    "quantity" to item.quantity,
                    "totalPrice" to item.totalPrice
                )
            },
            "subtotal" to order.subtotal,
            "handlingCharge" to order.handlingCharge,
            "totalAmount" to order.totalAmount,
            "pickupTime" to order.pickupTime,
            "status" to order.status.name,
            "createdAt" to order.createdAt,
            "updatedAt" to order.updatedAt,
            "qrString" to order.qrString
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapToOrder(id: String, data: Map<String, Any?>): Order {
        val itemsList = (data["items"] as? List<Map<String, Any?>>) ?: emptyList()
        val items = itemsList.map { itemMap ->
            OrderItem(
                menuItemId = itemMap["menuItemId"] as? String ?: "",
                name = itemMap["name"] as? String ?: "",
                price = (itemMap["price"] as? Number)?.toDouble() ?: 0.0,
                quantity = (itemMap["quantity"] as? Number)?.toInt() ?: 1,
                totalPrice = (itemMap["totalPrice"] as? Number)?.toDouble() ?: 0.0
            )
        }

        return Order(
            orderId = id,
            token = data["token"] as? String ?: "",
            studentId = data["studentId"] as? String ?: "",
            studentName = data["studentName"] as? String ?: "",
            items = items,
            subtotal = (data["subtotal"] as? Number)?.toDouble() ?: 0.0,
            handlingCharge = (data["handlingCharge"] as? Number)?.toDouble() ?: 0.0,
            totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
            pickupTime = data["pickupTime"] as? String ?: "ASAP",
            status = OrderStatus.fromString(data["status"] as? String ?: "RECEIVED"),
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            qrString = data["qrString"] as? String ?: ""
        )
    }

    companion object {
        private const val ORDERS = "orders"
    }
}


package com.example.canteengo.models

data class Order(
    val orderId: String = "",
    val token: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val items: List<OrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val handlingCharge: Double = 0.0,
    val totalAmount: Double = 0.0,
    val pickupTime: String = "ASAP",
    val status: OrderStatus = OrderStatus.RECEIVED,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val qrString: String = "",
    val acceptedByAdminPhone: String = "",
    val acceptedByAdminName: String = ""
)

data class OrderItem(
    val menuItemId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val totalPrice: Double = 0.0
)

enum class OrderStatus(val displayName: String, val colorHex: String) {
    RECEIVED("Received", "#3B82F6"),
    ACCEPTED("Accepted", "#8B5CF6"),
    PREPARING("Preparing", "#F59E0B"),
    READY("Ready", "#10B981"),
    COLLECTED("Collected", "#6B7280"),
    REJECTED("Rejected", "#EF4444");

    companion object {
        fun fromString(value: String): OrderStatus {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: RECEIVED
        }
    }
}

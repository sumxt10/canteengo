package com.example.canteengo.messaging

import com.example.canteengo.models.OrderStatus
import com.example.canteengo.repository.UserRepository
import com.example.canteengo.utils.FcmTokenManager
import com.example.canteengo.utils.OrderStatusNotifier
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CanteenGoMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val userRepository = UserRepository()
        serviceScope.launch {
            try {
                userRepository.upsertCurrentUserFcmToken(token)
            } catch (_: Exception) {
                // If this arrives before auth state is ready, sync will happen again after login/splash.
                FcmTokenManager.syncTokenForLoggedInUser(userRepository)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val data = message.data
        if (data.get("type") != "order_status") return

        val recipientUid = data.get("recipientUid")
        if (!recipientUid.isNullOrBlank() && recipientUid != currentUser.uid) return

        val orderId = data.get("orderId").orEmpty()
        if (orderId.isBlank()) return

        val status = OrderStatus.fromString(data.get("status") ?: "")
        if (status !in setOf(OrderStatus.ACCEPTED, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.REJECTED)) {
            return
        }

        val adminName = data.get("adminName")
        OrderStatusNotifier.showOrderStatusNotification(
            context = applicationContext,
            orderId = orderId,
            status = status,
            adminName = adminName
        )
    }
}


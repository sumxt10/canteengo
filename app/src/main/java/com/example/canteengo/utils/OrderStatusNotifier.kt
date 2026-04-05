package com.example.canteengo.utils

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.canteengo.R
import com.example.canteengo.activities.student.StudentOrderDetailsActivity
import com.example.canteengo.models.OrderStatus

object OrderStatusNotifier {

    private const val CHANNEL_ID = "order_updates"

    fun showOrderStatusNotification(
        context: Context,
        orderId: String,
        status: OrderStatus,
        adminName: String?
    ) {
        createChannelIfNeeded(context)

        val title = "Order Update"
        val body = when (status) {
            OrderStatus.ACCEPTED -> {
                val adminText = if (!adminName.isNullOrBlank()) " by $adminName" else ""
                "Your order $orderId has been accepted$adminText."
            }

            OrderStatus.PREPARING -> "Your order $orderId is being prepared."
            OrderStatus.READY -> "Your order $orderId is ready. Please collect it from the canteen."
            OrderStatus.REJECTED -> "Your order $orderId was rejected."
            else -> return
        }

        val tapIntent = Intent(context, StudentOrderDetailsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("orderId", orderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            orderId.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_orders)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        postNotification(context, orderId.hashCode(), notification)
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(context: Context, notificationId: Int, notification: android.app.Notification) {
        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Order Updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for order status changes"
        }

        manager.createNotificationChannel(channel)
    }
}


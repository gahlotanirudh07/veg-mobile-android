package com.freshveg.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.freshveg.app.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ORDERS = "mandi_orders_channel"
        const val CHANNEL_RATES = "mandi_rates_channel"
        const val CHANNEL_PAYMENTS = "mandi_payments_channel"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val ordersChannel = NotificationChannel(
                CHANNEL_ORDERS,
                "Orders & Deliveries",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Live order status, buyer placements, and dispatch notifications"
                enableVibration(true)
                enableLights(true)
            }

            val ratesChannel = NotificationChannel(
                CHANNEL_RATES,
                "Market Rates & Prices",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily vegetable rate card updates"
            }

            val paymentsChannel = NotificationChannel(
                CHANNEL_PAYMENTS,
                "Payments & Khata",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Payment receipts and customer ledger updates"
                enableVibration(true)
            }

            manager.createNotificationChannels(listOf(ordersChannel, ratesChannel, paymentsChannel))
        }
    }

    fun showNotification(
        title: String,
        message: String,
        channelId: String = CHANNEL_ORDERS,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            android.util.Log.w("NotificationHelper", "POST_NOTIFICATIONS permission not granted", e)
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Failed to dispatch notification", e)
        }
    }
}

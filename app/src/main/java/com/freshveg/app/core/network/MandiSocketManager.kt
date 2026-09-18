package com.freshveg.app.core.network

import android.util.Log
import com.freshveg.app.core.datastore.SessionManager
import com.freshveg.app.core.notification.NotificationHelper
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class MandiSocketEvent(
    val event: String,
    val data: JsonObject? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class MandiSocketManager @Inject constructor(
    private val sessionManager: SessionManager,
    private val notificationHelper: NotificationHelper,
    private val gson: Gson
) {
    companion object {
        private const val TAG = "MandiSocketManager"
        private const val RECONNECT_DELAY_MS = 3000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var webSocket: WebSocket? = null
    private var isExplicitDisconnect = false
    private var currentToken: String? = null

    private val _socketEvents = MutableSharedFlow<MandiSocketEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val socketEvents: SharedFlow<MandiSocketEvent> = _socketEvents.asSharedFlow()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for websocket
        .pingInterval(25, TimeUnit.SECONDS)
        .build()

    fun connect(token: String? = null) {
        if (token != null) {
            currentToken = token
        }
        isExplicitDisconnect = false

        if (webSocket != null) {
            webSocket?.close(1000, "Reconnecting")
            webSocket = null
        }

        val url = if (!currentToken.isNullOrBlank()) {
            "${ApiConfig.WS_URL}?token=$currentToken"
        } else {
            ApiConfig.WS_URL
        }

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully to $url")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val jsonObj = gson.fromJson(text, JsonObject::class.java)
                    val eventType = jsonObj.get("event")?.asString ?: return
                    val dataObj = jsonObj.getAsJsonObject("data")

                    val event = MandiSocketEvent(event = eventType, data = dataObj)
                    scope.launch {
                        _socketEvents.emit(event)
                    }

                    handlePushNotification(eventType, dataObj)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing websocket message: $text", e)
                }
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code / $reason")
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code / $reason")
                if (!isExplicitDisconnect) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.w(TAG, "WebSocket failure: ${t.message}")
                if (!isExplicitDisconnect) {
                    scheduleReconnect()
                }
            }
        })
    }

    private fun handlePushNotification(eventType: String, data: JsonObject?) {
        scope.launch {
            try {
                val isSeller = sessionManager.isSeller.firstOrNull() ?: false
                when (eventType) {
                    "ORDER_CREATED" -> {
                        // Only show "New Order Received!" alert to sellers
                        if (isSeller) {
                            val buyerName = data?.get("buyerName")?.asString ?: "A buyer"
                            val orderNum = data?.get("orderNumber")?.asString ?: ""
                            notificationHelper.showNotification(
                                title = "🛒 New Order Received!",
                                message = "New order #$orderNum received from $buyerName",
                                channelId = NotificationHelper.CHANNEL_ORDERS
                            )
                        }
                    }
                    "ORDER_UPDATED", "ORDER_FULFILLED" -> {
                        val status = data?.get("status")?.asString ?: "Updated"
                        val orderNum = data?.get("orderNumber")?.asString ?: ""
                        notificationHelper.showNotification(
                            title = "📦 Order $status",
                            message = "Order #$orderNum status updated to $status",
                            channelId = NotificationHelper.CHANNEL_ORDERS
                        )
                    }
                    "RATES_UPDATED" -> {
                        notificationHelper.showNotification(
                            title = "📈 Daily Rates Updated",
                            message = "Produce rate card has been updated with fresh market prices.",
                            channelId = NotificationHelper.CHANNEL_RATES
                        )
                    }
                    "PAYMENT_RECORDED" -> {
                        val amount = data?.get("amount")?.asString ?: ""
                        val title = if (isSeller) "💰 Payment Received" else "💰 Payment Recorded"
                        val msg = if (amount.isNotEmpty()) {
                            if (isSeller) "Payment of ₹$amount received & recorded" else "Payment of ₹$amount recorded in Khata"
                        } else {
                            if (isSeller) "New payment received" else "New payment recorded in Khata"
                        }
                        notificationHelper.showNotification(
                            title = title,
                            message = msg,
                            channelId = NotificationHelper.CHANNEL_PAYMENTS
                        )
                    }
                    "DISCOUNT_CHANGED" -> {
                        notificationHelper.showNotification(
                            title = "🏷️ Special Discount Updated",
                            message = "Your customer discount terms have been updated.",
                            channelId = NotificationHelper.CHANNEL_RATES
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build push notification for event $eventType", e)
            }
        }
    }

    private fun scheduleReconnect() {
        scope.launch {
            delay(RECONNECT_DELAY_MS)
            if (!isExplicitDisconnect) {
                Log.d(TAG, "Attempting WebSocket reconnect...")
                connect(currentToken)
            }
        }
    }

    fun disconnect() {
        isExplicitDisconnect = true
        webSocket?.close(1000, "App paused or logged out")
        webSocket = null
    }
}

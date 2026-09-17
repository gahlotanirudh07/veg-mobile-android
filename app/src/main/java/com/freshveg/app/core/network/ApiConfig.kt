package com.freshveg.app.core.network

object ApiConfig {
    // Local Dev Backend (via Android Emulator Loopback to localhost:3000):
    const val BASE_URL = "http://10.0.2.2:3000/"
    const val WS_URL = "ws://10.0.2.2:3000/ws"

    // Live Production Render Cloud (Fallback):
    // const val BASE_URL = "https://veg-app-lydh.onrender.com/"
    // const val WS_URL = "wss://veg-app-lydh.onrender.com/ws"
}

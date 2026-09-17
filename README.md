# 📱 MandiExpress — Native Android Application

> **High-Performance Wholesale Vegetable Marketplace & Mandi ERP Mobile Application**  
> Built with **100% Kotlin**, **Jetpack Compose (Material3)**, **Dagger-Hilt**, **Kotlin Coroutines & Flow**, and **OkHttp WebSockets**.

---

## 🌟 Overview

**MandiExpress Android** is a production-grade native mobile client engineered for Indian wholesale vegetable mandis, commission agents, B2B restaurant buyers, and retail merchants. It features real-time bidirectional WebSocket sync, instantaneous bilingual localization (English & Hindi), weighing scale variance detection, khata ledger management, and offline-resilient state caching.

---

## 🏗️ Technical Architecture & Specifications

### ⚙️ Core Technical Specifications
* **Language**: Kotlin `2.0.0`
* **UI Toolkit**: Jetpack Compose with Material Design 3 (`androidx.compose.material3:1.2.1`)
* **Minimum SDK**: Android 8.0 (API Level 26)
* **Target / Compile SDK**: Android 15 (API Level 35)
* **Java Compatibility**: JDK 17
* **Dependency Injection**: Dagger-Hilt `2.51.1` with KSP (`2.0.0-1.0.21`)
* **Networking & REST**: Retrofit `2.11.0` + OkHttp `4.12.0`
* **Real-time Engine**: OkHttp `WebSocket` with heartbeat monitoring & automatic exponential backoff reconnection
* **Local Persistence**: Android Jetpack DataStore Preferences
* **Vector Document Engine**: Native Android `android.graphics.pdf.PdfDocument` (A4 Vector PDF generator)
* **Typography**: Custom Poppins Font Family (`Regular`, `Medium`, `SemiBold`, `Bold`, `ExtraBold`)

---

## 🚀 Key Feature Modules

### 1. 👥 Multi-Role Portal Architecture
* **Seller Portal**: Real-time rate card updates, bulk pricing broadcasts, order fulfillment pipeline, customer credit khata,hamali/freight tally expenses, and P&L analytics.
* **Buyer Portal**: Live wholesale catalogue with tiered customer discounts, interactive basket drawer with savings badges, order tracking, and downloadable invoices.
* **Platform Operations / Admin**: Platform user directory, master produce library, and seller code management.

### 2. ⚡ Real-Time WebSocket Synchronization & Audio Chimes
* Instant event reception from Express WebSocket Hub (`ORDER_CREATED`, `ORDER_FULFILLED`, `RATES_UPDATED`, `PAYMENT_RECORDED`).
* Synthesized 3-tone acoustic chimes via Android `ToneGenerator` / `SoundPool` for audio feedback on incoming mandi orders.
* Foreground notification channels for Android 13+ (`TIRAMISU`) with explicit notification permission handling.

### 3. ⚖️ Weighing Scale Variance Detection & 4-Tier Live Bill Card
* During physical vegetable dispatch, sellers enter actual crate/bag scale weights that may differ from ordered quantities.
* Intelligent variance banner alerts sellers when supplied quantity differs from buyer's order (`Ordered 2 KG → Supplied 3 KG (+1.0 KG)`).
* **4-Tier Live Pricing Breakdown**:
  1. `Original Ordered Total`
  2. `Delivered Total (Actual Weight × Rate)`
  3. `Discount Savings (Proportional ₹ or % savings)`
  4. `Final Total to Pay`

### 4. 🇮🇳 Instant Bilingual Toggle (English 🇬🇧 & हिन्दी 🇮🇳)
* Built-in `LanguageManager` backed by DataStore Preferences.
* One-tap header toggle switching UI strings and produce names (e.g. *Tomato* ⇄ *टमाटर*, *Potato* ⇄ *आलू*, *Cutoff Time* ⇄ *ऑर्डर की समय सीमा*).

### 5. 📑 Vector A4 Invoice PDF Generator & WhatsApp Sharing
* Zero-dependency on third-party PDF cloud renderers — generates crisp, scalable vector A4 invoice sheets directly on-device using Android Canvas.
* Features seller GST details, customer ledger summary, itemized rate tables, discount line-items, and instant WhatsApp PDF dispatch.

### 6. 🛡️ Strict Zero-Trust Pricing Model
* Client payloads **NEVER transmit prices or totals**. The server strictly recalculates line items and totals against verified database rate records.

---

## 📂 Project Directory Structure

```text
veg-mobile-android/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/freshveg/app/
│   │   │   │   ├── core/
│   │   │   │   │   ├── datastore/       # Token & Language DataStore Preferences
│   │   │   │   │   ├── network/         # Retrofit API Service & WebSocket Hub
│   │   │   │   │   ├── ui/              # Theme, Colors, Typography & Animations
│   │   │   │   │   └── utils/           # PDF Generator & Mandi Translation Utils
│   │   │   │   ├── di/                  # Hilt Dependency Injection Modules
│   │   │   │   ├── features/
│   │   │   │   │   ├── admin/           # Super Admin Control Center
│   │   │   │   │   ├── auth/            # Phone & Password Authentication
│   │   │   │   │   ├── buyer/           # Buyer Catalogue, Basket & Invoices
│   │   │   │   │   └── seller/          # Rate Card, Orders, Khata & Procurement
│   │   │   │   └── navigation/          # Compose Navigation Routes & Bottom Bars
│   │   │   └── res/                     # Vector Drawables, Strings, Fonts (Poppins)
│   │   └── test/                        # 22+ Automated Unit & Automation Tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/                              # Version Catalogs (libs.versions.toml)
├── build.gradle.kts                     # Root Project Gradle Config
└── settings.gradle.kts                  # Project Settings & Repositories
```

---

## 🛠️ Build, Test & Run Guide

### Prerequisites
* **Android Studio**: Ladybug / Koala or newer
* **JDK**: OpenJDK 17
* **Android SDK**: API 35 (Android 15)

### Command-Line Execution

```bash
# Navigate to android root
cd veg-mobile-android

# 1. Compile Kotlin & Run Strict Type Check
./gradlew compileDebugKotlin

# 2. Run All Automated Unit & ViewModel Tests (All 22+ tests must pass)
./gradlew testDebugUnitTest

# 3. Build Debug APK
./gradlew assembleDebug

# 4. Install Directly to Connected Device / Emulator
./gradlew installDebug
```

---

## 🔗 Backend Environment Switching

The app connects to the Express 5 backend. To toggle between Local Development and Cloud Production, update `NetworkModule.kt`:

* **Production (Cloud Render)**: `https://veg-app-lydh.onrender.com/`
* **Local Development (Android Emulator)**: `http://10.0.2.2:3000/`
* **Local Development (Physical Device via Wi-Fi)**: `http://<YOUR_LOCAL_IP>:3000/`

---

## 📄 License
Copyright © 2026 MandiExpress. All rights reserved.

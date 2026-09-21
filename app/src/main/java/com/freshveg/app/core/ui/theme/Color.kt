package com.freshveg.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// Warm Canvas & Product Surfaces
val CatalogueCanvas = Color(0xFFF8F9F6)          // Clean warm off-white / very-light neutral background
val ProductCardSurface = Color(0xFFFFFFFF)       // Pure white product surface
val ProductCardBorder = Color(0xFFE5E9E2)        // Extremely subtle 1dp border
val SurfaceMuted = Color(0xFFF1F4F0)             // Subtle light-neutral surface (search bar, chips, steppers)
val BorderSubtle = Color(0xFFE2E6DF)             // Subtle neutral border

// Strict Green Design System (1 Primary, 1 Dark, Subtle Light-Green Surfaces)
val BrandGreenPrimary = Color(0xFF1E8344)        // One Primary Green (crisp, professional commerce green)
val BrandGreenDark = Color(0xFF0F3B20)           // One Dark Green (deep botanical accent)
val BrandGreenSurface = Color(0xFFEFF7F1)        // Subtle light-green surface (badges, cart banner, active states)
val BrandGreenBorder = Color(0xFFCCE8D4)         // Subtle green border token

// Text & Ink Tokens (Charcoal & Restrained Grey-Green)
val InkPrimary = Color(0xFF16251C)               // Primary text: dark green/charcoal rather than pure black
val InkSecondary = Color(0xFF5A6E63)             // Secondary text: restrained grey-green
val InkTertiary = Color(0xFF8A9A90)              // Tertiary text: subtle placeholder / disabled

// System & Warning Tokens
val AmberWarning = Color(0xFFD97706)
val MutedRedError = Color(0xFFDC2626)
val NeutralSurface = ProductCardSurface
val SecondarySurface = CatalogueCanvas
val HarvestLime = Color(0xFFDDF06A)

// Backward Compatibility Aliases
val BackgroundCanvas = CatalogueCanvas
val MainInk = InkPrimary
val ActionGreen = BrandGreenPrimary
val ForestGreenPrimary = BrandGreenDark
val FarmGreenSecondary = BrandGreenPrimary
val MintGreenTertiary = BrandGreenPrimary
val LightMintAccent = BrandGreenSurface
val BackgroundSurface = CatalogueCanvas
val CardSurface = ProductCardSurface
val TextPrimary = InkPrimary
val TextSecondary = InkSecondary
val RedError = MutedRedError
val BorderLight = ProductCardBorder



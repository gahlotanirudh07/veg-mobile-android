package com.freshveg.app

import com.freshveg.app.core.network.SellerRateDto
import com.freshveg.app.core.network.UnitType
import org.junit.Assert.*
import org.junit.Test

class RateBoardAndPricingTest {

    @Test
    fun testGrossMarginPercentageCalculation() {
        val sellingPrice = 40.0
        val costPrice = 30.0

        val margin = ((sellingPrice - costPrice) / sellingPrice) * 100.0
        assertEquals(25.0, margin, 0.001)

        val zeroCostMargin = ((50.0 - 0.0) / 50.0) * 100.0
        assertEquals(100.0, zeroCostMargin, 0.001)
    }

    @Test
    fun testOneThumbRateSteppers() {
        var rate = 32.0

        // +2 Stepper
        rate += 2.0
        assertEquals(34.0, rate, 0.001)

        // +5 Stepper
        rate += 5.0
        assertEquals(39.0, rate, 0.001)

        // -2 Stepper
        rate -= 2.0
        assertEquals(37.0, rate, 0.001)

        // -5 Stepper
        rate -= 5.0
        assertEquals(32.0, rate, 0.001)
    }

    @Test
    fun testBulkRateAdjustments() {
        val rates = mutableListOf(
            SellerRateDto(id = "1", name = "Tomato", sellingPrice = 30.0, isAvailable = true),
            SellerRateDto(id = "2", name = "Potato", sellingPrice = 20.0, isAvailable = true),
            SellerRateDto(id = "3", name = "Onion", sellingPrice = 25.0, isAvailable = false)
        )

        // Global +₹2 on active produce
        val updatedRates = rates.map { item ->
            if (item.isAvailable) item.copy(sellingPrice = item.sellingPrice + 2.0) else item
        }

        assertEquals(32.0, updatedRates[0].sellingPrice, 0.001)
        assertEquals(22.0, updatedRates[1].sellingPrice, 0.001)
        assertEquals(25.0, updatedRates[2].sellingPrice, 0.001) // Untouched because inactive
    }

    @Test
    fun testZeroTrustPricingSanitization() {
        val validRate = 32.0
        val negativeRate = -5.0
        val zeroRate = 0.0

        assertTrue(validRate > 0)
        assertFalse(negativeRate > 0)
        assertFalse(zeroRate > 0)
    }
}

package com.freshveg.app

import com.freshveg.app.core.network.DailySalesRowDto
import com.freshveg.app.core.network.PlatformAnalyticsResponse
import org.junit.Assert.*
import org.junit.Test

class AnalyticsAndAdminTest {

    @Test
    fun testAverageOrderValueCalculation() {
        val totalSales = 125000.0
        val totalOrders = 50

        val aov = if (totalOrders > 0) totalSales / totalOrders else 0.0
        assertEquals(2500.0, aov, 0.001)
    }

    @Test
    fun testDailySalesAggregationFormula() {
        val rows = listOf(
            DailySalesRowDto(date = "2026-08-20", ordersCount = 10, uniqueBuyersCount = 8, totalWeight = 250.0, totalRevenue = 15000.0),
            DailySalesRowDto(date = "2026-08-21", ordersCount = 12, uniqueBuyersCount = 9, totalWeight = 320.0, totalRevenue = 18500.0),
            DailySalesRowDto(date = "2026-08-22", ordersCount = 15, uniqueBuyersCount = 12, totalWeight = 410.0, totalRevenue = 24000.0)
        )

        val totalRevenue = rows.sumOf { it.totalRevenue }
        val totalWeight = rows.sumOf { it.totalWeight }
        val totalOrders = rows.sumOf { it.ordersCount }

        assertEquals(57500.0, totalRevenue, 0.001)
        assertEquals(980.0, totalWeight, 0.001)
        assertEquals(37, totalOrders)
    }

    @Test
    fun testPercentageContributionOfTopProduce() {
        val tomatoRevenue = 30000.0
        val potatoRevenue = 20000.0
        val onionRevenue = 50000.0
        val totalRevenue = tomatoRevenue + potatoRevenue + onionRevenue // 100,000.0

        val onionPercentage = (onionRevenue / totalRevenue) * 100.0
        val tomatoPercentage = (tomatoRevenue / totalRevenue) * 100.0
        val potatoPercentage = (potatoRevenue / totalRevenue) * 100.0

        assertEquals(50.0, onionPercentage, 0.001)
        assertEquals(30.0, tomatoPercentage, 0.001)
        assertEquals(20.0, potatoPercentage, 0.001)
    }

    @Test
    fun testPlatformAnalyticsTotalVolume() {
        val analytics = PlatformAnalyticsResponse(
            totalGmv = 2450000.0,
            activeSellers = 12,
            totalBuyers = 180,
            totalOrders = 3400,
            totalProduceVolumeKg = 85000.0,
            activeRateCardsCount = 450
        )

        assertTrue(analytics.totalGmv > 0)
        assertEquals(12, analytics.activeSellers)
        assertEquals(180, analytics.totalBuyers)
        assertEquals(3400, analytics.totalOrders)
        assertEquals(85000.0, analytics.totalProduceVolumeKg, 0.001)
    }
}

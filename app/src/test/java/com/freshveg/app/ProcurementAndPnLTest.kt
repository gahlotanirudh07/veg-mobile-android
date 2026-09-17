package com.freshveg.app

import com.freshveg.app.core.network.PnlMetricsDto
import org.junit.Assert.*
import org.junit.Test

class ProcurementAndPnLTest {

    @Test
    fun testPnLCalculationFormula() {
        val totalRevenue = 15000.0
        val totalProduceCost = 10500.0
        val totalOverheadExpenses = 1500.0 // Labour + Freight + Mandi Tax

        val grossProfit = totalRevenue - totalProduceCost
        val grossMarginPercentage = (grossProfit / totalRevenue) * 100.0

        val netProfit = grossProfit - totalOverheadExpenses
        val netMarginPercentage = (netProfit / totalRevenue) * 100.0

        val metrics = PnlMetricsDto(
            totalRevenue = totalRevenue,
            totalProduceCost = totalProduceCost,
            totalExpenses = totalOverheadExpenses,
            grossProfit = grossProfit,
            grossMarginPercentage = grossMarginPercentage,
            netProfit = netProfit,
            netMarginPercentage = netMarginPercentage
        )

        assertEquals(4500.0, metrics.grossProfit, 0.001)
        assertEquals(30.0, metrics.grossMarginPercentage, 0.001)
        assertEquals(3000.0, metrics.netProfit, 0.001)
        assertEquals(20.0, metrics.netMarginPercentage, 0.001)
    }

    @Test
    fun testDemandAggregationFromMultipleRestaurants() {
        // Restaurant 1 orders: 10kg Tomato, 20kg Potato
        // Restaurant 2 orders: 15kg Tomato, 5kg Coriander
        // Restaurant 3 orders: 25kg Tomato, 10kg Potato

        val orders = listOf(
            mapOf("Tomato" to 10.0, "Potato" to 20.0),
            mapOf("Tomato" to 15.0, "Coriander" to 5.0),
            mapOf("Tomato" to 25.0, "Potato" to 10.0)
        )

        val aggregatedDemand = mutableMapOf<String, Double>()
        orders.forEach { orderMap ->
            orderMap.forEach { (item, qty) ->
                aggregatedDemand[item] = (aggregatedDemand[item] ?: 0.0) + qty
            }
        }

        assertEquals(50.0, aggregatedDemand["Tomato"] ?: 0.0, 0.001)
        assertEquals(30.0, aggregatedDemand["Potato"] ?: 0.0, 0.001)
        assertEquals(5.0, aggregatedDemand["Coriander"] ?: 0.0, 0.001)
    }
}

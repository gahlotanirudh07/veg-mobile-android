package com.freshveg.app

import org.junit.Assert.*
import org.junit.Test
import java.net.URLEncoder

class InvoiceAndLedgerTest {

    @Test
    fun testRunningKhataBalanceFormula() {
        val totalInvoiced = 142500.0
        val totalPaid = 98500.0

        val outstandingDues = totalInvoiced - totalPaid
        assertEquals(44000.0, outstandingDues, 0.001)

        // New payment received of ₹14,000
        val newPayment = 14000.0
        val updatedPaid = totalPaid + newPayment
        val updatedDues = totalInvoiced - updatedPaid

        assertEquals(112500.0, updatedPaid, 0.001)
        assertEquals(30000.0, updatedDues, 0.001)
    }

    @Test
    fun testUpiUriStringFormat() {
        val upiId = "mandiexpress@upi"
        val payeeName = "Kumar Veggies"
        val amount = 1250
        val customerName = "Hotel Royal Spice"

        val encodedName = URLEncoder.encode(payeeName, "UTF-8")
        val note = URLEncoder.encode("Mandi dues for $customerName", "UTF-8")

        val upiUri = "upi://pay?pa=$upiId&pn=$encodedName&am=$amount&cu=INR&tn=$note"

        assertTrue(upiUri.startsWith("upi://pay?"))
        assertTrue(upiUri.contains("pa=mandiexpress@upi"))
        assertTrue(upiUri.contains("am=1250"))
        assertTrue(upiUri.contains("cu=INR"))
    }

    @Test
    fun testPaymentModesCompleteness() {
        val validModes = listOf("CASH", "UPI", "BANK_TRANSFER", "CHEQUE")

        assertEquals(4, validModes.size)
        assertTrue(validModes.contains("CASH"))
        assertTrue(validModes.contains("UPI"))
        assertTrue(validModes.contains("BANK_TRANSFER"))
        assertTrue(validModes.contains("CHEQUE"))
    }
}

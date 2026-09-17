package com.freshveg.app.core.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.freshveg.app.core.network.InvoiceDetailDto
import java.io.File
import java.io.FileOutputStream

object InvoicePdfGenerator {

    /**
     * Generates a professional vector A4 PDF tax invoice and saves it to the Downloads folder.
     * Automatically triggers an Intent chooser to view/print the generated PDF.
     */
    fun generateAndDownloadPdf(context: Context, invoice: InvoiceDetailDto) {
        try {
            // A4 dimensions: 595 x 842 points at 72 DPI
            val pageWidth = 595
            val pageHeight = 842
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            val leftMargin = 36f
            val rightMargin = (pageWidth - 36).toFloat()
            var yPos = 40f

            // --- 1. HEADER BRANDING ---
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 18f
            paint.color = Color.rgb(25, 118, 210) // Blue 700
            val sellerName = invoice.seller?.businessName ?: "Fresh Produce Wholesale"
            canvas.drawText(sellerName, leftMargin, yPos, paint)

            // Tax Invoice badge on top right
            paint.textSize = 11f
            paint.color = Color.rgb(25, 118, 210)
            val badgeText = "ORIGINAL TAX INVOICE"
            val badgeWidth = paint.measureText(badgeText)
            canvas.drawText(badgeText, rightMargin - badgeWidth, yPos, paint)

            yPos += 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f
            paint.color = Color.DKGRAY

            val sellerContact = invoice.seller?.primaryContactName?.let { "Prop: $it" } ?: ""
            val sellerMobile = invoice.seller?.mobile?.let { " • Phone: +91 $it" } ?: ""
            canvas.drawText(sellerContact + sellerMobile, leftMargin, yPos, paint)

            val invoiceNoText = "Invoice No: ${invoice.invoiceNumber ?: "INV"}"
            val invNoWidth = paint.measureText(invoiceNoText)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(invoiceNoText, rightMargin - invNoWidth, yPos, paint)

            yPos += 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val sellerAddr = invoice.seller?.address ?: "Mandi Wholesale Terminal"
            canvas.drawText(sellerAddr, leftMargin, yPos, paint)

            val dateText = "Date: ${formatSafeDate(invoice.invoiceDate, invoice.createdAt)}"
            val dateWidth = paint.measureText(dateText)
            canvas.drawText(dateText, rightMargin - dateWidth, yPos, paint)

            if (!invoice.seller?.gstNumber.isNullOrBlank()) {
                yPos += 12f
                canvas.drawText("GSTIN: ${invoice.seller?.gstNumber}", leftMargin, yPos, paint)
            }

            // Divider line
            yPos += 14f
            paint.color = Color.rgb(25, 118, 210)
            paint.strokeWidth = 1.5f
            canvas.drawLine(leftMargin, yPos, rightMargin, yPos, paint)

            // --- 2. BILL TO SECTION ---
            yPos += 18f
            paint.strokeWidth = 0f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 10f
            paint.color = Color.rgb(25, 118, 210)
            canvas.drawText("BILLED TO (BUYER DETAILS):", leftMargin, yPos, paint)

            yPos += 14f
            paint.color = Color.BLACK
            paint.textSize = 11f
            val buyerName = invoice.customer?.businessName ?: "Customer"
            canvas.drawText(buyerName, leftMargin, yPos, paint)

            val statusText = "Status: ${invoice.status ?: "GENERATED"}"
            val statusWidth = paint.measureText(statusText)
            paint.textSize = 10f
            paint.color = Color.rgb(46, 125, 50) // Green
            canvas.drawText(statusText, rightMargin - statusWidth, yPos, paint)

            yPos += 12f
            paint.color = Color.DKGRAY
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f
            val buyerContact = invoice.customer?.primaryContactName?.let { "Contact: $it" } ?: ""
            val buyerPhone = invoice.customer?.mobile?.let { " • Phone: +91 $it" } ?: ""
            canvas.drawText(buyerContact + buyerPhone, leftMargin, yPos, paint)

            if (!invoice.order?.orderNumber.isNullOrBlank()) {
                val ordText = "Order Ref: #${invoice.order?.orderNumber}"
                val ordWidth = paint.measureText(ordText)
                canvas.drawText(ordText, rightMargin - ordWidth, yPos, paint)
            }

            if (!invoice.customer?.address.isNullOrBlank()) {
                yPos += 12f
                canvas.drawText("Address: ${invoice.customer?.address}", leftMargin, yPos, paint)
            }

            // --- 3. ITEMS TABLE ---
            yPos += 20f
            val tableTop = yPos
            val colSr = leftMargin
            val colProduct = leftMargin + 25f
            val colOrdered = 310f
            val colDelivered = 360f
            val colUnit = 410f
            val colRate = 450f
            val colTotal = rightMargin

            // Table Header Bar Background
            paint.color = Color.rgb(240, 244, 248)
            canvas.drawRect(leftMargin, tableTop - 10f, rightMargin, tableTop + 8f, paint)

            // Table Header Labels
            paint.color = Color.rgb(30, 41, 59)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            canvas.drawText("#", colSr, yPos, paint)
            canvas.drawText("PRODUCE ITEM", colProduct, yPos, paint)
            canvas.drawText("ORD", colOrdered, yPos, paint)
            canvas.drawText("DEL", colDelivered, yPos, paint)
            canvas.drawText("UNIT", colUnit, yPos, paint)
            canvas.drawText("RATE", colRate, yPos, paint)
            val totalHdr = "TOTAL (₹)"
            canvas.drawText(totalHdr, colTotal - paint.measureText(totalHdr), yPos, paint)

            // Header line
            yPos += 12f
            paint.color = Color.rgb(203, 213, 225)
            paint.strokeWidth = 1f
            canvas.drawLine(leftMargin, yPos, rightMargin, yPos, paint)

            // Line items
            val items = invoice.items ?: emptyList()
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f

            if (items.isEmpty()) {
                yPos += 18f
                paint.color = Color.GRAY
                canvas.drawText("No produce line items billed.", colProduct, yPos, paint)
            } else {
                items.forEachIndexed { index, item ->
                    yPos += 16f
                    paint.color = Color.DKGRAY
                    canvas.drawText("${index + 1}", colSr, yPos, paint)

                    paint.color = Color.BLACK
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val prodName = item.productNameSnapshot ?: "Produce"
                    val truncatedName = if (prodName.length > 32) prodName.take(30) + "…" else prodName
                    canvas.drawText(truncatedName, colProduct, yPos, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.color = Color.DKGRAY
                    canvas.drawText("${item.orderedQuantity ?: item.deliveredQuantity}", colOrdered, yPos, paint)

                    paint.color = Color.rgb(46, 125, 50)
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("${item.deliveredQuantity}", colDelivered, yPos, paint)

                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    paint.color = Color.DKGRAY
                    canvas.drawText(item.unitTypeSnapshot ?: "KG", colUnit, yPos, paint)

                    canvas.drawText("₹${item.price.toInt()}", colRate, yPos, paint)

                    val lineTotStr = "₹${item.total.toInt()}.00"
                    val lineTotWidth = paint.measureText(lineTotStr)
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    paint.color = Color.BLACK
                    canvas.drawText(lineTotStr, colTotal - lineTotWidth, yPos, paint)

                    // Sub-divider
                    yPos += 6f
                    paint.color = Color.rgb(241, 245, 249)
                    paint.strokeWidth = 0.5f
                    canvas.drawLine(leftMargin, yPos, rightMargin, yPos, paint)
                }
            }

            // --- 4. FINANCIAL TOTALS SUMMARY ---
            yPos += 20f
            val summaryLeft = 320f
            paint.strokeWidth = 1f
            paint.color = Color.rgb(203, 213, 225)
            canvas.drawLine(summaryLeft, yPos - 6f, rightMargin, yPos - 6f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 10f
            paint.color = Color.DKGRAY
            canvas.drawText("Subtotal (Actual Price):", summaryLeft, yPos + 6f, paint)
            val subtotalStr = "₹${invoice.subtotal.toInt()}.00"
            canvas.drawText(subtotalStr, rightMargin - paint.measureText(subtotalStr), yPos + 6f, paint)

            if (invoice.discountAmount > 0) {
                yPos += 16f
                paint.color = Color.rgb(46, 125, 50)
                canvas.drawText("Discount Savings:", summaryLeft, yPos + 6f, paint)
                val discStr = "-₹${invoice.discountAmount.toInt()}.00"
                canvas.drawText(discStr, rightMargin - paint.measureText(discStr), yPos + 6f, paint)
                paint.color = Color.DKGRAY
            }

            yPos += 16f
            canvas.drawText("GST Amount:", summaryLeft, yPos + 6f, paint)
            val gstStr = "₹${invoice.gstAmount.toInt()}.00"
            canvas.drawText(gstStr, rightMargin - paint.measureText(gstStr), yPos + 6f, paint)

            yPos += 18f
            paint.color = Color.rgb(25, 118, 210)
            paint.strokeWidth = 1.5f
            canvas.drawLine(summaryLeft, yPos, rightMargin, yPos, paint)

            yPos += 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 13f
            paint.color = Color.rgb(25, 118, 210)
            canvas.drawText("GRAND TOTAL:", summaryLeft, yPos, paint)
            val grandTotStr = "₹${invoice.totalAmount.toInt()}.00"
            canvas.drawText(grandTotStr, rightMargin - paint.measureText(grandTotStr), yPos, paint)

            if (invoice.outstandingBalance > 0) {
                yPos += 16f
                paint.textSize = 10f
                paint.color = Color.rgb(211, 47, 47) // Red
                canvas.drawText("Net Outstanding Dues:", summaryLeft, yPos, paint)
                val outStr = "₹${invoice.outstandingBalance.toInt()}.00"
                canvas.drawText(outStr, rightMargin - paint.measureText(outStr), yPos, paint)
            }

            // --- 5. FOOTER & AUTHORIZED SIGNATURE ---
            val footerY = (pageHeight - 65).toFloat()
            paint.strokeWidth = 0.5f
            paint.color = Color.LTGRAY
            canvas.drawLine(leftMargin, footerY, rightMargin, footerY, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            paint.color = Color.GRAY
            canvas.drawText("This is a computer-generated tax invoice verified by MandiExpress.", leftMargin, footerY + 14f, paint)
            canvas.drawText("Subject to wholesale mandi jurisdiction.", leftMargin, footerY + 26f, paint)

            val signText = "For $sellerName"
            val signWidth = paint.measureText(signText)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.DKGRAY
            canvas.drawText(signText, rightMargin - signWidth, footerY + 14f, paint)
            val authText = "Authorized Signatory"
            val authWidth = paint.measureText(authText)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(authText, rightMargin - authWidth, footerY + 26f, paint)

            document.finishPage(page)

            // --- 6. SAVE TO DOWNLOADS ---
            val fileName = "Invoice-${invoice.invoiceNumber ?: System.currentTimeMillis()}.pdf"
            var savedUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { stream ->
                        document.writeTo(stream)
                    }
                    savedUri = uri
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { stream ->
                    document.writeTo(stream)
                }
                savedUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }

            document.close()

            Toast.makeText(context, "📥 Invoice saved to Downloads: $fileName", Toast.LENGTH_LONG).show()

            // Open intent to view/print PDF
            if (savedUri != null) {
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(savedUri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                val chooser = Intent.createChooser(viewIntent, "Open or Print Tax Invoice PDF")
                chooser.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(chooser)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to export PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.R
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.CustomerPaymentEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.preferences.BoutiqueSettings
import com.example.data.repository.SaleWithItems
import java.io.File
import java.io.FileOutputStream

object PdfInvoiceGenerator {

    private const val PAGE_WIDTH = 595 // A4 width at 72 dpi
    private const val PAGE_HEIGHT = 842 // A4 height at 72 dpi
    private const val MARGIN = 32f

    private const val COLOR_EMERALD = 0xFF0B3B2E.toInt()
    private const val COLOR_EMERALD_MID = 0xFF134C3D.toInt()
    private const val COLOR_GOLD = 0xFFD4AF37.toInt()
    private const val COLOR_IVORY = 0xFFFBF8F1.toInt()
    private const val COLOR_BEIGE = 0xFFF3ECE0.toInt()
    private const val COLOR_INK = 0xFF18211E.toInt()
    private const val COLOR_MUTED = 0xFF56635E.toInt()

    fun generateInvoicePdf(
        context: Context,
        saleWithItems: SaleWithItems,
        settings: BoutiqueSettings,
        isDuplicate: Boolean = false
    ): File {
        val sale = saleWithItems.sale
        val items = saleWithItems.items
        val doc = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        var page = doc.startPage(pageInfo)
        var canvas = page.canvas

        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)

        // Emerald Header Banner
        val headerPaint = Paint().apply { color = COLOR_EMERALD }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 118f, headerPaint)

        // Gold Bottom Rule on Header
        val goldPaint = Paint().apply { color = COLOR_GOLD }
        canvas.drawRect(0f, 114f, PAGE_WIDTH.toFloat(), 118f, goldPaint)

        // Logo Bitmap
        val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_nc_monogram)
        if (logoBitmap != null) {
            val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 72, 72, true)
            canvas.drawBitmap(scaledLogo, MARGIN, 22f, null)
            if (scaledLogo != logoBitmap) scaledLogo.recycle()
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GOLD
            textSize = 10f
            letterSpacing = 0.15f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val headerSubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
        }

        val textStartX = MARGIN + 86f
        canvas.drawText(settings.shopName.uppercase(), textStartX, 46f, titlePaint)
        canvas.drawText(settings.shopTagline.uppercase(), textStartX, 62f, taglinePaint)
        if (settings.shopAddress.isNotBlank()) {
            canvas.drawText(settings.shopAddress, textStartX, 78f, headerSubPaint)
        }
        if (settings.shopPhone.isNotBlank()) {
            canvas.drawText("Mob: ${settings.shopPhone}", textStartX, 92f, headerSubPaint)
        }

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_GOLD
            textSize = 12f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(
            if (isDuplicate) "DUPLICATE INVOICE" else "TAX / RETAIL INVOICE",
            PAGE_WIDTH - MARGIN,
            46f,
            badgeTextPaint
        )
        val invRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(sale.invoiceNumber, PAGE_WIDTH - MARGIN, 64f, invRightPaint)
        val dateRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(IndianFormatters.formatDateTime(sale.timestamp), PAGE_WIDTH - MARGIN, 80f, dateRightPaint)

        // Customer Box
        var y = 136f
        val boxPaint = Paint().apply { color = COLOR_IVORY }
        val borderPaint = Paint().apply {
            color = COLOR_GOLD
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val customerBox = RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 54f)
        canvas.drawRoundRect(customerBox, 8f, 8f, boxPaint)
        canvas.drawRoundRect(customerBox, 8f, 8f, borderPaint)

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val regularPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            textSize = 10f
        }

        canvas.drawText("BILL TO / GRAHAK", MARGIN + 12f, y + 17f, labelPaint)
        canvas.drawText(sale.customerName, MARGIN + 12f, y + 33f, valuePaint)
        if (sale.customerMobile.isNotBlank()) {
            canvas.drawText("Mobile: ${sale.customerMobile}", MARGIN + 12f, y + 46f, regularPaint)
        }

        val rightLabelPaint = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightValPaint = Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("PAYMENT METHOD", PAGE_WIDTH - MARGIN - 12f, y + 17f, rightLabelPaint)
        canvas.drawText(sale.paymentMethod.replace("_", " "), PAGE_WIDTH - MARGIN - 12f, y + 33f, rightValPaint)

        // Table Header
        y += 72f
        val tableHeaderPaint = Paint().apply { color = COLOR_EMERALD_MID }
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 24f, tableHeaderPaint)

        val thPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val thRight = Paint(thPaint).apply { textAlign = Paint.Align.RIGHT }

        canvas.drawText("#", MARGIN + 8f, y + 16f, thPaint)
        canvas.drawText("PRODUCT / VARIANT", MARGIN + 28f, y + 16f, thPaint)
        canvas.drawText("QTY", MARGIN + 300f, y + 16f, thRight)
        canvas.drawText("RATE", MARGIN + 375f, y + 16f, thRight)
        canvas.drawText("DISC", MARGIN + 440f, y + 16f, thRight)
        canvas.drawText("AMOUNT", PAGE_WIDTH - MARGIN - 8f, y + 16f, thRight)

        y += 24f
        val rowAltPaint = Paint().apply { color = COLOR_BEIGE }
        val linePaint = Paint().apply {
            color = 0xFFE0D6C4.toInt()
            strokeWidth = 0.7f
        }
        val cellRightPaint = Paint(regularPaint).apply { textAlign = Paint.Align.RIGHT }
        val cellBoldRightPaint = Paint(valuePaint).apply {
            textSize = 10f
            textAlign = Paint.Align.RIGHT
        }

        var pageNum = 1
        items.forEachIndexed { index, item ->
            if (y > PAGE_HEIGHT - 190f) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                canvas = page.canvas
                canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)
                y = MARGIN + 10f
            }

            val rowHeight = 28f
            if (index % 2 == 1) {
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + rowHeight, rowAltPaint)
            }
            canvas.drawText("${index + 1}", MARGIN + 8f, y + 13f, regularPaint)
            val title = item.productName.take(36)
            val variant = listOf(item.size, item.color).filter { it.isNotBlank() }.joinToString(" • ")
            canvas.drawText(title, MARGIN + 28f, y + 13f, valuePaint.apply { textSize = 10f })
            if (variant.isNotBlank()) {
                canvas.drawText(variant.take(38), MARGIN + 28f, y + 24f, labelPaint.apply { textSize = 8f })
            }
            canvas.drawText("${item.quantity}", MARGIN + 300f, y + 16f, cellRightPaint)
            canvas.drawText(IndianFormatters.formatCurrency(item.unitSellingPrice), MARGIN + 375f, y + 16f, cellRightPaint)
            canvas.drawText(
                if (item.itemDiscount > 0) IndianFormatters.formatCurrency(item.itemDiscount) else "-",
                MARGIN + 440f,
                y + 16f,
                cellRightPaint
            )
            canvas.drawText(IndianFormatters.formatCurrency(item.lineTotal), PAGE_WIDTH - MARGIN - 8f, y + 16f, cellBoldRightPaint)

            y += rowHeight
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        }

        // Totals Summary Box
        y += 16f
        if (y > PAGE_HEIGHT - 170f) {
            doc.finishPage(page)
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
            canvas = page.canvas
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgPaint)
            y = MARGIN
        }

        val summaryLeft = PAGE_WIDTH - MARGIN - 235f
        val summaryRight = PAGE_WIDTH - MARGIN

        fun drawSummaryRow(label: String, value: String, bold: Boolean = false, color: Int = COLOR_INK) {
            val lp = if (bold) Paint(valuePaint) else Paint(regularPaint)
            val vp = if (bold) Paint(valuePaint) else Paint(regularPaint)
            lp.color = color
            vp.color = color
            vp.textAlign = Paint.Align.RIGHT
            canvas.drawText(label, summaryLeft + 10f, y, lp)
            canvas.drawText(value, summaryRight - 10f, y, vp)
            y += 17f
        }

        drawSummaryRow("Subtotal:", IndianFormatters.formatCurrency(sale.subtotal))
        if (sale.itemDiscountTotal > 0) {
            drawSummaryRow("Item Discounts:", "-${IndianFormatters.formatCurrency(sale.itemDiscountTotal)}")
        }
        if (sale.overallDiscount > 0) {
            drawSummaryRow("Bill Discount:", "-${IndianFormatters.formatCurrency(sale.overallDiscount)}")
        }

        // Grand Total Highlight Bar
        val gtRect = RectF(summaryLeft, y - 11f, summaryRight, y + 12f)
        canvas.drawRoundRect(gtRect, 6f, 6f, Paint().apply { color = COLOR_EMERALD })
        canvas.drawText("GRAND TOTAL", summaryLeft + 10f, y + 4f, Paint(thPaint).apply { textSize = 10.5f })
        canvas.drawText(
            IndianFormatters.formatCurrency(sale.grandTotal),
            summaryRight - 10f,
            y + 4f,
            Paint(thRight).apply {
                color = COLOR_GOLD
                textSize = 11.5f
            }
        )
        y += 24f

        if (sale.advanceUsed > 0) {
            drawSummaryRow("Advance Applied:", IndianFormatters.formatCurrency(sale.advanceUsed))
        }
        drawSummaryRow("Amount Paid:", IndianFormatters.formatCurrency(sale.paidAmount), bold = true, color = COLOR_EMERALD_MID)
        if (sale.remainingBalance > 0) {
            drawSummaryRow("Remaining Balance (Due):", IndianFormatters.formatCurrency(sale.remainingBalance), bold = true, color = 0xFFB3261E.toInt())
        }
        if (sale.changeReturned > 0) {
            drawSummaryRow("Change Returned:", IndianFormatters.formatCurrency(sale.changeReturned))
        }

        if (sale.notes.isNotBlank()) {
            canvas.drawText("Notes: ${sale.notes.take(55)}", MARGIN, y, regularPaint)
        }

        // Thank You Footer
        val footerY = PAGE_HEIGHT - 58f
        canvas.drawLine(MARGIN, footerY - 12f, PAGE_WIDTH - MARGIN, footerY - 12f, Paint().apply {
            color = COLOR_GOLD
            strokeWidth = 1.2f
        })
        val footerTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_EMERALD
            textSize = 12f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val footerSub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_MUTED
            textSize = 9.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Shukriya! Dobara Tashreef Layein — ${settings.shopName}",
            PAGE_WIDTH / 2f,
            footerY + 6f,
            footerTitle
        )
        canvas.drawText(
            "${settings.shopTagline} | Generated offline by NAQSH COLLECTION",
            PAGE_WIDTH / 2f,
            footerY + 22f,
            footerSub
        )

        doc.finishPage(page)

        val outDir = File(context.filesDir, "invoices").apply { mkdirs() }
        val file = File(outDir, "Invoice_${sale.invoiceNumber}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun generateCustomerStatementPdf(
        context: Context,
        customer: CustomerEntity,
        sales: List<SaleEntity>,
        payments: List<CustomerPaymentEntity>,
        settings: BoutiqueSettings
    ): File {
        val headers = listOf("Date", "Ref / Invoice", "Type", "Method", "Amount")
        val rows = mutableListOf<List<String>>()
        sales.forEach { s ->
            rows.add(
                listOf(
                    IndianFormatters.formatDate(s.timestamp),
                    s.invoiceNumber,
                    "Sale (Due: ${IndianFormatters.formatCurrency(s.remainingBalance)})",
                    s.paymentMethod,
                    IndianFormatters.formatCurrency(s.grandTotal)
                )
            )
        }
        payments.forEach { p ->
            rows.add(
                listOf(
                    IndianFormatters.formatDate(p.timestamp),
                    p.invoiceNumber ?: "-",
                    p.type,
                    p.paymentMethod,
                    IndianFormatters.formatCurrency(p.amount)
                )
            )
        }
        val summaryLines = listOf(
            "Customer: ${customer.name} (${customer.mobile})",
            "Address: ${customer.address.ifBlank { "-" }}",
            "Total Purchases: ${IndianFormatters.formatCurrency(customer.totalPurchases)}  |  Total Payments: ${IndianFormatters.formatCurrency(customer.totalPayments)}",
            "Outstanding Due: ${IndianFormatters.formatCurrency(customer.outstandingBalance)}  |  Advance Balance: ${IndianFormatters.formatCurrency(customer.advanceBalance)}"
        )
        return generateTabularReportPdf(
            context = context,
            fileName = "Statement_${customer.name.replace(" ", "_")}_${IndianFormatters.formatFileTimestamp()}.pdf",
            reportTitle = "CUSTOMER LEDGER STATEMENT",
            summaryLines = summaryLines,
            headers = headers,
            rows = rows,
            settings = settings
        )
    }

    fun generateTabularReportPdf(
        context: Context,
        fileName: String,
        reportTitle: String,
        summaryLines: List<String>,
        headers: List<String>,
        rows: List<List<String>>,
        settings: BoutiqueSettings
    ): File {
        val doc = PdfDocument()
        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var canvas = page.canvas

        fun drawHeader() {
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), Paint().apply { color = Color.WHITE })
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 74f, Paint().apply { color = COLOR_EMERALD })
            canvas.drawRect(0f, 70f, PAGE_WIDTH.toFloat(), 74f, Paint().apply { color = COLOR_GOLD })

            canvas.drawText(
                settings.shopName,
                MARGIN,
                32f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 18f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                }
            )
            canvas.drawText(
                reportTitle,
                MARGIN,
                52f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_GOLD
                    textSize = 11f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
            )
            canvas.drawText(
                IndianFormatters.formatDateTime(System.currentTimeMillis()),
                PAGE_WIDTH - MARGIN,
                52f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 9f
                    textAlign = Paint.Align.RIGHT
                }
            )
        }

        drawHeader()
        var y = 94f
        val summaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            textSize = 10f
        }
        summaryLines.forEach { line ->
            canvas.drawText(line, MARGIN, y, summaryPaint)
            y += 15f
        }
        y += 8f

        val usableWidth = PAGE_WIDTH - 2 * MARGIN
        val colCount = headers.size.coerceAtLeast(1)
        val colWidth = usableWidth / colCount

        fun drawTableHeaders() {
            canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 22f, Paint().apply { color = COLOR_EMERALD_MID })
            val thP = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 9f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            headers.forEachIndexed { i, h ->
                canvas.drawText(h.take(18), MARGIN + i * colWidth + 6f, y + 15f, thP)
            }
            y += 22f
        }

        drawTableHeaders()
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_INK
            textSize = 8.8f
        }
        val rowBg = Paint().apply { color = COLOR_BEIGE }
        val linePaint = Paint().apply { color = 0xFFE0D6C4.toInt(); strokeWidth = 0.6f }

        rows.forEachIndexed { idx, row ->
            if (y > PAGE_HEIGHT - 55f) {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
                canvas = page.canvas
                drawHeader()
                y = 92f
                drawTableHeaders()
            }
            if (idx % 2 == 1) {
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20f, rowBg)
            }
            row.forEachIndexed { colIdx, cell ->
                if (colIdx < colCount) {
                    canvas.drawText(cell.take(22), MARGIN + colIdx * colWidth + 6f, y + 14f, cellPaint)
                }
            }
            y += 20f
            canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        }

        doc.finishPage(page)
        val dir = File(context.filesDir, "reports").apply { mkdirs() }
        val file = File(dir, fileName)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}

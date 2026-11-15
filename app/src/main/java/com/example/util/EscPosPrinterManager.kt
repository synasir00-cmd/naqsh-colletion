package com.example.util

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.R
import com.example.data.preferences.BoutiqueSettings
import com.example.data.repository.SaleWithItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

data class BluetoothPrinterDevice(
    val name: String,
    val address: String,
    val isPaired: Boolean,
    val deviceClassMajor: Int
)

data class PrinterConnectionState(
    val bluetoothSupported: Boolean = true,
    val bluetoothEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val isPrinting: Boolean = false,
    val connectedName: String? = null,
    val connectedAddress: String? = null,
    val pairedDevices: List<BluetoothPrinterDevice> = emptyList(),
    val discoveredDevices: List<BluetoothPrinterDevice> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class EscPosPrinterManager(private val context: Context) {

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var receiverRegistered = false

    private val _state = MutableStateFlow(
        PrinterConnectionState(
            bluetoothSupported = bluetoothAdapter != null,
            bluetoothEnabled = bluetoothAdapter?.isEnabled == true
        )
    )
    val state: StateFlow<PrinterConnectionState> = _state.asStateFlow()

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null) {
                        val name = try { device.name ?: "Unknown Device" } catch (_: SecurityException) { "Unknown Device" }
                        val addr = device.address
                        val majorClass = try { device.bluetoothClass?.majorDeviceClass ?: 0 } catch (_: SecurityException) { 0 }
                        val entry = BluetoothPrinterDevice(
                            name = name,
                            address = addr,
                            isPaired = try { device.bondState == BluetoothDevice.BOND_BONDED } catch (_: SecurityException) { false },
                            deviceClassMajor = majorClass
                        )
                        _state.update { curr ->
                            if (curr.discoveredDevices.any { it.address == addr }) curr
                            else curr.copy(discoveredDevices = curr.discoveredDevices + entry)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _state.update { it.copy(isScanning = false) }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _state.update { it.copy(bluetoothSupported = false, errorMessage = "Bluetooth is not supported on this phone") }
            return
        }
        val enabled = adapter.isEnabled
        if (!enabled) {
            _state.update {
                it.copy(
                    bluetoothSupported = true,
                    bluetoothEnabled = false,
                    pairedDevices = emptyList(),
                    errorMessage = "Please turn ON Bluetooth to view paired printers"
                )
            }
            return
        }
        val paired = try {
            adapter.bondedDevices.map { d ->
                BluetoothPrinterDevice(
                    name = d.name ?: "Bluetooth Device",
                    address = d.address,
                    isPaired = true,
                    deviceClassMajor = d.bluetoothClass?.majorDeviceClass ?: 0
                )
            }.sortedBy { it.name }
        } catch (se: SecurityException) {
            _state.update { it.copy(errorMessage = "Bluetooth permission required to list paired devices") }
            return
        }
        _state.update {
            it.copy(
                bluetoothSupported = true,
                bluetoothEnabled = true,
                pairedDevices = paired,
                errorMessage = null
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) {
            _state.update { it.copy(bluetoothEnabled = false, errorMessage = "Turn ON Bluetooth first") }
            return
        }
        refreshPairedDevices()
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(discoveryReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                context.registerReceiver(discoveryReceiver, filter)
            }
            receiverRegistered = true
        }
        try {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            val started = adapter.startDiscovery()
            _state.update {
                it.copy(
                    isScanning = started,
                    discoveredDevices = emptyList(),
                    errorMessage = if (!started) "Could not start Bluetooth scan" else null
                )
            }
        } catch (se: SecurityException) {
            _state.update { it.copy(isScanning = false, errorMessage = "Bluetooth Scan permission denied") }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (_: SecurityException) {
        }
        _state.update { it.copy(isScanning = false) }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(address: String, displayName: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter
            ?: return@withContext Result.failure(IllegalStateException("Bluetooth hardware unavailable"))
        if (!adapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Bluetooth is turned OFF"))
        }

        _state.update { it.copy(isConnecting = true, errorMessage = null, statusMessage = "Connecting...") }
        disconnectInternal()

        return@withContext try {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            val device = adapter.getRemoteDevice(address)
            val btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            btSocket.connect()
            val out = btSocket.outputStream
            socket = btSocket
            outputStream = out
            val resolvedName = displayName ?: try { device.name ?: address } catch (_: SecurityException) { address }
            _state.update {
                it.copy(
                    isConnecting = false,
                    connectedName = resolvedName,
                    connectedAddress = address,
                    statusMessage = "Connected to $resolvedName",
                    errorMessage = null
                )
            }
            Result.success(Unit)
        } catch (e: IOException) {
            disconnectInternal()
            val msg = "Printer connection failed. Verify printer is ON, paired, and supports ESC/POS SPP (${e.message ?: "IO error"})."
            _state.update { it.copy(isConnecting = false, statusMessage = null, errorMessage = msg) }
            Result.failure(IllegalStateException(msg))
        } catch (se: SecurityException) {
            disconnectInternal()
            val msg = "Bluetooth Connect permission required."
            _state.update { it.copy(isConnecting = false, statusMessage = null, errorMessage = msg) }
            Result.failure(se)
        }
    }

    fun disconnect() {
        disconnectInternal()
        _state.update {
            it.copy(
                connectedName = null,
                connectedAddress = null,
                statusMessage = "Printer disconnected"
            )
        }
    }

    private fun disconnectInternal() {
        try { outputStream?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        outputStream = null
        socket = null
    }

    fun release() {
        stopDiscovery()
        if (receiverRegistered) {
            try { context.unregisterReceiver(discoveryReceiver) } catch (_: Exception) {}
            receiverRegistered = false
        }
        disconnectInternal()
    }

    suspend fun printTestReceipt(settings: BoutiqueSettings): Result<Unit> {
        val paperWidthMm = settings.paperWidthMm
        return if (settings.bitmapThermalMode) {
            val bitmap = renderTestReceiptBitmap(settings, paperWidthMm)
            sendRasterBitmapReceipt(bitmap)
        } else {
            val bytes = buildTextTestEscPos(settings, paperWidthMm)
            sendRawBytes(bytes)
        }
    }

    suspend fun printInvoice(
        saleWithItems: SaleWithItems,
        settings: BoutiqueSettings,
        isDuplicate: Boolean
    ): Result<Unit> {
        val paperWidthMm = settings.paperWidthMm
        return if (settings.bitmapThermalMode) {
            val bitmap = renderInvoiceReceiptBitmap(saleWithItems, settings, paperWidthMm, isDuplicate)
            sendRasterBitmapReceipt(bitmap)
        } else {
            val bytes = buildTextInvoiceEscPos(saleWithItems, settings, paperWidthMm, isDuplicate)
            sendRawBytes(bytes)
        }
    }

    private suspend fun sendRawBytes(bytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        val out = outputStream
        val sock = socket
        if (out == null || sock == null || !sock.isConnected) {
            val err = "No thermal printer connected. Connect a paired ESC/POS printer first."
            _state.update { it.copy(errorMessage = err) }
            return@withContext Result.failure(IllegalStateException(err))
        }
        _state.update { it.copy(isPrinting = true, errorMessage = null, statusMessage = "Printing...") }
        return@withContext try {
            // Write in chunks to prevent Bluetooth buffer overflow on budget thermal printers
            val chunkSize = 1024
            var offset = 0
            while (offset < bytes.size) {
                val len = minOf(chunkSize, bytes.size - offset)
                out.write(bytes, offset, len)
                out.flush()
                offset += len
                Thread.sleep(25)
            }
            _state.update { it.copy(isPrinting = false, statusMessage = "Print sent successfully", errorMessage = null) }
            Result.success(Unit)
        } catch (e: Exception) {
            disconnectInternal()
            val err = "Print failed (${e.message ?: "connection lost"}). Check paper & printer battery."
            _state.update {
                it.copy(
                    isPrinting = false,
                    connectedName = null,
                    connectedAddress = null,
                    statusMessage = null,
                    errorMessage = err
                )
            }
            Result.failure(IllegalStateException(err))
        }
    }

    private suspend fun sendRasterBitmapReceipt(bitmap: Bitmap): Result<Unit> {
        val baos = ByteArrayOutputStream()
        baos.write(byteArrayOf(0x1B, 0x40)) // ESC @ Init
        baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center

        // Print in vertical slices of 240px so printer buffer never overflows
        val sliceHeight = 240
        var y = 0
        while (y < bitmap.height) {
            val h = minOf(sliceHeight, bitmap.height - y)
            val slice = Bitmap.createBitmap(bitmap, 0, y, bitmap.width, h)
            baos.write(bitmapToEscPosRaster(slice))
            slice.recycle()
            y += h
        }
        bitmap.recycle()

        baos.write(byteArrayOf(0x1B, 0x64, 0x04)) // Feed 4 lines
        baos.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // Partial cut
        return sendRawBytes(baos.toByteArray())
    }

    private fun buildTextTestEscPos(settings: BoutiqueSettings, widthMm: Int): ByteArray {
        val cols = if (widthMm >= 80) 48 else 32
        val baos = ByteArrayOutputStream()
        baos.write(byteArrayOf(0x1B, 0x40)) // Init
        baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center

        val logo = loadMonochromeLogo(if (widthMm >= 80) 140 else 110)
        if (logo != null) {
            baos.write(bitmapToEscPosRaster(logo))
            logo.recycle()
        }

        baos.write(byteArrayOf(0x1B, 0x45, 0x01)) // Bold
        baos.write("${settings.shopName}\n".toByteArray(Charsets.US_ASCII))
        baos.write(byteArrayOf(0x1B, 0x45, 0x00))
        baos.write("${settings.shopTagline}\n".toByteArray(Charsets.US_ASCII))
        baos.write(("=".repeat(cols) + "\n").toByteArray(Charsets.US_ASCII))
        baos.write("PRINTER TEST RECEIPT\n".toByteArray(Charsets.US_ASCII))
        baos.write("Paper Width: ${widthMm}mm ($cols cols)\n".toByteArray(Charsets.US_ASCII))
        baos.write(("Date: ${IndianFormatters.formatDateTime(System.currentTimeMillis())}\n").toByteArray(Charsets.US_ASCII))
        baos.write("Status: ESC/POS Ready\n".toByteArray(Charsets.US_ASCII))
        baos.write(("=".repeat(cols) + "\n").toByteArray(Charsets.US_ASCII))
        baos.write("Shukriya! NAQSH COLLECTION\n\n\n".toByteArray(Charsets.US_ASCII))
        baos.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        return baos.toByteArray()
    }

    private fun buildTextInvoiceEscPos(
        saleWithItems: SaleWithItems,
        settings: BoutiqueSettings,
        widthMm: Int,
        isDuplicate: Boolean
    ): ByteArray {
        val cols = if (widthMm >= 80) 48 else 32
        val sale = saleWithItems.sale
        val baos = ByteArrayOutputStream()
        baos.write(byteArrayOf(0x1B, 0x40)) // Init
        baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center

        val logo = loadMonochromeLogo(if (widthMm >= 80) 128 else 96)
        if (logo != null) {
            baos.write(bitmapToEscPosRaster(logo))
            logo.recycle()
        }

        fun writeAscii(text: String) {
            val clean = text.replace("₹", "Rs.")
            baos.write(clean.toByteArray(Charsets.US_ASCII))
        }

        fun formatRow(left: String, right: String): String {
            val l = left.take(cols - right.length - 1)
            val spaces = (cols - l.length - right.length).coerceAtLeast(1)
            return l + " ".repeat(spaces) + right + "\n"
        }

        baos.write(byteArrayOf(0x1B, 0x45, 0x01))
        writeAscii("${settings.shopName}\n")
        baos.write(byteArrayOf(0x1B, 0x45, 0x00))
        writeAscii("${settings.shopTagline}\n")
        if (settings.shopPhone.isNotBlank()) writeAscii("Mob: ${settings.shopPhone}\n")
        if (settings.shopAddress.isNotBlank()) writeAscii("${settings.shopAddress}\n")
        if (isDuplicate) writeAscii("*** DUPLICATE INVOICE ***\n")
        writeAscii("-".repeat(cols) + "\n")

        baos.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left
        writeAscii("Inv: ${sale.invoiceNumber}\n")
        writeAscii("Date: ${IndianFormatters.formatDateTime(sale.timestamp)}\n")
        writeAscii("Cust: ${sale.customerName}\n")
        if (sale.customerMobile.isNotBlank()) writeAscii("Mob: ${sale.customerMobile}\n")
        writeAscii("-".repeat(cols) + "\n")

        saleWithItems.items.forEach { item ->
            val variant = listOf(item.size, item.color).filter { it.isNotBlank() }.joinToString("/")
            val title = if (variant.isNotBlank()) "${item.productName} ($variant)" else item.productName
            writeAscii(title.take(cols) + "\n")
            val left = " ${item.quantity} x ${IndianFormatters.formatCurrencyPlain(item.unitSellingPrice)}" +
                if (item.itemDiscount > 0) " -${IndianFormatters.formatCurrencyPlain(item.itemDiscount)}" else ""
            val right = IndianFormatters.formatCurrencyPlain(item.lineTotal)
            writeAscii(formatRow(left, right))
        }

        writeAscii("-".repeat(cols) + "\n")
        writeAscii(formatRow("Subtotal:", IndianFormatters.formatCurrencyPlain(sale.subtotal)))
        if (sale.itemDiscountTotal > 0) {
            writeAscii(formatRow("Item Disc:", "-${IndianFormatters.formatCurrencyPlain(sale.itemDiscountTotal)}"))
        }
        if (sale.overallDiscount > 0) {
            writeAscii(formatRow("Bill Disc:", "-${IndianFormatters.formatCurrencyPlain(sale.overallDiscount)}"))
        }
        baos.write(byteArrayOf(0x1B, 0x45, 0x01))
        writeAscii(formatRow("GRAND TOTAL:", IndianFormatters.formatCurrencyPlain(sale.grandTotal)))
        baos.write(byteArrayOf(0x1B, 0x45, 0x00))
        if (sale.advanceUsed > 0) {
            writeAscii(formatRow("Advance Used:", IndianFormatters.formatCurrencyPlain(sale.advanceUsed)))
        }
        writeAscii(formatRow("Paid (${sale.paymentMethod}):", IndianFormatters.formatCurrencyPlain(sale.paidAmount)))
        if (sale.remainingBalance > 0) {
            writeAscii(formatRow("Due Balance:", IndianFormatters.formatCurrencyPlain(sale.remainingBalance)))
        }
        if (sale.changeReturned > 0) {
            writeAscii(formatRow("Change:", IndianFormatters.formatCurrencyPlain(sale.changeReturned)))
        }

        baos.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
        writeAscii("-".repeat(cols) + "\n")
        writeAscii("Shukriya! Dobara Tashreef Layein\n\n\n")
        baos.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
        return baos.toByteArray()
    }

    private fun renderTestReceiptBitmap(settings: BoutiqueSettings, widthMm: Int): Bitmap {
        val widthPx = if (widthMm >= 80) 576 else 384
        val lines = listOf(
            settings.shopName,
            settings.shopTagline,
            "--------------------------------",
            "TEST RECEIPT (${widthMm}mm)",
            "Date: ${IndianFormatters.formatDateTime(System.currentTimeMillis())}",
            "Hindi Check: नक्श कलेक्शन",
            "Urdu Check: نقش کلیکشن",
            "Currency: ${IndianFormatters.formatCurrency(100000.00)}",
            "--------------------------------",
            "Shukriya! Printer Ready."
        )
        return renderLinesWithLogoToBitmap(widthPx, lines, centeredAll = true)
    }

    private fun renderInvoiceReceiptBitmap(
        saleWithItems: SaleWithItems,
        settings: BoutiqueSettings,
        widthMm: Int,
        isDuplicate: Boolean
    ): Bitmap {
        val widthPx = if (widthMm >= 80) 576 else 384
        val sale = saleWithItems.sale
        val body = StringBuilder()
        if (isDuplicate) body.append("*** DUPLICATE INVOICE ***\n")
        body.append("Inv: ${sale.invoiceNumber}\n")
        body.append("Date: ${IndianFormatters.formatDateTime(sale.timestamp)}\n")
        body.append("Customer: ${sale.customerName}\n")
        if (sale.customerMobile.isNotBlank()) body.append("Mob: ${sale.customerMobile}\n")
        body.append("--------------------------------\n")
        saleWithItems.items.forEach { item ->
            val variant = listOf(item.size, item.color).filter { it.isNotBlank() }.joinToString("/")
            val title = if (variant.isNotBlank()) "${item.productName} ($variant)" else item.productName
            body.append("$title\n")
            body.append("  ${item.quantity} x ${IndianFormatters.formatCurrency(item.unitSellingPrice)}")
            if (item.itemDiscount > 0) body.append(" -${IndianFormatters.formatCurrency(item.itemDiscount)}")
            body.append(" = ${IndianFormatters.formatCurrency(item.lineTotal)}\n")
        }
        body.append("--------------------------------\n")
        body.append("Subtotal: ${IndianFormatters.formatCurrency(sale.subtotal)}\n")
        if (sale.itemDiscountTotal > 0) body.append("Item Disc: -${IndianFormatters.formatCurrency(sale.itemDiscountTotal)}\n")
        if (sale.overallDiscount > 0) body.append("Bill Disc: -${IndianFormatters.formatCurrency(sale.overallDiscount)}\n")
        body.append("GRAND TOTAL: ${IndianFormatters.formatCurrency(sale.grandTotal)}\n")
        if (sale.advanceUsed > 0) body.append("Advance Used: ${IndianFormatters.formatCurrency(sale.advanceUsed)}\n")
        body.append("Paid (${sale.paymentMethod}): ${IndianFormatters.formatCurrency(sale.paidAmount)}\n")
        if (sale.remainingBalance > 0) body.append("Due Balance: ${IndianFormatters.formatCurrency(sale.remainingBalance)}\n")
        if (sale.changeReturned > 0) body.append("Change Returned: ${IndianFormatters.formatCurrency(sale.changeReturned)}\n")
        body.append("--------------------------------\n")
        body.append("Shukriya! Dobara Tashreef Layein\n")

        val headerLines = buildList {
            add(settings.shopName)
            add(settings.shopTagline)
            if (settings.shopPhone.isNotBlank()) add("Mob: ${settings.shopPhone}")
            if (settings.shopAddress.isNotBlank()) add(settings.shopAddress)
        }
        return renderReceiptLayoutBitmap(widthPx, headerLines, body.toString())
    }

    private fun renderLinesWithLogoToBitmap(widthPx: Int, lines: List<String>, centeredAll: Boolean): Bitmap {
        return renderReceiptLayoutBitmap(widthPx, lines.take(2), lines.drop(2).joinToString("\n"), centeredAll)
    }

    private fun renderReceiptLayoutBitmap(
        widthPx: Int,
        headerLines: List<String>,
        bodyText: String,
        centeredBody: Boolean = false
    ): Bitmap {
        val logoSize = if (widthPx >= 576) 120 else 92
        val logo = loadMonochromeLogo(logoSize)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = if (widthPx >= 576) 30f else 23f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = if (widthPx >= 576) 20f else 16f
            textAlign = Paint.Align.CENTER
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = if (widthPx >= 576) 22f else 17.5f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val padding = 10
        val contentWidth = widthPx - padding * 2
        val alignment = if (centeredBody) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
        val bodyLayout = StaticLayout.Builder.obtain(bodyText, 0, bodyText.length, bodyPaint, contentWidth)
            .setAlignment(alignment)
            .setLineSpacing(4f, 1.0f)
            .build()

        val logoHeight = if (logo != null) logo.height + 12 else 0
        val headerHeight = headerLines.size * (if (widthPx >= 576) 32 else 25) + 12
        val totalHeight = padding * 2 + logoHeight + headerHeight + bodyLayout.height + 20

        val bmp = Bitmap.createBitmap(widthPx, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        var y = padding.toFloat()
        if (logo != null) {
            canvas.drawBitmap(logo, (widthPx - logo.width) / 2f, y, null)
            y += logo.height + 10f
            logo.recycle()
        }

        headerLines.forEachIndexed { idx, line ->
            val p = if (idx == 0) titlePaint else subPaint
            y += if (idx == 0) 28f else 22f
            canvas.drawText(line, widthPx / 2f, y, p)
        }
        y += 14f

        canvas.save()
        canvas.translate(padding.toFloat(), y)
        bodyLayout.draw(canvas)
        canvas.restore()

        return bmp
    }

    private fun loadMonochromeLogo(targetSize: Int): Bitmap? {
        val raw = BitmapFactory.decodeResource(context.resources, R.drawable.img_nc_monogram) ?: return null
        val scaled = Bitmap.createScaledBitmap(raw, targetSize, targetSize, true)
        if (scaled != raw) raw.recycle()
        // Invert dark emerald background -> white paper, gold monogram -> black thermal ink
        val out = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        for (py in 0 until targetSize) {
            for (px in 0 until targetSize) {
                val pixel = scaled.getPixel(px, py)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                // Gold pixels have high red/green brightness relative to emerald dark green
                val isGoldInk = r > 120 && g > 105
                out.setPixel(px, py, if (isGoldInk) Color.BLACK else Color.WHITE)
            }
        }
        scaled.recycle()
        return out
    }

    /**
     * Converts a Bitmap into ESC/POS GS v 0 monochrome raster command bytes.
     */
    private fun bitmapToEscPosRaster(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val bytesPerRow = (width + 7) / 8

        val baos = ByteArrayOutputStream()
        // GS v 0 m xL xH yL yH
        baos.write(0x1D)
        baos.write(0x76)
        baos.write(0x30)
        baos.write(0x00)
        baos.write(bytesPerRow and 0xFF)
        baos.write((bytesPerRow shr 8) and 0xFF)
        baos.write(height and 0xFF)
        baos.write((height shr 8) and 0xFF)

        for (y in 0 until height) {
            for (bx in 0 until bytesPerRow) {
                var slice = 0
                for (bit in 0 until 8) {
                    val x = bx * 8 + bit
                    if (x < width) {
                        val pixel = bitmap.getPixel(x, y)
                        val luminance = (0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel))
                        if (luminance < 160) {
                            slice = slice or (1 shl (7 - bit))
                        }
                    }
                }
                baos.write(slice)
            }
        }
        return baos.toByteArray()
    }
}

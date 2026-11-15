package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.repository.BoutiqueRepository
import com.example.ui.BoutiqueUiState
import com.example.ui.components.PinPadView
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneSoft
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.IndianFormatters

enum class ReportType {
    DAILY_SALES,
    WEEKLY_SALES,
    MONTHLY_SALES,
    ANNUAL_SALES,
    PROFIT_LOSS,
    EXPENSE,
    STOCK,
    PURCHASE,
    CUSTOMER_LEDGER,
    SUPPLIER_LEDGER,
    PAYMENT_METHOD,
    BEST_SELLING,
    LOW_STOCK
}

enum class DateRangePreset {
    TODAY, WEEK, MONTH, YEAR, ALL, CUSTOM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: BoutiqueUiState,
    repository: BoutiqueRepository,
    isUnlocked: Boolean,
    onUnlockWithPin: (String) -> Unit,
    onExportReport: (
        title: String,
        fileBase: String,
        summaryLines: List<String>,
        headers: List<String>,
        rows: List<List<String>>,
        asCsv: Boolean,
        whatsapp: Boolean
    ) -> Unit,
    onPrintReport: (
        title: String,
        summaryLines: List<String>,
        headers: List<String>,
        rows: List<List<String>>
    ) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    if (!isUnlocked) {
        Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TopAppBar(
                title = { Text(strings.reportsAnalytics, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldDeep)
            )
            PinPadView(
                title = strings.financialPinLock,
                subtitle = strings.enterPin,
                onPinComplete = onUnlockWithPin,
                modifier = Modifier.weight(1f)
            )
        }
        return
    }

    var selectedReport by remember { mutableStateOf(ReportType.PROFIT_LOSS) }
    var datePreset by remember { mutableStateOf(DateRangePreset.MONTH) }
    val now = System.currentTimeMillis()
    var customStart by remember { mutableLongStateOf(IndianFormatters.startOfMonthMillis(now)) }
    var customEnd by remember { mutableLongStateOf(now) }
    var showDatePicker by remember { mutableStateOf(false) }

    val (fromMillis, toMillis) = remember(datePreset, selectedReport, customStart, customEnd) {
        val currentNow = System.currentTimeMillis()
        when {
            selectedReport == ReportType.DAILY_SALES -> IndianFormatters.startOfTodayMillis(currentNow) to IndianFormatters.endOfDayMillis(currentNow)
            selectedReport == ReportType.WEEKLY_SALES -> IndianFormatters.startOfWeekMillis(currentNow) to IndianFormatters.endOfDayMillis(currentNow)
            selectedReport == ReportType.MONTHLY_SALES -> IndianFormatters.startOfMonthMillis(currentNow) to IndianFormatters.endOfDayMillis(currentNow)
            selectedReport == ReportType.ANNUAL_SALES -> IndianFormatters.startOfYearMillis(currentNow) to IndianFormatters.endOfDayMillis(currentNow)
            datePreset == DateRangePreset.TODAY -> IndianFormatters.startOfTodayMillis(currentNow) to IndianFormatters.endOfDayMillis(currentNow)
            datePreset == DateRangePreset.WEEK -> IndianFormatters.startOfWeekMillis(currentNow) to IndianFormatters.endOfDayMillis(currentNow)
            datePreset == DateRangePreset.MONTH -> IndianFormatters.startOfMonthMillis(currentNow) to IndianFormatters.endOfDayMillis(currentNow)
            datePreset == DateRangePreset.YEAR -> IndianFormatters.startOfYearMillis(currentNow) to IndianFormatters.endOfDayMillis(currentNow)
            datePreset == DateRangePreset.CUSTOM -> customStart to IndianFormatters.endOfDayMillis(customEnd)
            else -> 0L to Long.MAX_VALUE
        }
    }

    val accounting = remember(state, fromMillis, toMillis) {
        repository.computeAccountingSummary(
            allProducts = state.products,
            allCustomers = state.customers,
            allSuppliers = state.suppliers,
            allSales = state.sales,
            allCustomerPayments = state.customerPayments,
            allPurchases = state.purchases,
            allSupplierPayments = state.supplierPayments,
            allExpenses = state.expenses,
            allReturns = state.saleReturns,
            fromMillis = fromMillis,
            toMillis = toMillis
        )
    }

    val reportData = remember(selectedReport, state, accounting, fromMillis, toMillis, strings) {
        buildReportPayload(selectedReport, state, accounting, fromMillis, toMillis, strings)
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(strings.reportsAnalytics, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldDeep)
        )

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date Range Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        DateRangePreset.TODAY to "Today",
                        DateRangePreset.WEEK to "This Week",
                        DateRangePreset.MONTH to "This Month",
                        DateRangePreset.YEAR to "This Year",
                        DateRangePreset.ALL to "All Time"
                    ).forEach { (preset, label) ->
                        item {
                            FilterChip(
                                selected = datePreset == preset,
                                onClick = { datePreset = preset },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                    item {
                        FilterChip(
                            selected = datePreset == DateRangePreset.CUSTOM,
                            onClick = { showDatePicker = true },
                            leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            label = {
                                Text(
                                    if (datePreset == DateRangePreset.CUSTOM) {
                                        "${IndianFormatters.formatDate(customStart)} - ${IndianFormatters.formatDate(customEnd)}"
                                    } else "Custom Range"
                                )
                            }
                        )
                    }
                }
            }

            // Report Type Selector
            item {
                val reportEntries = listOf(
                    ReportType.PROFIT_LOSS to strings.profitAndLoss,
                    ReportType.DAILY_SALES to strings.dailySalesReport,
                    ReportType.WEEKLY_SALES to strings.weeklySalesReport,
                    ReportType.MONTHLY_SALES to strings.monthlySalesReport,
                    ReportType.ANNUAL_SALES to strings.annualSalesReport,
                    ReportType.EXPENSE to strings.expenseReport,
                    ReportType.STOCK to strings.stockReport,
                    ReportType.PURCHASE to strings.purchaseReport,
                    ReportType.CUSTOMER_LEDGER to strings.customerLedger,
                    ReportType.SUPPLIER_LEDGER to "Supplier Ledger",
                    ReportType.PAYMENT_METHOD to strings.paymentMethodReport,
                    ReportType.BEST_SELLING to strings.bestSellingProducts,
                    ReportType.LOW_STOCK to strings.lowStockProducts
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(reportEntries) { (type, label) ->
                        FilterChip(
                            selected = selectedReport == type,
                            onClick = { selectedReport = type },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChampagneSoft,
                                selectedLabelColor = EmeraldDeep
                            )
                        )
                    }
                }
            }

            // Full Accounting Snapshot Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmeraldDeep),
                    border = BorderStroke(1.dp, ChampagneGold)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(reportData.title, style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                        HorizontalDivider(color = ChampagneGold.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 2.dp))
                        AccountingLine("Net Sales", IndianFormatters.formatCurrency(accounting.netSales), Color.White)
                        AccountingLine(strings.costOfGoods, IndianFormatters.formatCurrency(accounting.costOfGoodsSold), Color.White)
                        AccountingLine(strings.grossProfit, IndianFormatters.formatCurrency(accounting.grossProfit), ChampagneGold)
                        AccountingLine(strings.operatingExpenses, IndianFormatters.formatCurrency(accounting.operatingExpenses), Color(0xFFFFB4AB))
                        AccountingLine(
                            strings.netProfit,
                            IndianFormatters.formatCurrency(accounting.netProfit),
                            if (accounting.netProfit >= 0) Color(0xFF8FF7C3) else Color(0xFFFFB4AB),
                            bold = true
                        )
                        HorizontalDivider(color = ChampagneGold.copy(alpha = 0.25f), modifier = Modifier.padding(vertical = 2.dp))
                        AccountingLine("${strings.cash} / ${strings.upi} / Bank", "${IndianFormatters.formatCurrency(accounting.cashReceived)} | ${IndianFormatters.formatCurrency(accounting.upiReceived)} | ${IndianFormatters.formatCurrency(accounting.bankReceived)}", Color.White)
                        AccountingLine("Credit Sales (Period)", IndianFormatters.formatCurrency(accounting.creditSalesAmount), Color.White)
                        AccountingLine(strings.customerOutstanding, IndianFormatters.formatCurrency(accounting.customerOutstanding), ChampagneGold)
                        AccountingLine(strings.supplierOutstanding, IndianFormatters.formatCurrency(accounting.supplierOutstanding), Color.White)
                        AccountingLine(strings.cashBalance, IndianFormatters.formatCurrency(accounting.cashBalance), Color.White)
                        AccountingLine(strings.dailyClosingBalance, IndianFormatters.formatCurrency(accounting.dailyClosingBalance), ChampagneGold)
                    }
                }
            }

            // Export & Share Action Row
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onExportReport(reportData.title, reportData.fileBase, reportData.summaryLines, reportData.headers, reportData.rows, false, false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.weight(1f).testTag("btn_report_pdf"),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.exportPdf, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = {
                            onExportReport(reportData.title, reportData.fileBase, reportData.summaryLines, reportData.headers, reportData.rows, true, false)
                        },
                        modifier = Modifier.weight(1f).testTag("btn_report_csv"),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.exportCsv, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onExportReport(reportData.title, reportData.fileBase, reportData.summaryLines, reportData.headers, reportData.rows, false, true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("WhatsApp PDF", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = {
                            onPrintReport(reportData.title, reportData.summaryLines, reportData.headers, reportData.rows)
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Print", maxLines = 1)
                    }
                }
            }

            // Data Table Preview
            item {
                Surface(
                    color = EmeraldPrimary,
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp)) {
                        reportData.headers.forEach { h ->
                            Text(
                                text = h,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (reportData.rows.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            strings.noData,
                            modifier = Modifier.padding(20.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(reportData.rows) { row ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                            row.forEach { cell ->
                                Text(
                                    text = cell,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = customStart,
            initialSelectedEndDateMillis = customEnd
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedStartDateMillis?.let { customStart = it }
                    pickerState.selectedEndDateMillis?.let { customEnd = it }
                    datePreset = DateRangePreset.CUSTOM
                    showDatePicker = false
                }) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strings.cancel) }
            }
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.height(420.dp))
        }
    }
}

@Composable
private fun AccountingLine(label: String, value: String, color: Color, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
        Text(
            value,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}

private data class ReportPayload(
    val title: String,
    val fileBase: String,
    val summaryLines: List<String>,
    val headers: List<String>,
    val rows: List<List<String>>
)

private fun buildReportPayload(
    type: ReportType,
    state: BoutiqueUiState,
    acc: com.example.data.repository.AccountingSummary,
    fromMillis: Long,
    toMillis: Long,
    strings: com.example.ui.i18n.AppStrings
): ReportPayload {
    val salesInRange = state.sales.filter { it.timestamp in fromMillis..toMillis }
    val expensesInRange = state.expenses.filter { it.expenseDate in fromMillis..toMillis }
    val purchasesInRange = state.purchases.filter { it.purchaseDate in fromMillis..toMillis }
    val saleIdsInRange = salesInRange.map { it.id }.toSet()

    val baseSummary = listOf(
        "Period: ${if (fromMillis == 0L) "All Time" else IndianFormatters.formatDate(fromMillis)} to ${if (toMillis == Long.MAX_VALUE) "Now" else IndianFormatters.formatDate(toMillis)}",
        "Net Sales: ${IndianFormatters.formatCurrency(acc.netSales)} | COGS: ${IndianFormatters.formatCurrency(acc.costOfGoodsSold)}",
        "Gross Profit: ${IndianFormatters.formatCurrency(acc.grossProfit)} | Expenses: ${IndianFormatters.formatCurrency(acc.operatingExpenses)} | Net Profit: ${IndianFormatters.formatCurrency(acc.netProfit)}"
    )

    return when (type) {
        ReportType.DAILY_SALES, ReportType.WEEKLY_SALES, ReportType.MONTHLY_SALES, ReportType.ANNUAL_SALES -> {
            val title = when (type) {
                ReportType.DAILY_SALES -> strings.dailySalesReport
                ReportType.WEEKLY_SALES -> strings.weeklySalesReport
                ReportType.MONTHLY_SALES -> strings.monthlySalesReport
                else -> strings.annualSalesReport
            }
            ReportPayload(
                title = title,
                fileBase = type.name,
                summaryLines = baseSummary,
                headers = listOf("Invoice", "Customer", "Method", "Total", "Due"),
                rows = salesInRange.map { s ->
                    listOf(
                        s.invoiceNumber,
                        s.customerName,
                        s.paymentMethod,
                        IndianFormatters.formatCurrency(s.grandTotal),
                        IndianFormatters.formatCurrency(s.remainingBalance)
                    )
                }
            )
        }
        ReportType.PROFIT_LOSS -> ReportPayload(
            title = strings.profitAndLoss,
            fileBase = "Profit_Loss",
            summaryLines = baseSummary,
            headers = listOf("Metric", "Amount"),
            rows = listOf(
                listOf("Gross Billed Sales", IndianFormatters.formatCurrency(acc.grossBilledSales)),
                listOf("Net Sales (After Returns)", IndianFormatters.formatCurrency(acc.netSales)),
                listOf("Cost of Goods Sold (COGS)", IndianFormatters.formatCurrency(acc.costOfGoodsSold)),
                listOf("Gross Profit", IndianFormatters.formatCurrency(acc.grossProfit)),
                listOf("Operating Expenses", IndianFormatters.formatCurrency(acc.operatingExpenses)),
                listOf("Net Profit", IndianFormatters.formatCurrency(acc.netProfit)),
                listOf("Total Purchases (Inventory)", IndianFormatters.formatCurrency(acc.totalPurchases)),
                listOf("Cash Balance", IndianFormatters.formatCurrency(acc.cashBalance)),
                listOf("Customer Outstanding", IndianFormatters.formatCurrency(acc.customerOutstanding)),
                listOf("Supplier Outstanding", IndianFormatters.formatCurrency(acc.supplierOutstanding))
            )
        )
        ReportType.EXPENSE -> ReportPayload(
            title = strings.expenseReport,
            fileBase = "Expense_Report",
            summaryLines = listOf("Total Expenses: ${IndianFormatters.formatCurrency(acc.operatingExpenses)}"),
            headers = listOf("Date", "Category", "Method", "Amount", "Note"),
            rows = expensesInRange.map { e ->
                listOf(
                    IndianFormatters.formatDate(e.expenseDate),
                    e.category,
                    e.paymentMethod,
                    IndianFormatters.formatCurrency(e.amount),
                    e.description.ifBlank { "-" }
                )
            }
        )
        ReportType.STOCK -> ReportPayload(
            title = strings.stockReport,
            fileBase = "Stock_Report",
            summaryLines = listOf(
                "Total Products: ${state.products.size} | Cost Value: ${IndianFormatters.formatCurrency(acc.stockCostValue)} | Retail Value: ${IndianFormatters.formatCurrency(acc.stockRetailValue)}"
            ),
            headers = listOf("Product", "SKU", "Qty", "Buy", "Sell"),
            rows = state.products.map { p ->
                listOf(
                    "${p.name} (${p.size})",
                    p.sku,
                    "${p.quantity}",
                    IndianFormatters.formatCurrency(p.purchasePrice),
                    IndianFormatters.formatCurrency(p.sellingPrice)
                )
            }
        )
        ReportType.PURCHASE -> ReportPayload(
            title = strings.purchaseReport,
            fileBase = "Purchase_Report",
            summaryLines = listOf("Total Purchases: ${IndianFormatters.formatCurrency(acc.totalPurchases)}"),
            headers = listOf("Bill No", "Supplier", "Date", "Total", "Due"),
            rows = purchasesInRange.map { p ->
                listOf(
                    p.purchaseInvoiceNo,
                    p.supplierName,
                    IndianFormatters.formatDate(p.purchaseDate),
                    IndianFormatters.formatCurrency(p.totalAmount),
                    IndianFormatters.formatCurrency(p.remainingBalance)
                )
            }
        )
        ReportType.CUSTOMER_LEDGER -> ReportPayload(
            title = strings.customerLedger,
            fileBase = "Customer_Ledger",
            summaryLines = listOf("Total Customer Outstanding: ${IndianFormatters.formatCurrency(acc.customerOutstanding)}"),
            headers = listOf("Customer", "Mobile", "Purchases", "Paid", "Due / Adv"),
            rows = state.customers.map { c ->
                listOf(
                    c.name,
                    c.mobile,
                    IndianFormatters.formatCurrency(c.totalPurchases),
                    IndianFormatters.formatCurrency(c.totalPayments),
                    "${IndianFormatters.formatCurrency(c.outstandingBalance)} / ${IndianFormatters.formatCurrency(c.advanceBalance)}"
                )
            }
        )
        ReportType.SUPPLIER_LEDGER -> ReportPayload(
            title = "Supplier Ledger",
            fileBase = "Supplier_Ledger",
            summaryLines = listOf("Total Supplier Outstanding: ${IndianFormatters.formatCurrency(acc.supplierOutstanding)}"),
            headers = listOf("Supplier", "Contact", "Purchases", "Paid", "Outstanding"),
            rows = state.suppliers.map { s ->
                listOf(
                    s.name,
                    s.contactNumber,
                    IndianFormatters.formatCurrency(s.totalPurchases),
                    IndianFormatters.formatCurrency(s.totalPayments),
                    IndianFormatters.formatCurrency(s.outstandingAmount)
                )
            }
        )
        ReportType.PAYMENT_METHOD -> ReportPayload(
            title = strings.paymentMethodReport,
            fileBase = "Payment_Methods",
            summaryLines = listOf("Total Collections: ${IndianFormatters.formatCurrency(acc.totalCollections)}"),
            headers = listOf("Method", "Collected / Billed"),
            rows = listOf(
                listOf("Cash Received", IndianFormatters.formatCurrency(acc.cashReceived)),
                listOf("UPI Received", IndianFormatters.formatCurrency(acc.upiReceived)),
                listOf("Bank Transfer", IndianFormatters.formatCurrency(acc.bankReceived)),
                listOf("Other Methods", IndianFormatters.formatCurrency(acc.otherReceived)),
                listOf("Credit Billed (Unpaid)", IndianFormatters.formatCurrency(acc.creditSalesAmount))
            )
        )
        ReportType.BEST_SELLING -> {
            val grouped = state.saleItems
                .filter { it.saleId in saleIdsInRange }
                .groupBy { it.productName }
                .map { (name, items) ->
                    val netQty = items.sumOf { (it.quantity - it.returnedQuantity).coerceAtLeast(0) }
                    val rev = items.sumOf {
                        val ratio = if (it.quantity > 0) (it.quantity - it.returnedQuantity).toDouble() / it.quantity else 0.0
                        it.lineTotal * ratio
                    }
                    Triple(name, netQty, rev)
                }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
            ReportPayload(
                title = strings.bestSellingProducts,
                fileBase = "Best_Selling",
                summaryLines = baseSummary,
                headers = listOf("Product", "Net Qty Sold", "Revenue"),
                rows = grouped.map { (name, qty, rev) ->
                    listOf(name, "$qty", IndianFormatters.formatCurrency(rev))
                }
            )
        }
        ReportType.LOW_STOCK -> ReportPayload(
            title = strings.lowStockProducts,
            fileBase = "Low_Stock",
            summaryLines = listOf("Low Stock Products Count: ${state.lowStockProducts.size}"),
            headers = listOf("Product", "SKU", "Qty", "Min Alert"),
            rows = state.lowStockProducts.map { p ->
                listOf("${p.name} (${p.size})", p.sku, "${p.quantity}", "${p.minStockAlert}")
            }
        )
    }
}

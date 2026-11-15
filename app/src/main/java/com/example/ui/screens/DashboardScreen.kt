package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.SaleEntity
import com.example.ui.BoutiqueUiState
import com.example.ui.components.EmptyBoutiqueState
import com.example.ui.components.KpiMetricCard
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedBg
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneSoft
import com.example.ui.theme.CreditAmber
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IvoryWhite
import com.example.ui.theme.ProfitGreen
import com.example.util.IndianFormatters

data class QuickActionItem(
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val containerColor: Color,
    val contentColor: Color,
    val onClick: () -> Unit
)

@Composable
fun DashboardScreen(
    state: BoutiqueUiState,
    onNewSale: () -> Unit,
    onAddProduct: () -> Unit,
    onAddCustomer: () -> Unit,
    onAddPurchase: () -> Unit,
    onAddExpense: () -> Unit,
    onViewReports: () -> Unit,
    onPrintBill: () -> Unit,
    onOpenInvoice: (SaleEntity) -> Unit,
    onCycleLanguage: () -> Unit,
    onLockApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val today = state.todayAccounting
    val month = state.monthlyAccounting
    val overall = state.overallAccounting

    val quickActions = listOf(
        QuickActionItem(strings.newSale, Icons.Filled.PointOfSale, "qa_new_sale", EmeraldPrimary, Color.White, onNewSale),
        QuickActionItem(strings.addProduct, Icons.Filled.Inventory2, "qa_add_product", ChampagneSoft, EmeraldDeep, onAddProduct),
        QuickActionItem(strings.addCustomer, Icons.Filled.PersonAdd, "qa_add_customer", ChampagneSoft, EmeraldDeep, onAddCustomer),
        QuickActionItem(strings.addPurchase, Icons.Filled.AddShoppingCart, "qa_add_purchase", ChampagneSoft, EmeraldDeep, onAddPurchase),
        QuickActionItem(strings.addExpense, Icons.Filled.MoneyOff, "qa_add_expense", ChampagneSoft, EmeraldDeep, onAddExpense),
        QuickActionItem(strings.viewReports, Icons.Filled.Assessment, "qa_view_reports", ChampagneSoft, EmeraldDeep, onViewReports),
        QuickActionItem(strings.printBill, Icons.Filled.Print, "qa_print_bill", EmeraldDeep, ChampagneGold, onPrintBill)
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Luxury Brand Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(EmeraldDeep, EmeraldPrimary)
                        )
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_boutique_hero),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.22f)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_nc_monogram),
                            contentDescription = "NAQSH COLLECTION Emblem",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, ChampagneGold, CircleShape)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = state.settings.shopName,
                                style = MaterialTheme.typography.headlineMedium,
                                color = IvoryWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = state.settings.shopTagline,
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampagneGold,
                                letterSpacing = 1.4.sp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onCycleLanguage() }
                                .testTag("btn_cycle_language")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Translate,
                                    contentDescription = "Change Language",
                                    tint = ChampagneGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = when (state.settings.language) {
                                        AppLanguage.ROMAN_URDU -> "UR"
                                        AppLanguage.HINDI -> "HI"
                                        AppLanguage.ENGLISH -> "EN"
                                    },
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        if (state.settings.pinEnabled) {
                            Spacer(Modifier.width(4.dp))
                            IconButton(onClick = onLockApp) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = "Lock App",
                                    tint = ChampagneGold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Summary pills: Low Stock, Total Products, Total Customers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeaderStatPill(
                            label = strings.lowStockAlerts,
                            value = "${state.lowStockProducts.size}",
                            isWarning = state.lowStockProducts.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        )
                        HeaderStatPill(
                            label = strings.totalProducts,
                            value = "${state.products.size}",
                            isWarning = false,
                            modifier = Modifier.weight(1f)
                        )
                        HeaderStatPill(
                            label = strings.totalCustomers,
                            value = "${state.customers.size}",
                            isWarning = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Quick Actions Strip
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = strings.quickActions,
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldDeep
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(quickActions) { qa ->
                        Card(
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = qa.containerColor),
                            border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .width(118.dp)
                                .clickable { qa.onClick() }
                                .testTag(qa.testTag)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = qa.icon,
                                    contentDescription = qa.label,
                                    tint = qa.contentColor,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = qa.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = qa.contentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // 8 KPI Cards Grid (2 per row)
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiMetricCard(
                        label = strings.todaysSales,
                        value = IndianFormatters.formatCurrency(today.netSales),
                        accentColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = strings.todaysProfit,
                        value = IndianFormatters.formatCurrency(today.netProfit),
                        accentColor = if (today.netProfit >= 0) ProfitGreen else AlertRed,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiMetricCard(
                        label = strings.todaysExpenses,
                        value = IndianFormatters.formatCurrency(today.operatingExpenses),
                        accentColor = AlertRed,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = strings.todaysCashReceived,
                        value = IndianFormatters.formatCurrency(today.cashReceived),
                        accentColor = ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiMetricCard(
                        label = strings.monthlySales,
                        value = IndianFormatters.formatCurrency(month.netSales),
                        accentColor = EmeraldDeep,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = strings.monthlyProfit,
                        value = IndianFormatters.formatCurrency(month.netProfit),
                        accentColor = if (month.netProfit >= 0) ProfitGreen else AlertRed,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    KpiMetricCard(
                        label = strings.totalStockValue,
                        value = IndianFormatters.formatCurrency(overall.stockCostValue),
                        accentColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMetricCard(
                        label = strings.customerOutstanding,
                        value = IndianFormatters.formatCurrency(overall.customerOutstanding),
                        accentColor = CreditAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Low Stock Alert Section
        if (state.lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = AlertRedBg),
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${strings.lowStockAlerts} (${state.lowStockProducts.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = AlertRed
                            )
                        }
                        state.lowStockProducts.take(4).forEach { prod ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${prod.name} (${prod.size}/${prod.color})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "Qty: ${prod.quantity}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AlertRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Invoices
        item {
            Text(
                text = strings.recentInvoices,
                style = MaterialTheme.typography.titleMedium,
                color = EmeraldDeep,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (state.sales.isEmpty()) {
            item {
                EmptyBoutiqueState(
                    icon = Icons.Filled.ReceiptLong,
                    message = strings.noData,
                    actionLabel = strings.newSale,
                    onAction = onNewSale
                )
            }
        } else {
            items(state.sales.take(6), key = { it.id }) { sale ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onOpenInvoice(sale) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sale.invoiceNumber,
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldDeep
                            )
                            Text(
                                text = "${sale.customerName} • ${IndianFormatters.formatDateTime(sale.timestamp)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = IndianFormatters.formatCurrency(sale.grandTotal),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = if (sale.remainingBalance > 0) "Due: ${IndianFormatters.formatCurrency(sale.remainingBalance)}" else sale.paymentMethod,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (sale.remainingBalance > 0) AlertRed else ProfitGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderStatPill(
    label: String,
    value: String,
    isWarning: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isWarning) AlertRed.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ChampagneGold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

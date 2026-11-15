package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.entity.BoutiqueConstants
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.repository.ExchangeLineRequest
import com.example.data.repository.PurchaseLineRequest
import com.example.data.repository.ReturnLineRequest
import com.example.ui.BoutiqueUiState
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyBoutiqueState
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneSoft
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ProfitGreen
import com.example.util.IndianFormatters
import java.io.File

@Composable
fun InventoryTradeScreen(
    state: BoutiqueUiState,
    initialTab: Int = 0,
    openAddProductOnLaunch: Boolean = false,
    openAddPurchaseOnLaunch: Boolean = false,
    onConsumeLaunchFlags: () -> Unit,
    onCopyPickedImage: (Uri, (String?) -> Unit) -> Unit,
    onSaveProduct: (ProductEntity, () -> Unit) -> Unit,
    onAdjustStock: (Long, Int, String) -> Unit,
    onDeleteProduct: (ProductEntity) -> Unit,
    onSaveSupplier: (SupplierEntity, () -> Unit) -> Unit,
    onDeleteSupplier: (SupplierEntity) -> Unit,
    onRecordSupplierPayment: (Long, Double, String, String) -> Unit,
    onRecordPurchase: (String, Long?, String, List<PurchaseLineRequest>, Double, String, Long, String, () -> Unit) -> Unit,
    onProcessReturn: (Long, List<ReturnLineRequest>, List<ExchangeLineRequest>, String, Double, String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    var showProductDialog by remember(openAddProductOnLaunch) {
        mutableStateOf(openAddProductOnLaunch).also { if (openAddProductOnLaunch) onConsumeLaunchFlags() }
    }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var deletingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var adjustingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var historyProduct by remember { mutableStateOf<ProductEntity?>(null) }

    var showPurchaseDialog by remember(openAddPurchaseOnLaunch) {
        mutableStateOf(openAddPurchaseOnLaunch).also { if (openAddPurchaseOnLaunch) onConsumeLaunchFlags() }
    }

    var showSupplierDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var deletingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var payingSupplier by remember { mutableStateOf<SupplierEntity?>(null) }
    var ledgerSupplier by remember { mutableStateOf<SupplierEntity?>(null) }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(color = EmeraldDeep, shadowElevation = 4.dp) {
                Column {
                    Text(
                        text = strings.navStock,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = EmeraldDeep,
                        contentColor = ChampagneGold,
                        edgePadding = 12.dp
                    ) {
                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(strings.products) })
                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(strings.purchases) })
                        Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text(strings.suppliers) })
                        Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text(strings.returnsExchange) })
                    }
                }
            }

            when (selectedTab) {
                0 -> ProductsTab(
                    state = state,
                    onEdit = { editingProduct = it; showProductDialog = true },
                    onAdjust = { adjustingProduct = it },
                    onHistory = { historyProduct = it },
                    onDelete = { deletingProduct = it }
                )
                1 -> PurchasesTab(
                    state = state
                )
                2 -> SuppliersTab(
                    state = state,
                    onEdit = { editingSupplier = it; showSupplierDialog = true },
                    onPay = { payingSupplier = it },
                    onLedger = { ledgerSupplier = it },
                    onDelete = { deletingSupplier = it }
                )
                3 -> ReturnsExchangeTab(
                    state = state,
                    onProcessReturn = onProcessReturn
                )
            }
        }

        if (selectedTab in 0..2) {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> { editingProduct = null; showProductDialog = true }
                        1 -> showPurchaseDialog = true
                        2 -> { editingSupplier = null; showSupplierDialog = true }
                    }
                },
                containerColor = EmeraldPrimary,
                contentColor = ChampagneGold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("fab_stock_add")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
    }

    if (showProductDialog) {
        ProductEditorDialog(
            initial = editingProduct,
            suppliers = state.suppliers,
            onCopyPickedImage = onCopyPickedImage,
            onSave = { prod ->
                onSaveProduct(prod) { showProductDialog = false }
            },
            onDismiss = { showProductDialog = false }
        )
    }

    deletingProduct?.let { prod ->
        ConfirmDeleteDialog(
            title = strings.confirmDeleteTitle,
            body = "${strings.confirmDeleteBody}\n(${prod.name})",
            onConfirm = {
                onDeleteProduct(prod)
                deletingProduct = null
            },
            onDismiss = { deletingProduct = null }
        )
    }

    adjustingProduct?.let { prod ->
        StockAdjustDialog(
            product = prod,
            onConfirm = { delta, note ->
                onAdjustStock(prod.id, delta, note)
                adjustingProduct = null
            },
            onDismiss = { adjustingProduct = null }
        )
    }

    historyProduct?.let { prod ->
        StockHistoryDialog(
            product = prod,
            movements = state.stockMovements.filter { it.productId == prod.id },
            onDismiss = { historyProduct = null }
        )
    }

    if (showPurchaseDialog) {
        RecordPurchaseDialog(
            products = state.products,
            suppliers = state.suppliers,
            onSave = { invNo, suppId, suppName, lines, paid, method, note ->
                onRecordPurchase(invNo, suppId, suppName, lines, paid, method, System.currentTimeMillis(), note) {
                    showPurchaseDialog = false
                }
            },
            onDismiss = { showPurchaseDialog = false }
        )
    }

    if (showSupplierDialog) {
        SupplierEditorDialog(
            initial = editingSupplier,
            onSave = { supp ->
                onSaveSupplier(supp) { showSupplierDialog = false }
            },
            onDismiss = { showSupplierDialog = false }
        )
    }

    deletingSupplier?.let { supp ->
        ConfirmDeleteDialog(
            title = strings.confirmDeleteTitle,
            body = "${strings.confirmDeleteBody}\n(${supp.name})",
            onConfirm = {
                onDeleteSupplier(supp)
                deletingSupplier = null
            },
            onDismiss = { deletingSupplier = null }
        )
    }

    payingSupplier?.let { supp ->
        SupplierPaymentDialog(
            supplier = supp,
            onConfirm = { amt, method, note ->
                onRecordSupplierPayment(supp.id, amt, method, note)
                payingSupplier = null
            },
            onDismiss = { payingSupplier = null }
        )
    }

    ledgerSupplier?.let { supp ->
        SupplierLedgerDialog(
            supplier = supp,
            purchases = state.purchases.filter { it.supplierId == supp.id },
            payments = state.supplierPayments.filter { it.supplierId == supp.id },
            onDismiss = { ledgerSupplier = null }
        )
    }
}

@Composable
private fun ProductsTab(
    state: BoutiqueUiState,
    onEdit: (ProductEntity) -> Unit,
    onAdjust: (ProductEntity) -> Unit,
    onHistory: (ProductEntity) -> Unit,
    onDelete: (ProductEntity) -> Unit
) {
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("ALL") }
    var onlyLowStock by remember { mutableStateOf(false) }

    val filtered = remember(state.products, query, category, onlyLowStock) {
        state.products.filter { p ->
            val catOk = category == "ALL" || p.category.equals(category, true)
            val lowOk = !onlyLowStock || p.quantity <= p.minStockAlert
            val q = query.trim()
            val qOk = q.isEmpty() || p.name.contains(q, true) || p.sku.contains(q, true) || p.brand.contains(q, true)
            catOk && lowOk && qOk
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(strings.searchProductSku) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = category == "ALL" && !onlyLowStock,
                    onClick = { category = "ALL"; onlyLowStock = false },
                    label = { Text(strings.all) }
                )
            }
            item {
                FilterChip(
                    selected = onlyLowStock,
                    onClick = { onlyLowStock = !onlyLowStock },
                    label = { Text("${strings.lowStockAlerts} (${state.lowStockProducts.size})") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AlertRed, selectedLabelColor = Color.White)
                )
            }
            items(BoutiqueConstants.PRODUCT_CATEGORIES) { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = { category = cat },
                    label = { Text(cat) }
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyBoutiqueState(icon = Icons.Filled.Inventory2, message = strings.noData)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { prod ->
                    val isLow = prod.quantity <= prod.minStockAlert
                    Card(
                        border = BorderStroke(1.dp, if (isLow) AlertRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!prod.imageUri.isNullOrBlank() && File(prod.imageUri).exists()) {
                                    AsyncImage(
                                        model = File(prod.imageUri),
                                        contentDescription = prod.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(prod.name, style = MaterialTheme.typography.titleMedium, color = EmeraldDeep)
                                    Text(
                                        "${prod.category} • ${prod.size} • ${prod.color}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text("SKU: ${prod.sku} | ${prod.brand}", style = MaterialTheme.typography.labelSmall)
                                }
                                Surface(
                                    color = if (isLow) AlertRed else EmeraldPrimary,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "Qty: ${prod.quantity}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Buy: ${IndianFormatters.formatCurrency(prod.purchasePrice)} | Sell: ${IndianFormatters.formatCurrency(prod.sellingPrice)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldPrimary
                                )
                                Row {
                                    IconButton(onClick = { onAdjust(prod) }) {
                                        Icon(Icons.Filled.SwapVert, contentDescription = strings.stockAdjustment, tint = EmeraldPrimary)
                                    }
                                    IconButton(onClick = { onHistory(prod) }) {
                                        Icon(Icons.Filled.History, contentDescription = strings.stockHistory, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { onEdit(prod) }) {
                                        Icon(Icons.Filled.Edit, contentDescription = strings.edit, tint = EmeraldDeep)
                                    }
                                    IconButton(onClick = { onDelete(prod) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = AlertRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchasesTab(state: BoutiqueUiState) {
    val strings = LocalStrings.current
    if (state.purchases.isEmpty()) {
        EmptyBoutiqueState(icon = Icons.Filled.LocalShipping, message = strings.noData)
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.purchases, key = { it.id }) { purchase ->
                val items = state.purchaseItems.filter { it.purchaseId == purchase.id }
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(purchase.purchaseInvoiceNo, style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
                            Text(
                                IndianFormatters.formatCurrency(purchase.totalAmount),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                        Text("${purchase.supplierName} • ${IndianFormatters.formatDate(purchase.purchaseDate)}", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${items.size} items | Paid: ${IndianFormatters.formatCurrency(purchase.paidAmount)} | Due: ${IndianFormatters.formatCurrency(purchase.remainingBalance)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (purchase.remainingBalance > 0) AlertRed else ProfitGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuppliersTab(
    state: BoutiqueUiState,
    onEdit: (SupplierEntity) -> Unit,
    onPay: (SupplierEntity) -> Unit,
    onLedger: (SupplierEntity) -> Unit,
    onDelete: (SupplierEntity) -> Unit
) {
    val strings = LocalStrings.current
    if (state.suppliers.isEmpty()) {
        EmptyBoutiqueState(icon = Icons.Filled.AddBusiness, message = strings.noData)
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 84.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.suppliers, key = { it.id }) { supp ->
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(supp.name, style = MaterialTheme.typography.titleMedium, color = EmeraldDeep)
                                Text("${supp.contactNumber} • ${supp.address}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = "Due: ${IndianFormatters.formatCurrency(supp.outstandingAmount)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (supp.outstandingAmount > 0) AlertRed else ProfitGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onLedger(supp) }, modifier = Modifier.weight(1f)) {
                                Text("Ledger", maxLines = 1)
                            }
                            Button(
                                onClick = { onPay(supp) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(strings.paySupplier, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { onEdit(supp) }) { Icon(Icons.Filled.Edit, contentDescription = strings.edit) }
                            IconButton(onClick = { onDelete(supp) }) { Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = AlertRed) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReturnsExchangeTab(
    state: BoutiqueUiState,
    onProcessReturn: (Long, List<ReturnLineRequest>, List<ExchangeLineRequest>, String, Double, String, () -> Unit) -> Unit
) {
    val strings = LocalStrings.current
    var invoiceQuery by remember { mutableStateOf("") }
    var selectedSale by remember { mutableStateOf<SaleEntity?>(null) }
    val returnQtyMap = remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    val exchangeItems = remember { mutableStateListOf<Pair<ProductEntity, Int>>() }
    var payMethod by remember { mutableStateOf("CASH") }
    var extraPaidText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var showExchangePicker by remember { mutableStateOf(false) }

    val matchingSales = remember(state.sales, invoiceQuery) {
        val q = invoiceQuery.trim()
        if (q.isEmpty()) state.sales.take(8)
        else state.sales.filter {
            it.invoiceNumber.contains(q, true) || it.customerName.contains(q, true)
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = invoiceQuery,
                onValueChange = { invoiceQuery = it },
                label = { Text(strings.searchOriginalInvoice) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("return_invoice_search")
            )
        }

        if (selectedSale == null) {
            if (matchingSales.isEmpty()) {
                item { EmptyBoutiqueState(icon = Icons.Filled.AssignmentReturn, message = strings.noData) }
            } else {
                items(matchingSales, key = { it.id }) { sale ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedSale = sale
                            returnQtyMap.value = emptyMap()
                            exchangeItems.clear()
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(sale.invoiceNumber, fontWeight = FontWeight.Bold, color = EmeraldDeep)
                                Text("${sale.customerName} • ${IndianFormatters.formatDateTime(sale.timestamp)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(IndianFormatters.formatCurrency(sale.grandTotal), fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                        }
                    }
                }
            }
        } else {
            val sale = selectedSale!!
            val origItems = state.saleItems.filter { it.saleId == sale.id }
            val totalAfterItem = (sale.subtotal - sale.itemDiscountTotal).coerceAtLeast(1.0)
            val discountRatio = (sale.grandTotal / totalAfterItem).coerceIn(0.0, 1.0)

            val returnedTotal = origItems.sumOf { item ->
                val rq = returnQtyMap.value[item.id] ?: 0
                (item.lineTotal / item.quantity) * discountRatio * rq
            }
            val exchangeTotal = exchangeItems.sumOf { (p, qty) -> p.sellingPrice * qty }
            val netDiff = exchangeTotal - returnedTotal

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ChampagneSoft),
                    border = BorderStroke(1.dp, ChampagneGold)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(sale.invoiceNumber, fontWeight = FontWeight.Bold, color = EmeraldDeep)
                            Text("${sale.customerName} • Bill: ${IndianFormatters.formatCurrency(sale.grandTotal)}", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { selectedSale = null }) { Text(strings.cancel) }
                    }
                }
            }

            item { Text(strings.selectReturnItems, style = MaterialTheme.typography.titleSmall, color = EmeraldDeep) }

            items(origItems, key = { it.id }) { item ->
                val returnable = (item.quantity - item.returnedQuantity).coerceAtLeast(0)
                val chosen = returnQtyMap.value[item.id] ?: 0
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.productName, fontWeight = FontWeight.SemiBold)
                            Text("Sold: ${item.quantity} | Returnable: $returnable", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (chosen > 0) returnQtyMap.value = returnQtyMap.value + (item.id to (chosen - 1))
                                }
                            ) { Text("-", fontWeight = FontWeight.Bold) }
                            Text("$chosen", fontWeight = FontWeight.Bold)
                            IconButton(
                                onClick = {
                                    if (chosen < returnable) returnQtyMap.value = returnQtyMap.value + (item.id to (chosen + 1))
                                }
                            ) { Icon(Icons.Filled.Add, contentDescription = "Add return qty") }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.exchangeItems, style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
                    OutlinedButton(onClick = { showExchangePicker = true }) { Text("+ Add Exchange") }
                }
                exchangeItems.forEachIndexed { idx, (prod, qty) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${prod.name} x$qty (${IndianFormatters.formatCurrency(prod.sellingPrice * qty)})")
                        IconButton(onClick = { exchangeItems.removeAt(idx) }) {
                            Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = AlertRed)
                        }
                    }
                }
            }

            item {
                Surface(color = EmeraldDeep, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Return Value: ${IndianFormatters.formatCurrency(returnedTotal)}", color = Color.White)
                        Text("Exchange Value: ${IndianFormatters.formatCurrency(exchangeTotal)}", color = Color.White)
                        if (netDiff < 0) {
                            Text(
                                "${strings.refundAmount}: ${IndianFormatters.formatCurrency(-netDiff)}",
                                color = ChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (netDiff > 0) {
                            Text(
                                "${strings.extraPayable}: ${IndianFormatters.formatCurrency(netDiff)}",
                                color = ChampagneGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                val methods = if (sale.customerId != null) BoutiqueConstants.DIRECT_PAYMENT_METHODS + "ADVANCE" else BoutiqueConstants.DIRECT_PAYMENT_METHODS
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(methods) { m ->
                        FilterChip(
                            selected = payMethod == m,
                            onClick = { payMethod = m },
                            label = { Text(m) }
                        )
                    }
                }
                if (netDiff > 0) {
                    OutlinedTextField(
                        value = extraPaidText,
                        onValueChange = { extraPaidText = it },
                        label = { Text("Paid Extra (Default: ${IndianFormatters.formatCurrency(netDiff)})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(strings.notes) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val retRequests = returnQtyMap.value.entries
                            .filter { it.value > 0 }
                            .mapNotNull { (saleItemId, qty) ->
                                origItems.find { it.id == saleItemId }?.let { orig ->
                                    ReturnLineRequest(saleItemId, orig.productId, qty)
                                }
                            }
                        val exRequests = exchangeItems.map { (p, q) ->
                            ExchangeLineRequest(p.id, q, p.sellingPrice)
                        }
                        val addPaid = if (netDiff > 0) (extraPaidText.toDoubleOrNull() ?: netDiff) else 0.0
                        onProcessReturn(sale.id, retRequests, exRequests, payMethod, addPaid, reason) {
                            selectedSale = null
                        }
                    },
                    enabled = returnQtyMap.value.values.any { it > 0 },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("btn_process_return")
                ) {
                    Text(strings.processReturn, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (state.saleReturns.isNotEmpty()) {
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Recent Returns & Exchanges", style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
            }
            items(state.saleReturns.take(10), key = { it.id }) { ret ->
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${ret.returnNumber} (${ret.originalInvoiceNo})", fontWeight = FontWeight.Bold)
                            Text("${ret.customerName} • ${IndianFormatters.formatDateTime(ret.timestamp)}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            "Ret: ${IndianFormatters.formatCurrency(ret.returnedAmount)}",
                            color = AlertRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showExchangePicker) {
        AlertDialog(
            onDismissRequest = { showExchangePicker = false },
            title = { Text(strings.exchangeItems) },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.products.filter { it.quantity > 0 }, key = { it.id }) { p ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                val existingIdx = exchangeItems.indexOfFirst { it.first.id == p.id }
                                if (existingIdx >= 0) {
                                    val curr = exchangeItems[existingIdx]
                                    exchangeItems[existingIdx] = curr.first to (curr.second + 1)
                                } else {
                                    exchangeItems.add(p to 1)
                                }
                                showExchangePicker = false
                            }
                        ) {
                            Row(Modifier.padding(10.dp), Arrangement.SpaceBetween) {
                                Text("${p.name} (${p.size})", maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(IndianFormatters.formatCurrency(p.sellingPrice), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showExchangePicker = false }) { Text(strings.close) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductEditorDialog(
    initial: ProductEntity?,
    suppliers: List<SupplierEntity>,
    onCopyPickedImage: (Uri, (String?) -> Unit) -> Unit,
    onSave: (ProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sku by remember { mutableStateOf(initial?.sku ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: BoutiqueConstants.PRODUCT_CATEGORIES.first()) }
    var size by remember { mutableStateOf(initial?.size ?: "Free Size") }
    var color by remember { mutableStateOf(initial?.color ?: "Black") }
    var brand by remember { mutableStateOf(initial?.brand ?: "Naqsh") }
    var supplierId by remember { mutableStateOf(initial?.supplierId) }
    var supplierName by remember { mutableStateOf(initial?.supplierName ?: "") }
    var buyPrice by remember { mutableStateOf(initial?.purchasePrice?.toString() ?: "") }
    var sellPrice by remember { mutableStateOf(initial?.sellingPrice?.toString() ?: "") }
    var qty by remember { mutableStateOf(initial?.quantity?.toString() ?: "0") }
    var minAlert by remember { mutableStateOf(initial?.minStockAlert?.toString() ?: "3") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var imagePath by remember { mutableStateOf(initial?.imageUri) }

    var catExpanded by remember { mutableStateOf(false) }
    var sizeExpanded by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            onCopyPickedImage(uri) { copied -> if (copied != null) imagePath = copied }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) strings.addProduct else strings.edit) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!imagePath.isNullOrBlank() && File(imagePath!!).exists()) {
                        AsyncImage(
                            model = File(imagePath!!),
                            contentDescription = "Product image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                        )
                    }
                    OutlinedButton(onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.pickPhoto)
                    }
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(strings.productName) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_prod_name")
                )
                OutlinedTextField(
                    value = sku, onValueChange = { sku = it },
                    label = { Text(strings.skuBarcode) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = !catExpanded }) {
                    OutlinedTextField(
                        value = category, onValueChange = {}, readOnly = true,
                        label = { Text(strings.category) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                        BoutiqueConstants.PRODUCT_CATEGORIES.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; catExpanded = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = sizeExpanded, onExpandedChange = { sizeExpanded = !sizeExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = size, onValueChange = { size = it },
                            label = { Text(strings.sizeLabel) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sizeExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = sizeExpanded, onDismissRequest = { sizeExpanded = false }) {
                            BoutiqueConstants.PRODUCT_SIZES.forEach { sz ->
                                DropdownMenuItem(text = { Text(sz) }, onClick = { size = sz; sizeExpanded = false })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = color, onValueChange = { color = it },
                        label = { Text(strings.colorLabel) }, singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = buyPrice, onValueChange = { buyPrice = it },
                        label = { Text(strings.purchasePrice) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_prod_buy")
                    )
                    OutlinedTextField(
                        value = sellPrice, onValueChange = { sellPrice = it },
                        label = { Text(strings.sellingPrice) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_prod_sell")
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qty, onValueChange = { qty = it },
                        label = { Text(strings.quantity) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_prod_qty")
                    )
                    OutlinedTextField(
                        value = minAlert, onValueChange = { minAlert = it },
                        label = { Text(strings.minStockAlertLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = brand, onValueChange = { brand = it },
                    label = { Text(strings.brandLabel) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (suppliers.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(suppliers) { s ->
                            FilterChip(
                                selected = supplierId == s.id,
                                onClick = { supplierId = s.id; supplierName = s.name },
                                label = { Text(s.name) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text(strings.descriptionLabel) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = (initial ?: ProductEntity(sku = "", name = "", category = category, purchasePrice = 0.0, sellingPrice = 0.0, quantity = 0, size = size, color = color, brand = brand)).copy(
                        name = name.trim(),
                        sku = sku.trim(),
                        category = category,
                        size = size.trim(),
                        color = color.trim(),
                        brand = brand.trim(),
                        supplierId = supplierId,
                        supplierName = supplierName,
                        purchasePrice = buyPrice.toDoubleOrNull() ?: 0.0,
                        sellingPrice = sellPrice.toDoubleOrNull() ?: 0.0,
                        quantity = qty.toIntOrNull() ?: 0,
                        minStockAlert = minAlert.toIntOrNull() ?: 3,
                        description = description.trim(),
                        imageUri = imagePath
                    )
                    onSave(p)
                },
                enabled = name.isNotBlank() && (sellPrice.toDoubleOrNull() ?: 0.0) >= 0.0,
                modifier = Modifier.testTag("btn_save_product")
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun StockAdjustDialog(
    product: ProductEntity,
    onConfirm: (Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var isAdd by remember { mutableStateOf(true) }
    var qtyText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.stockAdjustment}: ${product.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current Stock: ${product.quantity}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isAdd, onClick = { isAdd = true }, label = { Text("+ Add Stock") })
                    FilterChip(selected = !isAdd, onClick = { isAdd = false }, label = { Text("- Reduce Stock") })
                }
                OutlinedTextField(
                    value = qtyText, onValueChange = { qtyText = it },
                    label = { Text(strings.quantity) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text(strings.notes) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val q = qtyText.toIntOrNull() ?: 0
                    if (q > 0) onConfirm(if (isAdd) q else -q, note)
                },
                enabled = (qtyText.toIntOrNull() ?: 0) > 0
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun StockHistoryDialog(
    product: ProductEntity,
    movements: List<com.example.data.local.entity.StockMovementEntity>,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.stockHistory}: ${product.name}") },
        text = {
            if (movements.isEmpty()) {
                Text(strings.noData)
            } else {
                LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(movements, key = { it.id }) { m ->
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${m.type} (${m.referenceNo})", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                Text("${IndianFormatters.formatDateTime(m.timestamp)} • ${m.note}", style = MaterialTheme.typography.labelSmall)
                            }
                            Text(
                                text = (if (m.quantityChange >= 0) "+" else "") + "${m.quantityChange} → ${m.newQuantity}",
                                color = if (m.quantityChange >= 0) ProfitGreen else AlertRed,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } }
    )
}

@Composable
private fun RecordPurchaseDialog(
    products: List<ProductEntity>,
    suppliers: List<SupplierEntity>,
    onSave: (String, Long?, String, List<PurchaseLineRequest>, Double, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var invoiceNo by remember { mutableStateOf("") }
    var selectedSupplierId by remember { mutableStateOf<Long?>(suppliers.firstOrNull()?.id) }
    var supplierName by remember { mutableStateOf(suppliers.firstOrNull()?.name ?: "") }
    val lines = remember { mutableStateListOf<PurchaseLineRequest>() }
    var paidText by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("CASH") }
    var note by remember { mutableStateOf("") }

    var pickedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var itemQty by remember { mutableStateOf("1") }
    var itemCost by remember { mutableStateOf(products.firstOrNull()?.purchasePrice?.toString() ?: "") }

    val totalAmount = lines.sumOf { it.quantity * it.unitPurchasePrice }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addPurchase) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = invoiceNo, onValueChange = { invoiceNo = it },
                    label = { Text(strings.purchaseInvoiceNo) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (suppliers.isNotEmpty()) {
                    Text(strings.selectSupplier, style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(suppliers) { s ->
                            FilterChip(
                                selected = selectedSupplierId == s.id,
                                onClick = { selectedSupplierId = s.id; supplierName = s.name },
                                label = { Text(s.name) }
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = supplierName, onValueChange = { supplierName = it },
                        label = { Text(strings.supplierLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()
                if (products.isEmpty()) {
                    Text("Add products in Inventory first", color = AlertRed)
                } else {
                    Text("Add Product Line", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(products) { p ->
                            FilterChip(
                                selected = pickedProduct?.id == p.id,
                                onClick = { pickedProduct = p; itemCost = p.purchasePrice.toString() },
                                label = { Text(p.name) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = itemQty, onValueChange = { itemQty = it },
                            label = { Text(strings.quantity) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = itemCost, onValueChange = { itemCost = it },
                            label = { Text(strings.purchasePrice) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true, modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val p = pickedProduct ?: return@OutlinedButton
                            val q = itemQty.toIntOrNull() ?: 0
                            val c = itemCost.toDoubleOrNull() ?: 0.0
                            if (q > 0 && c >= 0) {
                                lines.add(PurchaseLineRequest(p.id, q, c))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("+ Add Item to Purchase") }
                }

                lines.forEachIndexed { idx, l ->
                    val name = products.find { it.id == l.productId }?.name ?: "Item"
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("$name: ${l.quantity} x ${IndianFormatters.formatCurrency(l.unitPurchasePrice)}", style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { lines.removeAt(idx) }) { Icon(Icons.Filled.Delete, contentDescription = null, tint = AlertRed) }
                    }
                }

                Text("${strings.total}: ${IndianFormatters.formatCurrency(totalAmount)}", fontWeight = FontWeight.Bold, color = EmeraldDeep)

                OutlinedTextField(
                    value = paidText, onValueChange = { paidText = it },
                    label = { Text("${strings.amountPaid} (Default: Full)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(BoutiqueConstants.DIRECT_PAYMENT_METHODS) { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                    }
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text(strings.notes) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val paid = paidText.toDoubleOrNull() ?: totalAmount
                    onSave(invoiceNo, selectedSupplierId, supplierName, lines.toList(), paid, method, note)
                },
                enabled = lines.isNotEmpty()
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun SupplierEditorDialog(
    initial: SupplierEntity?,
    onSave: (SupplierEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.contactNumber ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) strings.addSupplier else strings.edit) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.supplierLabel) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(strings.mobileNumber) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(strings.address) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = (initial ?: SupplierEntity(name = "", contactNumber = "")).copy(
                        name = name.trim(), contactNumber = phone.trim(), address = address.trim()
                    )
                    onSave(s)
                },
                enabled = name.isNotBlank()
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun SupplierPaymentDialog(
    supplier: SupplierEntity,
    onConfirm: (Double, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var amountText by remember { mutableStateOf(if (supplier.outstandingAmount > 0) supplier.outstandingAmount.toString() else "") }
    var method by remember { mutableStateOf("CASH") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.paySupplier}: ${supplier.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Outstanding: ${IndianFormatters.formatCurrency(supplier.outstandingAmount)}")
                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = it },
                    label = { Text(strings.amount) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(BoutiqueConstants.DIRECT_PAYMENT_METHODS) { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(strings.notes) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { (amountText.toDoubleOrNull() ?: 0.0).takeIf { it > 0 }?.let { onConfirm(it, method, note) } },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun SupplierLedgerDialog(
    supplier: SupplierEntity,
    purchases: List<com.example.data.local.entity.PurchaseEntity>,
    payments: List<com.example.data.local.entity.SupplierPaymentEntity>,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${supplier.name} Ledger") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Total Purchases: ${IndianFormatters.formatCurrency(supplier.totalPurchases)}")
                Text("Total Paid: ${IndianFormatters.formatCurrency(supplier.totalPayments)}")
                Text("Outstanding: ${IndianFormatters.formatCurrency(supplier.outstandingAmount)}", color = AlertRed, fontWeight = FontWeight.Bold)
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Purchases (${purchases.size}):", fontWeight = FontWeight.Bold)
                purchases.forEach { p ->
                    Text("${p.purchaseInvoiceNo} (${IndianFormatters.formatDate(p.purchaseDate)}): ${IndianFormatters.formatCurrency(p.totalAmount)}", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Payments (${payments.size}):", fontWeight = FontWeight.Bold)
                payments.forEach { p ->
                    Text("${IndianFormatters.formatDate(p.timestamp)} [${p.paymentMethod}]: ${IndianFormatters.formatCurrency(p.amount)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } }
    )
}

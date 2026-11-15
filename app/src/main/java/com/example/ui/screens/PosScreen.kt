package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleEntity
import com.example.ui.BoutiqueUiState
import com.example.ui.PosCartItem
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    state: BoutiqueUiState,
    cartItems: List<PosCartItem>,
    selectedCustomerId: Long?,
    walkInName: String,
    walkInMobile: String,
    overallDiscount: Double,
    paymentMethod: String,
    amountPaidText: String,
    useAdvance: Boolean,
    saleNotes: String,
    initialTab: Int = 0,
    onAddToCart: (ProductEntity) -> Unit,
    onUpdateCartQty: (Long, Int) -> Unit,
    onUpdateCartDiscount: (Long, Double) -> Unit,
    onClearCart: () -> Unit,
    onSelectCustomer: (Long?) -> Unit,
    onWalkInChanged: (String, String) -> Unit,
    onQuickAddCustomer: (String, String, String, (Long) -> Unit) -> Unit,
    onOverallDiscountChanged: (Double) -> Unit,
    onPaymentMethodChanged: (String) -> Unit,
    onAmountPaidTextChanged: (String) -> Unit,
    onUseAdvanceChanged: (Boolean) -> Unit,
    onNotesChanged: (String) -> Unit,
    onCompleteSale: () -> Unit,
    onOpenPastInvoice: (SaleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showCheckoutSheet by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var editingCartItem by remember { mutableStateOf<PosCartItem?>(null) }

    val filteredProducts = remember(state.products, searchQuery, selectedCategory) {
        state.products.filter { prod ->
            val matchesCat = selectedCategory == "ALL" || prod.category.equals(selectedCategory, ignoreCase = true)
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                prod.name.contains(q, ignoreCase = true) ||
                prod.sku.contains(q, ignoreCase = true) ||
                prod.color.contains(q, ignoreCase = true) ||
                prod.size.contains(q, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
    val itemDisc = cartItems.sumOf { it.itemDiscount.coerceIn(0.0, it.unitPrice * it.quantity) }
    val afterItemDisc = (subtotal - itemDisc).coerceAtLeast(0.0)
    val cappedOverallDisc = overallDiscount.coerceIn(0.0, afterItemDisc)
    val grandTotal = (afterItemDisc - cappedOverallDisc).coerceAtLeast(0.0)

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top Header + Mode Tabs
        Surface(color = EmeraldDeep, shadowElevation = 4.dp) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.navPos,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    if (cartItems.isNotEmpty()) {
                        TextButton(onClick = onClearCart) {
                            Text(strings.cancel, color = ChampagneGold)
                        }
                    }
                }
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = EmeraldDeep,
                    contentColor = ChampagneGold
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(strings.newSale, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("${strings.invoiceHistory} (${state.sales.size})", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        if (selectedTab == 1) {
            InvoiceHistoryList(
                sales = state.sales,
                onOpenInvoice = onOpenPastInvoice
            )
        } else {
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val isWide = maxWidth >= 720.dp
                if (isWide) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        ProductSelectorPane(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it },
                            products = filteredProducts,
                            cartItems = cartItems,
                            onAddToCart = onAddToCart,
                            modifier = Modifier.weight(1.25f).fillMaxHeight()
                        )
                        Surface(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            tonalElevation = 2.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            PosCheckoutPanel(
                                state = state,
                                cartItems = cartItems,
                                selectedCustomerId = selectedCustomerId,
                                walkInName = walkInName,
                                walkInMobile = walkInMobile,
                                overallDiscount = overallDiscount,
                                grandTotal = grandTotal,
                                subtotal = subtotal,
                                itemDisc = itemDisc,
                                paymentMethod = paymentMethod,
                                amountPaidText = amountPaidText,
                                useAdvance = useAdvance,
                                saleNotes = saleNotes,
                                onUpdateQty = onUpdateCartQty,
                                onEditItem = { editingCartItem = it },
                                onPickCustomer = { showCustomerPicker = true },
                                onAddCustomer = { showAddCustomerDialog = true },
                                onWalkInChanged = onWalkInChanged,
                                onOverallDiscountChanged = onOverallDiscountChanged,
                                onPaymentMethodChanged = onPaymentMethodChanged,
                                onAmountPaidTextChanged = onAmountPaidTextChanged,
                                onUseAdvanceChanged = onUseAdvanceChanged,
                                onNotesChanged = onNotesChanged,
                                onCompleteSale = onCompleteSale
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ProductSelectorPane(
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            selectedCategory = selectedCategory,
                            onCategoryChange = { selectedCategory = it },
                            products = filteredProducts,
                            cartItems = cartItems,
                            onAddToCart = onAddToCart,
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        )
                        // Bottom Sticky Cart Bar on Phones
                        Surface(
                            color = EmeraldDeep,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCheckoutSheet = true }
                                .testTag("pos_open_cart_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = ChampagneGold, contentColor = EmeraldDeep) {
                                                Text("${cartItems.sumOf { it.quantity }}")
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Filled.ShoppingCart,
                                            contentDescription = "Cart",
                                            tint = Color.White
                                        )
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column {
                                        Text(
                                            text = IndianFormatters.formatCurrency(grandTotal),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ChampagneGold
                                        )
                                        Text(
                                            text = if (cartItems.isEmpty()) strings.cartEmpty else "${cartItems.size} items • Tap to Checkout",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                                Button(
                                    onClick = { showCheckoutSheet = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ChampagneGold,
                                        contentColor = EmeraldDeep
                                    ),
                                    enabled = cartItems.isNotEmpty(),
                                    modifier = Modifier.testTag("btn_pos_checkout")
                                ) {
                                    Text(strings.completeSale, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCheckoutSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCheckoutSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            PosCheckoutPanel(
                state = state,
                cartItems = cartItems,
                selectedCustomerId = selectedCustomerId,
                walkInName = walkInName,
                walkInMobile = walkInMobile,
                overallDiscount = overallDiscount,
                grandTotal = grandTotal,
                subtotal = subtotal,
                itemDisc = itemDisc,
                paymentMethod = paymentMethod,
                amountPaidText = amountPaidText,
                useAdvance = useAdvance,
                saleNotes = saleNotes,
                onUpdateQty = onUpdateCartQty,
                onEditItem = { editingCartItem = it },
                onPickCustomer = { showCustomerPicker = true },
                onAddCustomer = { showAddCustomerDialog = true },
                onWalkInChanged = onWalkInChanged,
                onOverallDiscountChanged = onOverallDiscountChanged,
                onPaymentMethodChanged = onPaymentMethodChanged,
                onAmountPaidTextChanged = onAmountPaidTextChanged,
                onUseAdvanceChanged = onUseAdvanceChanged,
                onNotesChanged = onNotesChanged,
                onCompleteSale = {
                    showCheckoutSheet = false
                    onCompleteSale()
                }
            )
        }
    }

    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = state.customers,
            selectedId = selectedCustomerId,
            onSelect = {
                onSelectCustomer(it)
                showCustomerPicker = false
            },
            onAddNew = {
                showCustomerPicker = false
                showAddCustomerDialog = true
            },
            onDismiss = { showCustomerPicker = false }
        )
    }

    if (showAddCustomerDialog) {
        QuickAddCustomerDialog(
            onSave = { name, mobile, address ->
                onQuickAddCustomer(name, mobile, address) { newId ->
                    onSelectCustomer(newId)
                }
                showAddCustomerDialog = false
            },
            onDismiss = { showAddCustomerDialog = false }
        )
    }

    editingCartItem?.let { item ->
        var discText by remember(item) { mutableStateOf(if (item.itemDiscount > 0) item.itemDiscount.toString() else "") }
        AlertDialog(
            onDismissRequest = { editingCartItem = null },
            title = { Text("${item.product.name} - ${strings.itemDiscount}") },
            text = {
                OutlinedTextField(
                    value = discText,
                    onValueChange = { discText = it },
                    label = { Text(strings.itemDiscount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateCartDiscount(item.product.id, discText.toDoubleOrNull() ?: 0.0)
                    editingCartItem = null
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { editingCartItem = null }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun ProductSelectorPane(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    products: List<ProductEntity>,
    cartItems: List<PosCartItem>,
    onAddToCart: (ProductEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Column(modifier = modifier) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text(strings.searchProductSku) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("pos_search_input")
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == "ALL",
                    onClick = { onCategoryChange("ALL") },
                    label = { Text(strings.all) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            items(BoutiqueConstants.PRODUCT_CATEGORIES) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategoryChange(cat) },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        if (products.isEmpty()) {
            EmptyBoutiqueState(
                icon = Icons.Filled.PointOfSale,
                message = strings.noData
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    val inCartQty = cartItems.find { it.product.id == product.id }?.quantity ?: 0
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAddToCart(product) }
                            .testTag("pos_prod_${product.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            1.dp,
                            if (inCartQty > 0) ChampagneGold else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!product.imageUri.isNullOrBlank() && File(product.imageUri).exists()) {
                                AsyncImage(
                                    model = File(product.imageUri),
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = EmeraldDeep,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${product.sku} • ${product.size} • ${product.color}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = IndianFormatters.formatCurrency(product.sellingPrice),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                    Text(
                                        text = "Stock: ${product.quantity}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (product.quantity <= product.minStockAlert) AlertRed else ProfitGreen
                                    )
                                }
                            }
                            if (inCartQty > 0) {
                                Surface(
                                    color = ChampagneSoft,
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, ChampagneGold)
                                ) {
                                    Text(
                                        text = "x$inCartQty",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldDeep
                                    )
                                }
                            } else {
                                IconButton(onClick = { onAddToCart(product) }) {
                                    Icon(
                                        Icons.Filled.AddShoppingCart,
                                        contentDescription = "Add to bill",
                                        tint = EmeraldPrimary
                                    )
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
private fun PosCheckoutPanel(
    state: BoutiqueUiState,
    cartItems: List<PosCartItem>,
    selectedCustomerId: Long?,
    walkInName: String,
    walkInMobile: String,
    overallDiscount: Double,
    grandTotal: Double,
    subtotal: Double,
    itemDisc: Double,
    paymentMethod: String,
    amountPaidText: String,
    useAdvance: Boolean,
    saleNotes: String,
    onUpdateQty: (Long, Int) -> Unit,
    onEditItem: (PosCartItem) -> Unit,
    onPickCustomer: () -> Unit,
    onAddCustomer: () -> Unit,
    onWalkInChanged: (String, String) -> Unit,
    onOverallDiscountChanged: (Double) -> Unit,
    onPaymentMethodChanged: (String) -> Unit,
    onAmountPaidTextChanged: (String) -> Unit,
    onUseAdvanceChanged: (Boolean) -> Unit,
    onNotesChanged: (String) -> Unit,
    onCompleteSale: () -> Unit
) {
    val strings = LocalStrings.current
    val selectedCustomer = selectedCustomerId?.let { id -> state.customers.find { it.id == id } }
    val advanceApplicable = if (selectedCustomer != null && useAdvance) {
        minOf(selectedCustomer.advanceBalance, grandTotal)
    } else 0.0
    val netPayable = (grandTotal - advanceApplicable).coerceAtLeast(0.0)
    val enteredPaid = if (paymentMethod == "CREDIT") 0.0 else (amountPaidText.toDoubleOrNull() ?: netPayable)
    val changeReturned = if (paymentMethod == "CREDIT") 0.0 else (enteredPaid - netPayable).coerceAtLeast(0.0)
    val remainingCredit = (netPayable - enteredPaid).coerceAtLeast(0.0)

    var discInput by remember(overallDiscount) {
        mutableStateOf(if (overallDiscount > 0) overallDiscount.toString() else "")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Customer Selection Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPickCustomer,
                modifier = Modifier.weight(1f).height(48.dp).testTag("btn_pos_select_customer")
            ) {
                Text(
                    text = selectedCustomer?.name ?: strings.walkInCustomer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = onAddCustomer,
                modifier = Modifier.height(48.dp).testTag("btn_pos_add_customer")
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = strings.addCustomer, modifier = Modifier.size(18.dp))
            }
        }

        if (selectedCustomer == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = walkInName,
                    onValueChange = { onWalkInChanged(it, walkInMobile) },
                    label = { Text(strings.customerName) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = walkInMobile,
                    onValueChange = { onWalkInChanged(walkInName, it) },
                    label = { Text(strings.mobileNumber) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        "${selectedCustomer.name} (${selectedCustomer.mobile})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "Due: ${IndianFormatters.formatCurrency(selectedCustomer.outstandingBalance)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedCustomer.outstandingBalance > 0) AlertRed else ProfitGreen
                        )
                        Text(
                            "Adv: ${IndianFormatters.formatCurrency(selectedCustomer.advanceBalance)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary
                        )
                    }
                    if (selectedCustomer.advanceBalance > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = useAdvance, onCheckedChange = onUseAdvanceChanged)
                            Text(strings.useAdvanceBalance, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        // Cart Items
        if (cartItems.isEmpty()) {
            Text(strings.cartEmpty, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            cartItems.forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.product.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${IndianFormatters.formatCurrency(item.unitPrice)} x ${item.quantity}" +
                                        if (item.itemDiscount > 0) " (-${IndianFormatters.formatCurrency(item.itemDiscount)})" else "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                IndianFormatters.formatCurrency(item.lineTotal),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDeep
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { onEditItem(item) }) {
                                Text(strings.itemDiscount, style = MaterialTheme.typography.labelSmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onUpdateQty(item.product.id, item.quantity - 1) }) {
                                    Icon(
                                        if (item.quantity <= 1) Icons.Filled.Delete else Icons.Filled.Remove,
                                        contentDescription = "Decrease quantity",
                                        tint = if (item.quantity <= 1) AlertRed else EmeraldDeep
                                    )
                                }
                                Text("${item.quantity}", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { onUpdateQty(item.product.id, item.quantity + 1) }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase quantity", tint = EmeraldPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Discount + Totals
        OutlinedTextField(
            value = discInput,
            onValueChange = {
                discInput = it
                onOverallDiscountChanged(it.toDoubleOrNull() ?: 0.0)
            },
            label = { Text(strings.overallDiscount) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Surface(
            color = EmeraldDeep,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(strings.subtotal, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
                    Text(IndianFormatters.formatCurrency(subtotal), color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
                if (itemDisc + overallDiscount > 0) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(strings.discount, color = ChampagneGold, style = MaterialTheme.typography.bodySmall)
                        Text("-${IndianFormatters.formatCurrency(itemDisc + overallDiscount)}", color = ChampagneGold, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text(strings.grandTotal, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(
                        IndianFormatters.formatCurrency(grandTotal),
                        color = ChampagneGold,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Payment Method Chips
        Text(strings.paymentMethod, style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(BoutiqueConstants.PAYMENT_METHODS) { method ->
                val label = when (method) {
                    "CASH" -> strings.cash
                    "UPI" -> strings.upi
                    "BANK_TRANSFER" -> strings.bankTransfer
                    "CREDIT" -> strings.creditUdhar
                    else -> strings.otherMethod
                }
                FilterChip(
                    selected = paymentMethod == method,
                    onClick = { onPaymentMethodChanged(method) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (paymentMethod != "CREDIT") {
            OutlinedTextField(
                value = amountPaidText,
                onValueChange = onAmountPaidTextChanged,
                label = { Text("${strings.amountPaid} (Default: ${IndianFormatters.formatCurrency(netPayable)})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("pos_amount_paid_input")
            )
        }

        if (changeReturned > 0) {
            Text(
                "${strings.changeReturned}: ${IndianFormatters.formatCurrency(changeReturned)}",
                color = ProfitGreen,
                fontWeight = FontWeight.Bold
            )
        }
        if (remainingCredit > 0.01) {
            Text(
                "${strings.remainingCredit}: ${IndianFormatters.formatCurrency(remainingCredit)}",
                color = AlertRed,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedTextField(
            value = saleNotes,
            onValueChange = onNotesChanged,
            label = { Text(strings.notes) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onCompleteSale,
            enabled = cartItems.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_save_invoice")
        ) {
            Icon(Icons.Filled.Check, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.completeSale, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InvoiceHistoryList(
    sales: List<SaleEntity>,
    onOpenInvoice: (SaleEntity) -> Unit
) {
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(sales, query) {
        val q = query.trim()
        if (q.isEmpty()) sales
        else sales.filter {
            it.invoiceNumber.contains(q, ignoreCase = true) ||
                it.customerName.contains(q, ignoreCase = true) ||
                it.customerMobile.contains(q, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(strings.search) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
        if (filtered.isEmpty()) {
            EmptyBoutiqueState(icon = Icons.Filled.ReceiptLong, message = strings.noData)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { sale ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenInvoice(sale) },
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
                                Text(sale.invoiceNumber, style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
                                Text(sale.customerName, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    IndianFormatters.formatDateTime(sale.timestamp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    IndianFormatters.formatCurrency(sale.grandTotal),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    if (sale.remainingBalance > 0) "Due: ${IndianFormatters.formatCurrency(sale.remainingBalance)}" else sale.paymentMethod,
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
}

@Composable
private fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onAddNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var q by remember { mutableStateOf("") }
    val filtered = customers.filter {
        q.isBlank() || it.name.contains(q, true) || it.mobile.contains(q, true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.selectCustomer) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = q,
                    onValueChange = { q = it },
                    placeholder = { Text(strings.search) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { onSelect(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text(strings.walkInCustomer)
                }
                LazyColumn(modifier = Modifier.height(220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered, key = { it.id }) { cust ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (cust.id == selectedId) ChampagneSoft else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(cust.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(cust.name, fontWeight = FontWeight.SemiBold)
                                    Text(cust.mobile, style = MaterialTheme.typography.bodySmall)
                                }
                                if (cust.outstandingBalance > 0) {
                                    Text(
                                        IndianFormatters.formatCurrency(cust.outstandingBalance),
                                        color = AlertRed,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAddNew) { Text("+ ${strings.addCustomer}") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.close) }
        }
    )
}

@Composable
fun QuickAddCustomerDialog(
    initialCustomer: CustomerEntity? = null,
    onSave: (name: String, mobile: String, address: String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf(initialCustomer?.name ?: "") }
    var mobile by remember { mutableStateOf(initialCustomer?.mobile ?: "") }
    var address by remember { mutableStateOf(initialCustomer?.address ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialCustomer == null) strings.addCustomer else strings.edit) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.customerName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_cust_name")
                )
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text(strings.mobileNumber) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_cust_mobile")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(strings.address) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name.trim(), mobile.trim(), address.trim()) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("btn_save_customer")
            ) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

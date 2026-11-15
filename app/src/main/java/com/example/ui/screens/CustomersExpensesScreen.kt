package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BoutiqueConstants
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.SaleEntity
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    state: BoutiqueUiState,
    openAddOnLaunch: Boolean = false,
    onConsumeLaunchFlag: () -> Unit,
    onBack: () -> Unit,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onDeleteCustomer: (CustomerEntity) -> Unit,
    onRecordPayment: (Long, Double, String, Boolean, String) -> Unit,
    onShareStatement: (CustomerEntity, Boolean) -> Unit,
    onOpenSale: (SaleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var query by remember { mutableStateOf("") }
    var showEditor by remember(openAddOnLaunch) {
        mutableStateOf(openAddOnLaunch).also { if (openAddOnLaunch) onConsumeLaunchFlag() }
    }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var deletingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var paymentTarget by remember { mutableStateOf<Pair<CustomerEntity, Boolean>?>(null) } // (cust, isAdvance)
    var ledgerCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    val filtered = remember(state.customers, query) {
        val q = query.trim()
        if (q.isEmpty()) state.customers
        else state.customers.filter { it.name.contains(q, true) || it.mobile.contains(q, true) }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(strings.customers, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldDeep)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(strings.search) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
            )

            if (filtered.isEmpty()) {
                EmptyBoutiqueState(icon = Icons.Filled.People, message = strings.noData)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.id }) { cust ->
                        Card(
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cust.name, style = MaterialTheme.typography.titleMedium, color = EmeraldDeep)
                                        Text("${cust.mobile} • ${cust.address.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Due: ${IndianFormatters.formatCurrency(cust.outstandingBalance)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (cust.outstandingBalance > 0) AlertRed else ProfitGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (cust.advanceBalance > 0) {
                                            Text(
                                                text = "Adv: ${IndianFormatters.formatCurrency(cust.advanceBalance)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldPrimary
                                            )
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { ledgerCustomer = cust }, modifier = Modifier.weight(1f)) {
                                        Text("Khata", maxLines = 1)
                                    }
                                    Button(
                                        onClick = { paymentTarget = cust to false },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Text(strings.receivePayment, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { editingCustomer = cust; showEditor = true }) {
                                        Icon(Icons.Filled.Edit, contentDescription = strings.edit)
                                    }
                                    IconButton(onClick = { deletingCustomer = cust }) {
                                        Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = AlertRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editingCustomer = null; showEditor = true },
            containerColor = EmeraldPrimary,
            contentColor = ChampagneGold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag("fab_add_customer")
        ) {
            Icon(Icons.Filled.Add, contentDescription = strings.addCustomer)
        }
    }

    if (showEditor) {
        QuickAddCustomerDialog(
            initialCustomer = editingCustomer,
            onSave = { name, mobile, address ->
                val base = editingCustomer ?: CustomerEntity(name = name, mobile = mobile)
                onSaveCustomer(base.copy(name = name, mobile = mobile, address = address))
                showEditor = false
            },
            onDismiss = { showEditor = false }
        )
    }

    deletingCustomer?.let { cust ->
        ConfirmDeleteDialog(
            title = strings.confirmDeleteTitle,
            body = "${strings.confirmDeleteBody}\n(${cust.name})",
            onConfirm = {
                onDeleteCustomer(cust)
                deletingCustomer = null
            },
            onDismiss = { deletingCustomer = null }
        )
    }

    paymentTarget?.let { (cust, isAdv) ->
        CustomerPaymentDialog(
            customer = cust,
            initialIsAdvance = isAdv,
            onConfirm = { amount, method, advFlag, note ->
                onRecordPayment(cust.id, amount, method, advFlag, note)
                paymentTarget = null
            },
            onDismiss = { paymentTarget = null }
        )
    }

    ledgerCustomer?.let { currentCust ->
        val freshCust = state.customers.find { it.id == currentCust.id } ?: currentCust
        CustomerLedgerDialog(
            customer = freshCust,
            sales = state.sales.filter { it.customerId == freshCust.id },
            payments = state.customerPayments.filter { it.customerId == freshCust.id },
            onShareWhatsApp = { onShareStatement(freshCust, true) },
            onSharePdf = { onShareStatement(freshCust, false) },
            onAddAdvance = { paymentTarget = freshCust to true },
            onOpenSale = onOpenSale,
            onDismiss = { ledgerCustomer = null }
        )
    }
}

@Composable
private fun CustomerPaymentDialog(
    customer: CustomerEntity,
    initialIsAdvance: Boolean,
    onConfirm: (Double, String, Boolean, String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    var isAdvance by remember { mutableStateOf(initialIsAdvance) }
    var amountText by remember {
        mutableStateOf(
            if (!initialIsAdvance && customer.outstandingBalance > 0) customer.outstandingBalance.toString() else ""
        )
    }
    var method by remember { mutableStateOf("CASH") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${customer.name} - ${strings.receivePayment}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Due: ${IndianFormatters.formatCurrency(customer.outstandingBalance)} | Adv: ${IndianFormatters.formatCurrency(customer.advanceBalance)}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isAdvance, onClick = { isAdvance = false }, label = { Text(strings.receivePayment) })
                    FilterChip(selected = isAdvance, onClick = { isAdvance = true }, label = { Text(strings.addAdvance) })
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(strings.amount) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(BoutiqueConstants.DIRECT_PAYMENT_METHODS) { m ->
                        FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.notes) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onConfirm(amt, method, isAdvance, note)
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}

@Composable
private fun CustomerLedgerDialog(
    customer: CustomerEntity,
    sales: List<SaleEntity>,
    payments: List<com.example.data.local.entity.CustomerPaymentEntity>,
    onShareWhatsApp: () -> Unit,
    onSharePdf: () -> Unit,
    onAddAdvance: () -> Unit,
    onOpenSale: (SaleEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${customer.name} (${strings.customerLedger})") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(color = ChampagneSoft, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${strings.totalPurchases}: ${IndianFormatters.formatCurrency(customer.totalPurchases)}")
                        Text("${strings.totalPayments}: ${IndianFormatters.formatCurrency(customer.totalPayments)}")
                        Text("Outstanding Due: ${IndianFormatters.formatCurrency(customer.outstandingBalance)}", color = AlertRed, fontWeight = FontWeight.Bold)
                        Text("${strings.advanceBalance}: ${IndianFormatters.formatCurrency(customer.advanceBalance)}", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onShareWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("WhatsApp", maxLines = 1)
                    }
                    OutlinedButton(onClick = onSharePdf, modifier = Modifier.weight(1f)) {
                        Text("PDF", maxLines = 1)
                    }
                }
                OutlinedButton(onClick = onAddAdvance, modifier = Modifier.fillMaxWidth()) {
                    Text("+ ${strings.addAdvance}")
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Invoices (${sales.size})", fontWeight = FontWeight.Bold)
                sales.forEach { s ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onOpenSale(s) }
                    ) {
                        Row(Modifier.padding(8.dp), Arrangement.SpaceBetween) {
                            Column {
                                Text(s.invoiceNumber, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                Text(IndianFormatters.formatDate(s.timestamp), style = MaterialTheme.typography.labelSmall)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(IndianFormatters.formatCurrency(s.grandTotal), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                if (s.remainingBalance > 0) {
                                    Text("Due: ${IndianFormatters.formatCurrency(s.remainingBalance)}", color = AlertRed, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Payments (${payments.size})", fontWeight = FontWeight.Bold)
                payments.forEach { p ->
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("${IndianFormatters.formatDate(p.timestamp)} • ${p.type}", style = MaterialTheme.typography.bodySmall)
                        Text(IndianFormatters.formatCurrency(p.amount), color = ProfitGreen, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    state: BoutiqueUiState,
    openAddOnLaunch: Boolean = false,
    onConsumeLaunchFlag: () -> Unit,
    onBack: () -> Unit,
    onAddExpense: (String, Double, String, String, Long) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var showAddDialog by remember(openAddOnLaunch) {
        mutableStateOf(openAddOnLaunch).also { if (openAddOnLaunch) onConsumeLaunchFlag() }
    }
    var deletingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(strings.expenses, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldDeep)
            )

            Surface(
                color = ChampagneSoft,
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ChampagneGold)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(strings.todaysExpenses, fontWeight = FontWeight.SemiBold)
                    Text(
                        IndianFormatters.formatCurrency(state.todayAccounting.operatingExpenses),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AlertRed
                    )
                }
            }

            if (state.expenses.isEmpty()) {
                EmptyBoutiqueState(icon = Icons.Filled.MoneyOff, message = strings.noData)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.expenses, key = { it.id }) { exp ->
                        Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exp.category, style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
                                    if (exp.description.isNotBlank()) {
                                        Text(exp.description, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        "${IndianFormatters.formatDate(exp.expenseDate)} • ${exp.paymentMethod}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        IndianFormatters.formatCurrency(exp.amount),
                                        fontWeight = FontWeight.Bold,
                                        color = AlertRed
                                    )
                                    IconButton(onClick = { deletingExpense = exp }) {
                                        Icon(Icons.Filled.Delete, contentDescription = strings.delete, tint = AlertRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = EmeraldPrimary,
            contentColor = ChampagneGold,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag("fab_add_expense")
        ) {
            Icon(Icons.Filled.Add, contentDescription = strings.addExpense)
        }
    }

    if (showAddDialog) {
        var selectedCat by remember { mutableStateOf(BoutiqueConstants.EXPENSE_CATEGORIES.first()) }
        var customCat by remember { mutableStateOf("") }
        var useCustom by remember { mutableStateOf(false) }
        var amountText by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("CASH") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(strings.addExpense) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.expenseCategory, style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(BoutiqueConstants.EXPENSE_CATEGORIES) { cat ->
                            FilterChip(
                                selected = !useCustom && selectedCat == cat,
                                onClick = { useCustom = false; selectedCat = cat },
                                label = { Text(cat) }
                            )
                        }
                        item {
                            FilterChip(
                                selected = useCustom,
                                onClick = { useCustom = true },
                                label = { Text("+ Custom") }
                            )
                        }
                    }
                    if (useCustom) {
                        OutlinedTextField(
                            value = customCat, onValueChange = { customCat = it },
                            label = { Text(strings.customCategory) }, singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = amountText, onValueChange = { amountText = it },
                        label = { Text(strings.amount) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth().testTag("input_expense_amt")
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(BoutiqueConstants.DIRECT_PAYMENT_METHODS) { m ->
                            FilterChip(selected = method == m, onClick = { method = m }, label = { Text(m) })
                        }
                    }
                    OutlinedTextField(
                        value = desc, onValueChange = { desc = it },
                        label = { Text(strings.notes) }, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val cat = if (useCustom) customCat.trim().ifBlank { "Miscellaneous" } else selectedCat
                        if (amt > 0) {
                            onAddExpense(cat, amt, method, desc, System.currentTimeMillis())
                            showAddDialog = false
                        }
                    },
                    enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier.testTag("btn_save_expense")
                ) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text(strings.cancel) } }
        )
    }

    deletingExpense?.let { exp ->
        ConfirmDeleteDialog(
            title = strings.confirmDeleteTitle,
            body = "${strings.confirmDeleteBody}\n(${exp.category}: ${IndianFormatters.formatCurrency(exp.amount)})",
            onConfirm = {
                onDeleteExpense(exp)
                deletingExpense = null
            },
            onDismiss = { deletingExpense = null }
        )
    }
}

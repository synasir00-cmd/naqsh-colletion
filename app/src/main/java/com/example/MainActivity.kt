package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.SaleWithItems
import com.example.ui.BoutiqueViewModel
import com.example.ui.UiEvent
import com.example.ui.components.InvoiceActionsSheet
import com.example.ui.components.NaqshSplashScreen
import com.example.ui.components.PinPadView
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalStrings
import com.example.ui.i18n.stringsFor
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.InventoryTradeScreen
import com.example.ui.screens.MoreDestination
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.PrinterSetupScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsSecurityScreen
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NaqshCollectionTheme
import com.example.util.ShareHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class TopTab {
    HOME, POS, STOCK, MORE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BoutiqueViewModel = viewModel()
            NaqshAppRoot(viewModel = viewModel)
        }
    }
}

@Composable
fun NaqshAppRoot(viewModel: BoutiqueViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val printerState by viewModel.printerState.collectAsStateWithLifecycle()
    val localBackups by viewModel.localBackups.collectAsStateWithLifecycle()
    val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
    val isReportsUnlocked by viewModel.isReportsUnlocked.collectAsStateWithLifecycle()

    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val selectedCustomerId by viewModel.selectedCustomerId.collectAsStateWithLifecycle()
    val walkInName by viewModel.walkInName.collectAsStateWithLifecycle()
    val walkInMobile by viewModel.walkInMobile.collectAsStateWithLifecycle()
    val overallDiscount by viewModel.overallDiscount.collectAsStateWithLifecycle()
    val paymentMethod by viewModel.paymentMethod.collectAsStateWithLifecycle()
    val amountPaidText by viewModel.amountPaidText.collectAsStateWithLifecycle()
    val useAdvance by viewModel.useAdvance.collectAsStateWithLifecycle()
    val saleNotes by viewModel.saleNotes.collectAsStateWithLifecycle()

    var showSplash by remember { mutableStateOf(true) }
    var currentTab by remember { mutableStateOf(TopTab.HOME) }
    var posInitialTab by remember { mutableIntStateOf(0) }
    var stockInitialTab by remember { mutableIntStateOf(0) }
    var openAddProductOnLaunch by remember { mutableStateOf(false) }
    var openAddPurchaseOnLaunch by remember { mutableStateOf(false) }
    var openAddCustomerOnLaunch by remember { mutableStateOf(false) }
    var openAddExpenseOnLaunch by remember { mutableStateOf(false) }
    var moreDestination by remember { mutableStateOf(MoreDestination.HUB) }

    var activeInvoiceSheet by remember { mutableStateOf<SaleWithItems?>(null) }
    var pendingSafPdfFile by remember { mutableStateOf<File?>(null) }
    var pendingSafBackupJson by remember { mutableStateOf<String?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val src = pendingSafPdfFile
        pendingSafPdfFile = null
        if (uri != null && src != null) {
            scope.launch(Dispatchers.IO) {
                val ok = ShareHelper.copyFileToSafUri(context, src, uri)
                viewModel.emitMessage(if (ok) "PDF saved to phone storage" else "Failed to save PDF", !ok)
            }
        }
    }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        val json = pendingSafBackupJson
        pendingSafBackupJson = null
        if (uri != null && json != null) {
            scope.launch(Dispatchers.IO) {
                val ok = ShareHelper.writeTextToSafUri(context, json, uri)
                viewModel.onBackupSavedToSaf(ok)
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.OpenInvoiceActions -> {
                    activeInvoiceSheet = event.saleWithItems
                }
                is UiEvent.RequestSavePdfToSaf -> {
                    pendingSafPdfFile = event.sourceFile
                    savePdfLauncher.launch(event.suggestedName)
                }
                is UiEvent.RequestSaveBackupToSaf -> {
                    pendingSafBackupJson = event.jsonContent
                    saveBackupLauncher.launch(event.suggestedName)
                }
            }
        }
    }

    val strings = stringsFor(uiState.settings.language)

    NaqshCollectionTheme {
        CompositionLocalProvider(LocalStrings provides strings) {
            when {
                showSplash -> {
                    NaqshSplashScreen(onFinished = { showSplash = false })
                }
                isAppLocked -> {
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { pad ->
                        Box(modifier = Modifier.fillMaxSize().padding(pad)) {
                            PinPadView(
                                title = strings.appName,
                                subtitle = strings.enterPin,
                                onPinComplete = { viewModel.unlockAppWithPin(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
                else -> {
                    // Back button returns from sub-screens in More or non-Home tabs cleanly
                    if (currentTab == TopTab.MORE && moreDestination != MoreDestination.HUB) {
                        BackHandler { moreDestination = MoreDestination.HUB }
                    } else if (currentTab != TopTab.HOME) {
                        BackHandler { currentTab = TopTab.HOME }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val isExpanded = maxWidth >= 720.dp

                        Scaffold(
                            snackbarHost = {
                                SnackbarHost(snackbarHostState) { data ->
                                    Snackbar(
                                        snackbarData = data,
                                        containerColor = EmeraldDeep,
                                        contentColor = ChampagneGold
                                    )
                                }
                            },
                            bottomBar = {
                                if (!isExpanded) {
                                    NavigationBar(
                                        containerColor = EmeraldDeep,
                                        contentColor = Color.White,
                                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                                    ) {
                                        val navColors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = EmeraldDeep,
                                            selectedTextColor = ChampagneGold,
                                            indicatorColor = ChampagneGold,
                                            unselectedIconColor = Color.White.copy(alpha = 0.78f),
                                            unselectedTextColor = Color.White.copy(alpha = 0.78f)
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == TopTab.HOME,
                                            onClick = { currentTab = TopTab.HOME },
                                            icon = { Icon(Icons.Filled.Dashboard, contentDescription = strings.navHome) },
                                            label = { Text(strings.navHome, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            colors = navColors,
                                            modifier = Modifier.testTag("nav_home")
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == TopTab.POS,
                                            onClick = { posInitialTab = 0; currentTab = TopTab.POS },
                                            icon = { Icon(Icons.Filled.PointOfSale, contentDescription = strings.navPos) },
                                            label = { Text(strings.navPos, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            colors = navColors,
                                            modifier = Modifier.testTag("nav_pos")
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == TopTab.STOCK,
                                            onClick = { stockInitialTab = 0; currentTab = TopTab.STOCK },
                                            icon = { Icon(Icons.Filled.Inventory2, contentDescription = strings.navStock) },
                                            label = { Text(strings.navStock, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            colors = navColors,
                                            modifier = Modifier.testTag("nav_stock")
                                        )
                                        NavigationBarItem(
                                            selected = currentTab == TopTab.MORE,
                                            onClick = { moreDestination = MoreDestination.HUB; currentTab = TopTab.MORE },
                                            icon = { Icon(Icons.Filled.Menu, contentDescription = strings.navMore) },
                                            label = { Text(strings.navMore, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            colors = navColors,
                                            modifier = Modifier.testTag("nav_more")
                                        )
                                    }
                                }
                            },
                            contentWindowInsets = WindowInsets.safeDrawing
                        ) { innerPadding ->
                            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                if (isExpanded) {
                                    NavigationRail(
                                        containerColor = EmeraldDeep,
                                        contentColor = Color.White,
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        NavigationRailItem(
                                            selected = currentTab == TopTab.HOME,
                                            onClick = { currentTab = TopTab.HOME },
                                            icon = { Icon(Icons.Filled.Dashboard, contentDescription = strings.navHome) },
                                            label = { Text(strings.navHome) }
                                        )
                                        NavigationRailItem(
                                            selected = currentTab == TopTab.POS,
                                            onClick = { posInitialTab = 0; currentTab = TopTab.POS },
                                            icon = { Icon(Icons.Filled.PointOfSale, contentDescription = strings.navPos) },
                                            label = { Text(strings.navPos) }
                                        )
                                        NavigationRailItem(
                                            selected = currentTab == TopTab.STOCK,
                                            onClick = { stockInitialTab = 0; currentTab = TopTab.STOCK },
                                            icon = { Icon(Icons.Filled.Inventory2, contentDescription = strings.navStock) },
                                            label = { Text(strings.navStock) }
                                        )
                                        NavigationRailItem(
                                            selected = currentTab == TopTab.MORE,
                                            onClick = { moreDestination = MoreDestination.HUB; currentTab = TopTab.MORE },
                                            icon = { Icon(Icons.Filled.Menu, contentDescription = strings.navMore) },
                                            label = { Text(strings.navMore) }
                                        )
                                    }
                                }

                                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                    when (currentTab) {
                                        TopTab.HOME -> DashboardScreen(
                                            state = uiState,
                                            onNewSale = { posInitialTab = 0; currentTab = TopTab.POS },
                                            onAddProduct = {
                                                stockInitialTab = 0
                                                openAddProductOnLaunch = true
                                                currentTab = TopTab.STOCK
                                            },
                                            onAddCustomer = {
                                                openAddCustomerOnLaunch = true
                                                moreDestination = MoreDestination.CUSTOMERS
                                                currentTab = TopTab.MORE
                                            },
                                            onAddPurchase = {
                                                stockInitialTab = 1
                                                openAddPurchaseOnLaunch = true
                                                currentTab = TopTab.STOCK
                                            },
                                            onAddExpense = {
                                                openAddExpenseOnLaunch = true
                                                moreDestination = MoreDestination.EXPENSES
                                                currentTab = TopTab.MORE
                                            },
                                            onViewReports = {
                                                moreDestination = MoreDestination.REPORTS
                                                currentTab = TopTab.MORE
                                            },
                                            onPrintBill = {
                                                posInitialTab = 1
                                                currentTab = TopTab.POS
                                            },
                                            onOpenInvoice = { sale ->
                                                scope.launch {
                                                    val full = withContext(Dispatchers.IO) { viewModel.getSaleWithItems(sale.id) }
                                                    if (full != null) activeInvoiceSheet = full
                                                }
                                            },
                                            onCycleLanguage = {
                                                val next = when (uiState.settings.language) {
                                                    AppLanguage.ROMAN_URDU -> AppLanguage.HINDI
                                                    AppLanguage.HINDI -> AppLanguage.ENGLISH
                                                    AppLanguage.ENGLISH -> AppLanguage.ROMAN_URDU
                                                }
                                                viewModel.setLanguage(next)
                                            },
                                            onLockApp = { viewModel.lockNow() }
                                        )

                                        TopTab.POS -> PosScreen(
                                            state = uiState,
                                            cartItems = cartItems,
                                            selectedCustomerId = selectedCustomerId,
                                            walkInName = walkInName,
                                            walkInMobile = walkInMobile,
                                            overallDiscount = overallDiscount,
                                            paymentMethod = paymentMethod,
                                            amountPaidText = amountPaidText,
                                            useAdvance = useAdvance,
                                            saleNotes = saleNotes,
                                            initialTab = posInitialTab,
                                            onAddToCart = { viewModel.addToCart(it) },
                                            onUpdateCartQty = { id, q -> viewModel.updateCartItemQuantity(id, q) },
                                            onUpdateCartDiscount = { id, d -> viewModel.updateCartItemDiscount(id, d) },
                                            onClearCart = { viewModel.clearCart() },
                                            onSelectCustomer = { viewModel.selectCartCustomer(it) },
                                            onWalkInChanged = { n, m -> viewModel.setWalkInDetails(n, m) },
                                            onQuickAddCustomer = { name, mobile, addr, onDone ->
                                                viewModel.saveCustomer(
                                                    com.example.data.local.entity.CustomerEntity(
                                                        name = name, mobile = mobile, address = addr
                                                    ),
                                                    onSaved = onDone
                                                )
                                            },
                                            onOverallDiscountChanged = { viewModel.setOverallDiscount(it) },
                                            onPaymentMethodChanged = { viewModel.setPaymentMethod(it) },
                                            onAmountPaidTextChanged = { viewModel.setAmountPaidText(it) },
                                            onUseAdvanceChanged = { viewModel.setUseAdvance(it) },
                                            onNotesChanged = { viewModel.setSaleNotes(it) },
                                            onCompleteSale = { viewModel.completeSale() },
                                            onOpenPastInvoice = { sale ->
                                                scope.launch {
                                                    val full = withContext(Dispatchers.IO) { viewModel.getSaleWithItems(sale.id) }
                                                    if (full != null) activeInvoiceSheet = full
                                                }
                                            }
                                        )

                                        TopTab.STOCK -> InventoryTradeScreen(
                                            state = uiState,
                                            initialTab = stockInitialTab,
                                            openAddProductOnLaunch = openAddProductOnLaunch,
                                            openAddPurchaseOnLaunch = openAddPurchaseOnLaunch,
                                            onConsumeLaunchFlags = {
                                                openAddProductOnLaunch = false
                                                openAddPurchaseOnLaunch = false
                                            },
                                            onCopyPickedImage = { uri, cb -> viewModel.copyPickedImage(uri, cb) },
                                            onSaveProduct = { prod, onDone -> viewModel.saveProduct(prod, onDone) },
                                            onAdjustStock = { id, delta, note -> viewModel.adjustStock(id, delta, note) },
                                            onDeleteProduct = { viewModel.deleteProduct(it) },
                                            onSaveSupplier = { supp, onDone -> viewModel.saveSupplier(supp, onDone) },
                                            onDeleteSupplier = { viewModel.deleteSupplier(it) },
                                            onRecordSupplierPayment = { id, amt, m, note -> viewModel.recordSupplierPayment(id, amt, m, note) },
                                            onRecordPurchase = { inv, sId, sName, lines, paid, m, dt, note, onDone ->
                                                viewModel.recordPurchase(inv, sId, sName, lines, paid, m, dt, note, onDone)
                                            },
                                            onProcessReturn = { saleId, retLines, exLines, m, addPaid, reason, onDone ->
                                                viewModel.processReturnOrExchange(saleId, retLines, exLines, m, addPaid, reason, onDone)
                                            }
                                        )

                                        TopTab.MORE -> when (moreDestination) {
                                            MoreDestination.HUB -> MoreHubScreen(
                                                state = uiState,
                                                printerState = printerState,
                                                onNavigate = { moreDestination = it }
                                            )
                                            MoreDestination.CUSTOMERS -> CustomersScreen(
                                                state = uiState,
                                                openAddOnLaunch = openAddCustomerOnLaunch,
                                                onConsumeLaunchFlag = { openAddCustomerOnLaunch = false },
                                                onBack = { moreDestination = MoreDestination.HUB },
                                                onSaveCustomer = { viewModel.saveCustomer(it) },
                                                onDeleteCustomer = { viewModel.deleteCustomer(it) },
                                                onRecordPayment = { id, amt, m, isAdv, note ->
                                                    viewModel.recordCustomerPayment(id, amt, m, isAdv, note)
                                                },
                                                onShareStatement = { cust, viaWa -> viewModel.shareCustomerStatement(cust, viaWa) },
                                                onOpenSale = { sale ->
                                                    scope.launch {
                                                        val full = withContext(Dispatchers.IO) { viewModel.getSaleWithItems(sale.id) }
                                                        if (full != null) activeInvoiceSheet = full
                                                    }
                                                }
                                            )
                                            MoreDestination.EXPENSES -> ExpensesScreen(
                                                state = uiState,
                                                openAddOnLaunch = openAddExpenseOnLaunch,
                                                onConsumeLaunchFlag = { openAddExpenseOnLaunch = false },
                                                onBack = { moreDestination = MoreDestination.HUB },
                                                onAddExpense = { cat, amt, m, desc, dt ->
                                                    viewModel.addExpense(cat, amt, m, desc, dt)
                                                },
                                                onDeleteExpense = { viewModel.deleteExpense(it) }
                                            )
                                            MoreDestination.REPORTS -> ReportsScreen(
                                                state = uiState,
                                                repository = viewModel.repository,
                                                isUnlocked = isReportsUnlocked,
                                                onUnlockWithPin = { viewModel.unlockReportsWithPin(it) },
                                                onExportReport = { title, base, sum, hdr, rows, csv, wa ->
                                                    viewModel.exportReport(title, base, sum, hdr, rows, csv, wa, saveViaSaf = !csv && !wa)
                                                },
                                                onPrintReport = { title, sum, hdr, rows ->
                                                    viewModel.printReportSystem(title, sum, hdr, rows)
                                                },
                                                onBack = { moreDestination = MoreDestination.HUB }
                                            )
                                            MoreDestination.PRINTERS -> PrinterSetupScreen(
                                                state = uiState,
                                                printerState = printerState,
                                                onRefreshPaired = { viewModel.refreshPairedPrinters() },
                                                onStartDiscovery = { viewModel.startPrinterDiscovery() },
                                                onStopDiscovery = { viewModel.stopPrinterDiscovery() },
                                                onConnectPrinter = { addr, name -> viewModel.connectPrinter(addr, name) },
                                                onDisconnectPrinter = { viewModel.disconnectPrinter() },
                                                onReconnectLast = { viewModel.reconnectLastPrinter() },
                                                onTestPrint = { viewModel.testPrint() },
                                                onSetPrinterConfig = { w, bmp -> viewModel.setPrinterConfig(w, bmp) },
                                                onBack = { moreDestination = MoreDestination.HUB }
                                            )
                                            MoreDestination.BACKUP -> BackupRestoreScreen(
                                                state = uiState,
                                                localBackups = localBackups,
                                                onCreateLocalBackup = { viewModel.createLocalBackupNow() },
                                                onExportBackupStorage = { viewModel.exportBackupToPhoneStorage() },
                                                onPickRestoreFile = { openBackupLauncher.launch(arrayOf("*/*")) },
                                                onRestoreLocalBackup = { viewModel.restoreFromLocalBackup(it) },
                                                onToggleAutoBackup = { viewModel.setAutoBackupEnabled(it) },
                                                onBack = { moreDestination = MoreDestination.HUB }
                                            )
                                            MoreDestination.SETTINGS -> SettingsSecurityScreen(
                                                state = uiState,
                                                onLanguageSelected = { viewModel.setLanguage(it) },
                                                onSaveShopDetails = { n, t, p, a -> viewModel.updateShopDetails(n, t, p, a) },
                                                onSetupOrChangePin = { curr, next -> viewModel.setupOrChangePin(curr, next) },
                                                onDisablePin = { viewModel.disablePin(it) },
                                                onToggleLockReports = { viewModel.setLockReportsWithPin(it) },
                                                onToggleNegativeStock = { viewModel.setAllowNegativeStock(it) },
                                                onBack = { moreDestination = MoreDestination.HUB }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    activeInvoiceSheet?.let { saleWithItems ->
                        InvoiceActionsSheet(
                            saleWithItems = saleWithItems,
                            printerConnectedName = printerState.connectedName,
                            onShareWhatsApp = { viewModel.shareInvoiceWhatsApp(saleWithItems) },
                            onSharePdf = { viewModel.shareInvoiceGeneric(saleWithItems) },
                            onPrintThermal = { dup -> viewModel.printInvoiceBluetooth(saleWithItems, dup) },
                            onPrintSystem = { viewModel.printInvoiceSystem(saleWithItems) },
                            onSaveInvoicePdf = { viewModel.requestSaveInvoicePdf(saleWithItems) },
                            onOpenPrinterSetup = {
                                activeInvoiceSheet = null
                                moreDestination = MoreDestination.PRINTERS
                                currentTab = TopTab.MORE
                            },
                            onDismiss = { activeInvoiceSheet = null }
                        )
                    }

                    pendingRestoreUri?.let { uri ->
                        AlertDialog(
                            onDismissRequest = { pendingRestoreUri = null },
                            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = AlertRed) },
                            title = { Text("Confirm Database Restore") },
                            text = { Text(strings.restoreConfirmMsg) },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.restoreFromSafUri(uri)
                                        pendingRestoreUri = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                                ) { Text(strings.confirm) }
                            },
                            dismissButton = {
                                TextButton(onClick = { pendingRestoreUri = null }) { Text(strings.cancel) }
                            }
                        )
                    }
                }
            }
        }
    }
}

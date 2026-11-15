package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NaqshDatabase
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.CustomerPaymentEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.PurchaseEntity
import com.example.data.local.entity.PurchaseItemEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.SaleReturnEntity
import com.example.data.local.entity.SaleReturnItemEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.local.entity.SupplierPaymentEntity
import com.example.data.preferences.BoutiquePreferences
import com.example.data.preferences.BoutiqueSettings
import com.example.data.repository.AccountingSummary
import com.example.data.repository.BoutiqueRepository
import com.example.data.repository.CartLineRequest
import com.example.data.repository.ExchangeLineRequest
import com.example.data.repository.PurchaseLineRequest
import com.example.data.repository.ReturnLineRequest
import com.example.data.repository.SaleWithItems
import com.example.ui.i18n.AppLanguage
import com.example.util.EscPosPrinterManager
import com.example.util.IndianFormatters
import com.example.util.LocalBackupInfo
import com.example.util.PdfInvoiceGenerator
import com.example.util.PrinterConnectionState
import com.example.util.ShareHelper
import com.example.util.SystemPrintHelper
import com.example.util.WhatsAppShareResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PosCartItem(
    val product: ProductEntity,
    val quantity: Int = 1,
    val unitPrice: Double = product.sellingPrice,
    val itemDiscount: Double = 0.0
) {
    val lineTotal: Double
        get() = ((unitPrice * quantity) - itemDiscount).coerceAtLeast(0.0)
}

data class BoutiqueUiState(
    val settings: BoutiqueSettings = BoutiqueSettings(),
    val products: List<ProductEntity> = emptyList(),
    val stockMovements: List<StockMovementEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val customerPayments: List<CustomerPaymentEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val supplierPayments: List<SupplierPaymentEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val saleItems: List<SaleItemEntity> = emptyList(),
    val purchases: List<PurchaseEntity> = emptyList(),
    val purchaseItems: List<PurchaseItemEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val saleReturns: List<SaleReturnEntity> = emptyList(),
    val saleReturnItems: List<SaleReturnItemEntity> = emptyList(),
    val todayAccounting: AccountingSummary = AccountingSummary(),
    val monthlyAccounting: AccountingSummary = AccountingSummary(),
    val overallAccounting: AccountingSummary = AccountingSummary(),
    val lowStockProducts: List<ProductEntity> = emptyList()
)

sealed class UiEvent {
    data class ShowMessage(val message: String, val isError: Boolean = false) : UiEvent()
    data class OpenInvoiceActions(val saleWithItems: SaleWithItems) : UiEvent()
    data class RequestSavePdfToSaf(val sourceFile: File, val suggestedName: String) : UiEvent()
    data class RequestSaveBackupToSaf(val jsonContent: String, val suggestedName: String) : UiEvent()
}

class BoutiqueViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val preferences = BoutiquePreferences(appContext)
    private val database = NaqshDatabase.getInstance(appContext)
    val repository = BoutiqueRepository(database, preferences)
    val printerManager = EscPosPrinterManager(appContext)

    val printerState: StateFlow<PrinterConnectionState> = printerManager.state

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _isReportsUnlocked = MutableStateFlow(false)
    val isReportsUnlocked: StateFlow<Boolean> = _isReportsUnlocked.asStateFlow()

    private val _localBackups = MutableStateFlow<List<LocalBackupInfo>>(emptyList())
    val localBackups: StateFlow<List<LocalBackupInfo>> = _localBackups.asStateFlow()

    // POS Cart State
    private val _cartItems = MutableStateFlow<List<PosCartItem>>(emptyList())
    val cartItems: StateFlow<List<PosCartItem>> = _cartItems.asStateFlow()

    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId: StateFlow<Long?> = _selectedCustomerId.asStateFlow()

    private val _walkInName = MutableStateFlow("")
    val walkInName: StateFlow<String> = _walkInName.asStateFlow()

    private val _walkInMobile = MutableStateFlow("")
    val walkInMobile: StateFlow<String> = _walkInMobile.asStateFlow()

    private val _overallDiscount = MutableStateFlow(0.0)
    val overallDiscount: StateFlow<Double> = _overallDiscount.asStateFlow()

    private val _paymentMethod = MutableStateFlow("CASH")
    val paymentMethod: StateFlow<String> = _paymentMethod.asStateFlow()

    private val _amountPaidText = MutableStateFlow("")
    val amountPaidText: StateFlow<String> = _amountPaidText.asStateFlow()

    private val _useAdvance = MutableStateFlow(false)
    val useAdvance: StateFlow<Boolean> = _useAdvance.asStateFlow()

    private val _saleNotes = MutableStateFlow("")
    val saleNotes: StateFlow<String> = _saleNotes.asStateFlow()

    // Reactive Database + Accounting State
    private val coreDataFlow = combine(
        preferences.settingsFlow,
        repository.products,
        repository.customers,
        repository.suppliers,
        repository.sales
    ) { settings, products, customers, suppliers, sales ->
        CoreTuple(settings, products, customers, suppliers, sales)
    }

    private val ledgerDataFlow = combine(
        repository.customerPayments,
        repository.purchases,
        repository.supplierPayments,
        repository.expenses,
        repository.saleReturns
    ) { custPayments, purchases, suppPayments, expenses, returns ->
        LedgerTuple(custPayments, purchases, suppPayments, expenses, returns)
    }

    private val detailItemsFlow = combine(
        repository.stockMovements,
        repository.saleItems,
        repository.purchaseItems,
        repository.saleReturnItems
    ) { movements, saleItems, purchaseItems, returnItems ->
        DetailTuple(movements, saleItems, purchaseItems, returnItems)
    }

    val uiState: StateFlow<BoutiqueUiState> = combine(
        coreDataFlow,
        ledgerDataFlow,
        detailItemsFlow
    ) { core, ledger, details ->
        val now = System.currentTimeMillis()
        val todayStart = IndianFormatters.startOfTodayMillis(now)
        val monthStart = IndianFormatters.startOfMonthMillis(now)

        val todayAcc = repository.computeAccountingSummary(
            allProducts = core.products,
            allCustomers = core.customers,
            allSuppliers = core.suppliers,
            allSales = core.sales,
            allCustomerPayments = ledger.customerPayments,
            allPurchases = ledger.purchases,
            allSupplierPayments = ledger.supplierPayments,
            allExpenses = ledger.expenses,
            allReturns = ledger.saleReturns,
            fromMillis = todayStart,
            toMillis = now
        )
        val monthlyAcc = repository.computeAccountingSummary(
            allProducts = core.products,
            allCustomers = core.customers,
            allSuppliers = core.suppliers,
            allSales = core.sales,
            allCustomerPayments = ledger.customerPayments,
            allPurchases = ledger.purchases,
            allSupplierPayments = ledger.supplierPayments,
            allExpenses = ledger.expenses,
            allReturns = ledger.saleReturns,
            fromMillis = monthStart,
            toMillis = now
        )
        val overallAcc = repository.computeAccountingSummary(
            allProducts = core.products,
            allCustomers = core.customers,
            allSuppliers = core.suppliers,
            allSales = core.sales,
            allCustomerPayments = ledger.customerPayments,
            allPurchases = ledger.purchases,
            allSupplierPayments = ledger.supplierPayments,
            allExpenses = ledger.expenses,
            allReturns = ledger.saleReturns
        )

        BoutiqueUiState(
            settings = core.settings,
            products = core.products,
            stockMovements = details.stockMovements,
            customers = core.customers,
            customerPayments = ledger.customerPayments,
            suppliers = core.suppliers,
            supplierPayments = ledger.supplierPayments,
            sales = core.sales,
            saleItems = details.saleItems,
            purchases = ledger.purchases,
            purchaseItems = details.purchaseItems,
            expenses = ledger.expenses,
            saleReturns = ledger.saleReturns,
            saleReturnItems = details.returnItems,
            todayAccounting = todayAcc,
            monthlyAccounting = monthlyAcc,
            overallAccounting = overallAcc,
            lowStockProducts = core.products.filter { it.quantity <= it.minStockAlert }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BoutiqueUiState()
    )

    init {
        viewModelScope.launch {
            val s = preferences.settingsFlow.first()
            _isAppLocked.value = s.pinEnabled
            _isReportsUnlocked.value = !s.pinEnabled || !s.lockReportsWithPin
            refreshLocalBackups()
        }
    }

    override fun onCleared() {
        super.onCleared()
        printerManager.release()
    }

    // --- PIN & Security ---
    fun unlockAppWithPin(pin: String) {
        viewModelScope.launch {
            if (preferences.verifyPin(pin)) {
                _isAppLocked.value = false
                _isReportsUnlocked.value = true
            } else {
                _events.emit(UiEvent.ShowMessage("Invalid PIN. Try again.", isError = true))
            }
        }
    }

    fun unlockReportsWithPin(pin: String) {
        viewModelScope.launch {
            if (preferences.verifyPin(pin)) {
                _isReportsUnlocked.value = true
            } else {
                _events.emit(UiEvent.ShowMessage("Incorrect PIN", isError = true))
            }
        }
    }

    fun setupOrChangePin(currentPin: String?, newPin: String) {
        viewModelScope.launch {
            val settings = uiState.value.settings
            if (settings.pinEnabled) {
                val ok = currentPin != null && preferences.verifyPin(currentPin)
                if (!ok) {
                    _events.emit(UiEvent.ShowMessage("Current PIN is incorrect", isError = true))
                    return@launch
                }
            }
            if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
                _events.emit(UiEvent.ShowMessage("PIN must be 4 digits", isError = true))
                return@launch
            }
            preferences.setPin(newPin)
            _isReportsUnlocked.value = true
            _events.emit(UiEvent.ShowMessage("4-digit PIN updated"))
        }
    }

    fun disablePin(currentPin: String) {
        viewModelScope.launch {
            if (preferences.verifyPin(currentPin)) {
                preferences.disablePin()
                _isAppLocked.value = false
                _isReportsUnlocked.value = true
                _events.emit(UiEvent.ShowMessage("PIN lock disabled"))
            } else {
                _events.emit(UiEvent.ShowMessage("Current PIN is incorrect", isError = true))
            }
        }
    }

    fun lockNow() {
        if (uiState.value.settings.pinEnabled) {
            _isAppLocked.value = true
            if (uiState.value.settings.lockReportsWithPin) {
                _isReportsUnlocked.value = false
            }
        }
    }

    // --- Settings ---
    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { preferences.setLanguage(language) }
    }

    fun updateShopDetails(name: String, tagline: String, phone: String, address: String) {
        viewModelScope.launch {
            preferences.updateShopDetails(name, tagline, phone, address)
            _events.emit(UiEvent.ShowMessage("Boutique details saved"))
        }
    }

    fun setLockReportsWithPin(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setLockReportsWithPin(enabled)
            if (!enabled) _isReportsUnlocked.value = true
        }
    }

    fun setAllowNegativeStock(allowed: Boolean) {
        viewModelScope.launch { preferences.setAllowNegativeStock(allowed) }
    }

    fun setPrinterConfig(paperWidthMm: Int, bitmapMode: Boolean) {
        viewModelScope.launch { preferences.setPrinterConfig(paperWidthMm, bitmapMode) }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setAutoBackupEnabled(enabled) }
    }

    // --- POS Cart Actions ---
    fun addToCart(product: ProductEntity) {
        val allowNeg = uiState.value.settings.allowNegativeStock
        val current = _cartItems.value.toMutableList()
        val idx = current.indexOfFirst { it.product.id == product.id }
        if (idx >= 0) {
            val existing = current[idx]
            if (!allowNeg && existing.quantity + 1 > product.quantity) {
                viewModelScope.launch {
                    _events.emit(UiEvent.ShowMessage("Stock limit reached for ${product.name} (${product.quantity})", isError = true))
                }
                return
            }
            current[idx] = existing.copy(quantity = existing.quantity + 1)
        } else {
            if (!allowNeg && product.quantity <= 0) {
                viewModelScope.launch {
                    _events.emit(UiEvent.ShowMessage("${product.name} is out of stock", isError = true))
                }
                return
            }
            current.add(PosCartItem(product = product, quantity = 1, unitPrice = product.sellingPrice))
        }
        _cartItems.value = current
    }

    fun updateCartItemQuantity(productId: Long, newQty: Int) {
        val allowNeg = uiState.value.settings.allowNegativeStock
        val current = _cartItems.value.toMutableList()
        val idx = current.indexOfFirst { it.product.id == productId }
        if (idx < 0) return
        if (newQty <= 0) {
            current.removeAt(idx)
        } else {
            val item = current[idx]
            if (!allowNeg && newQty > item.product.quantity) {
                viewModelScope.launch {
                    _events.emit(UiEvent.ShowMessage("Available stock: ${item.product.quantity}", isError = true))
                }
                return
            }
            current[idx] = item.copy(quantity = newQty)
        }
        _cartItems.value = current
    }

    fun updateCartItemDiscount(productId: Long, discount: Double) {
        _cartItems.value = _cartItems.value.map {
            if (it.product.id == productId) it.copy(itemDiscount = discount.coerceAtLeast(0.0)) else it
        }
    }

    fun updateCartItemPrice(productId: Long, price: Double) {
        _cartItems.value = _cartItems.value.map {
            if (it.product.id == productId) it.copy(unitPrice = price.coerceAtLeast(0.0)) else it
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _selectedCustomerId.value = null
        _walkInName.value = ""
        _walkInMobile.value = ""
        _overallDiscount.value = 0.0
        _paymentMethod.value = "CASH"
        _amountPaidText.value = ""
        _useAdvance.value = false
        _saleNotes.value = ""
    }

    fun selectCartCustomer(customerId: Long?) {
        _selectedCustomerId.value = customerId
        if (customerId == null) _useAdvance.value = false
    }

    fun setWalkInDetails(name: String, mobile: String) {
        _walkInName.value = name
        _walkInMobile.value = mobile
    }

    fun setOverallDiscount(discount: Double) {
        _overallDiscount.value = discount.coerceAtLeast(0.0)
    }

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
        if (method == "CREDIT") _amountPaidText.value = "0"
    }

    fun setAmountPaidText(text: String) {
        _amountPaidText.value = text
    }

    fun setUseAdvance(use: Boolean) {
        _useAdvance.value = use
    }

    fun setSaleNotes(notes: String) {
        _saleNotes.value = notes
    }

    fun completeSale(customInvoiceNo: String? = null) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            viewModelScope.launch { _events.emit(UiEvent.ShowMessage("Add products to bill first", isError = true)) }
            return
        }
        viewModelScope.launch {
            try {
                val subtotal = items.sumOf { it.unitPrice * it.quantity }
                val itemDisc = items.sumOf { it.itemDiscount.coerceIn(0.0, it.unitPrice * it.quantity) }
                val afterItem = (subtotal - itemDisc).coerceAtLeast(0.0)
                val overallDisc = _overallDiscount.value.coerceIn(0.0, afterItem)
                val grandTotal = (afterItem - overallDisc).coerceAtLeast(0.0)

                val selectedCust = _selectedCustomerId.value?.let { id ->
                    uiState.value.customers.find { it.id == id }
                }
                val advanceUsedEstimate = if (selectedCust != null && _useAdvance.value) {
                    minOf(selectedCust.advanceBalance, grandTotal)
                } else 0.0
                val defaultTendered = (grandTotal - advanceUsedEstimate).coerceAtLeast(0.0)
                val tendered = if (_paymentMethod.value == "CREDIT") {
                    0.0
                } else {
                    _amountPaidText.value.toDoubleOrNull() ?: defaultTendered
                }

                val saved = repository.createSale(
                    invoiceNumberInput = customInvoiceNo,
                    customerId = _selectedCustomerId.value,
                    walkInName = _walkInName.value,
                    walkInMobile = _walkInMobile.value,
                    lines = items.map {
                        CartLineRequest(
                            productId = it.product.id,
                            quantity = it.quantity,
                            unitSellingPrice = it.unitPrice,
                            itemDiscount = it.itemDiscount
                        )
                    },
                    overallDiscountInput = overallDisc,
                    paymentMethod = _paymentMethod.value,
                    tenderedAmount = tendered,
                    useAdvanceBalance = _useAdvance.value,
                    allowNegativeStock = uiState.value.settings.allowNegativeStock,
                    notes = _saleNotes.value
                )

                clearCart()
                maybeTriggerAutoBackup()
                _events.emit(UiEvent.OpenInvoiceActions(saved))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Unable to save invoice", isError = true))
            }
        }
    }

    // --- Products & Inventory ---
    fun copyPickedImage(uri: Uri, onCopied: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val path = ShareHelper.copyPickedImageToInternalStorage(appContext, uri)
            withContext(Dispatchers.Main) { onCopied(path) }
        }
    }

    fun saveProduct(product: ProductEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.saveProduct(product)
                maybeTriggerAutoBackup()
                _events.emit(UiEvent.ShowMessage("${product.name} saved"))
                onSaved()
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Error saving product", isError = true))
            }
        }
    }

    fun adjustStock(productId: Long, delta: Int, note: String) {
        viewModelScope.launch {
            try {
                repository.adjustStock(productId, delta, note, uiState.value.settings.allowNegativeStock)
                _events.emit(UiEvent.ShowMessage("Stock updated"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Stock update failed", isError = true))
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _events.emit(UiEvent.ShowMessage("${product.name} deleted"))
        }
    }

    // --- Customers ---
    fun saveCustomer(customer: CustomerEntity, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val id = repository.saveCustomer(customer)
                maybeTriggerAutoBackup()
                _events.emit(UiEvent.ShowMessage("${customer.name} saved"))
                onSaved(id)
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Failed to save customer", isError = true))
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _events.emit(UiEvent.ShowMessage("${customer.name} deleted"))
        }
    }

    fun recordCustomerPayment(
        customerId: Long,
        amount: Double,
        paymentMethod: String,
        isAdvanceDeposit: Boolean,
        note: String
    ) {
        viewModelScope.launch {
            try {
                repository.recordCustomerPayment(customerId, amount, paymentMethod, isAdvanceDeposit, note)
                maybeTriggerAutoBackup()
                _events.emit(UiEvent.ShowMessage("Payment recorded"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Payment failed", isError = true))
            }
        }
    }

    fun shareCustomerStatement(customer: CustomerEntity, viaWhatsApp: Boolean) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                val custSales = uiState.value.sales.filter { it.customerId == customer.id }
                val custPayments = uiState.value.customerPayments.filter { it.customerId == customer.id }
                PdfInvoiceGenerator.generateCustomerStatementPdf(
                    appContext, customer, custSales, custPayments, uiState.value.settings
                )
            }
            val caption = "NAQSH COLLECTION - Statement for ${customer.name} | Balance: ${IndianFormatters.formatCurrency(customer.outstandingBalance)}"
            if (viaWhatsApp) {
                when (val res = ShareHelper.shareFileToWhatsApp(appContext, file, "application/pdf", caption)) {
                    WhatsAppShareResult.Launched -> Unit
                    WhatsAppShareResult.NotInstalled -> {
                        _events.emit(UiEvent.ShowMessage("WhatsApp not installed. Opening other apps...", isError = true))
                        ShareHelper.shareFileGeneric(appContext, file, "application/pdf", "Share Statement", caption)
                    }
                    is WhatsAppShareResult.Error -> {
                        _events.emit(UiEvent.ShowMessage(res.message, isError = true))
                    }
                }
            } else {
                ShareHelper.shareFileGeneric(appContext, file, "application/pdf", "Share Statement", caption)
            }
        }
    }

    // --- Suppliers & Purchases ---
    fun saveSupplier(supplier: SupplierEntity, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.saveSupplier(supplier)
                maybeTriggerAutoBackup()
                _events.emit(UiEvent.ShowMessage("${supplier.name} saved"))
                onSaved()
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Failed to save supplier", isError = true))
            }
        }
    }

    fun deleteSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
            _events.emit(UiEvent.ShowMessage("${supplier.name} deleted"))
        }
    }

    fun recordSupplierPayment(supplierId: Long, amount: Double, method: String, note: String) {
        viewModelScope.launch {
            try {
                repository.recordSupplierPayment(supplierId, amount, method, note)
                maybeTriggerAutoBackup()
                _events.emit(UiEvent.ShowMessage("Supplier payment recorded"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Failed to record payment", isError = true))
            }
        }
    }

    fun recordPurchase(
        purchaseInvoiceNo: String,
        supplierId: Long?,
        supplierName: String,
        lines: List<PurchaseLineRequest>,
        paidAmount: Double,
        paymentMethod: String,
        purchaseDate: Long,
        notes: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.recordPurchase(
                    purchaseInvoiceNo, supplierId, supplierName, lines, paidAmount, paymentMethod, purchaseDate, notes
                )
                maybeTriggerAutoBackup()
                _events.emit(UiEvent.ShowMessage("Purchase saved & inventory updated"))
                onDone()
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Purchase failed", isError = true))
            }
        }
    }

    // --- Expenses ---
    fun addExpense(category: String, amount: Double, method: String, description: String, dateMillis: Long, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.addExpense(category, amount, method, description, dateMillis)
                maybeTriggerAutoBackup()
                _events.emit(UiEvent.ShowMessage("Expense recorded"))
                onDone()
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Failed to record expense", isError = true))
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _events.emit(UiEvent.ShowMessage("Expense deleted"))
        }
    }

    // --- Returns & Exchanges ---
    fun processReturnOrExchange(
        originalSaleId: Long,
        returnLines: List<ReturnLineRequest>,
        exchangeLines: List<ExchangeLineRequest>,
        paymentMethod: String,
        additionalPaid: Double,
        reason: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val ret = repository.processReturnOrExchange(
                    originalSaleId = originalSaleId,
                    returnLines = returnLines,
                    exchangeLines = exchangeLines,
                    paymentMethod = paymentMethod,
                    additionalPaidInput = additionalPaid,
                    allowNegativeStock = uiState.value.settings.allowNegativeStock,
                    reason = reason
                )
                maybeTriggerAutoBackup()
                _events.emit(UiEvent.ShowMessage("Processed ${ret.returnNumber}"))
                onDone()
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Return failed", isError = true))
            }
        }
    }

    // --- Invoice PDF, WhatsApp, Print ---
    suspend fun getSaleWithItems(saleId: Long): SaleWithItems? = repository.getSaleWithItems(saleId)

    fun shareInvoiceWhatsApp(saleWithItems: SaleWithItems) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                PdfInvoiceGenerator.generateInvoicePdf(appContext, saleWithItems, uiState.value.settings)
            }
            val s = saleWithItems.sale
            val caption = "Assalamu Alaikum! NAQSH COLLECTION Invoice ${s.invoiceNumber} | Total: ${IndianFormatters.formatCurrency(s.grandTotal)} | Paid: ${IndianFormatters.formatCurrency(s.paidAmount)}"
            when (val res = ShareHelper.shareFileToWhatsApp(appContext, file, "application/pdf", caption)) {
                WhatsAppShareResult.Launched -> Unit
                WhatsAppShareResult.NotInstalled -> {
                    _events.emit(UiEvent.ShowMessage("WhatsApp not found. Choose another app to share PDF.", isError = true))
                    ShareHelper.shareFileGeneric(appContext, file, "application/pdf", "Share Invoice PDF", caption)
                }
                is WhatsAppShareResult.Error -> {
                    _events.emit(UiEvent.ShowMessage(res.message, isError = true))
                }
            }
        }
    }

    fun shareInvoiceGeneric(saleWithItems: SaleWithItems) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                PdfInvoiceGenerator.generateInvoicePdf(appContext, saleWithItems, uiState.value.settings)
            }
            val s = saleWithItems.sale
            val caption = "NAQSH COLLECTION Invoice ${s.invoiceNumber} - ${IndianFormatters.formatCurrency(s.grandTotal)}"
            ShareHelper.shareFileGeneric(appContext, file, "application/pdf", "Share Invoice PDF", caption)
        }
    }

    fun requestSaveInvoicePdf(saleWithItems: SaleWithItems) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                PdfInvoiceGenerator.generateInvoicePdf(appContext, saleWithItems, uiState.value.settings)
            }
            _events.emit(UiEvent.RequestSavePdfToSaf(file, "Invoice_${saleWithItems.sale.invoiceNumber}.pdf"))
        }
    }

    fun printInvoiceSystem(saleWithItems: SaleWithItems, isDuplicate: Boolean = false) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                PdfInvoiceGenerator.generateInvoicePdf(appContext, saleWithItems, uiState.value.settings, isDuplicate)
            }
            val res = SystemPrintHelper.printPdfFile(appContext, file, "Invoice_${saleWithItems.sale.invoiceNumber}")
            res.onFailure {
                _events.emit(UiEvent.ShowMessage(it.message ?: "System print failed", isError = true))
            }
        }
    }

    fun printInvoiceBluetooth(saleWithItems: SaleWithItems, isDuplicate: Boolean = false) {
        viewModelScope.launch {
            val res = printerManager.printInvoice(saleWithItems, uiState.value.settings, isDuplicate)
            res.onSuccess {
                _events.emit(UiEvent.ShowMessage(if (isDuplicate) "Duplicate invoice printed" else "Invoice printed"))
            }.onFailure {
                _events.emit(UiEvent.ShowMessage(it.message ?: "Bluetooth print failed", isError = true))
            }
        }
    }

    // --- Bluetooth Printer Actions ---
    fun refreshPairedPrinters() = printerManager.refreshPairedDevices()
    fun startPrinterDiscovery() = printerManager.startDiscovery()
    fun stopPrinterDiscovery() = printerManager.stopDiscovery()

    fun connectPrinter(address: String, name: String) {
        viewModelScope.launch {
            val res = printerManager.connect(address, name)
            res.onSuccess {
                preferences.setLastPrinter(address, name)
                _events.emit(UiEvent.ShowMessage("Connected to $name"))
            }.onFailure {
                _events.emit(UiEvent.ShowMessage(it.message ?: "Connection failed", isError = true))
            }
        }
    }

    fun reconnectLastPrinter() {
        val s = uiState.value.settings
        if (s.lastPrinterAddress.isBlank()) {
            viewModelScope.launch { _events.emit(UiEvent.ShowMessage("No previous printer saved", isError = true)) }
            return
        }
        connectPrinter(s.lastPrinterAddress, s.lastPrinterName.ifBlank { s.lastPrinterAddress })
    }

    fun disconnectPrinter() = printerManager.disconnect()

    fun testPrint() {
        viewModelScope.launch {
            val res = printerManager.printTestReceipt(uiState.value.settings)
            res.onSuccess {
                _events.emit(UiEvent.ShowMessage("Test receipt printed successfully"))
            }.onFailure {
                _events.emit(UiEvent.ShowMessage(it.message ?: "Test print failed", isError = true))
            }
        }
    }

    // --- Report Exports ---
    fun exportReport(
        reportTitle: String,
        fileBaseName: String,
        summaryLines: List<String>,
        headers: List<String>,
        rows: List<List<String>>,
        asCsv: Boolean,
        shareToWhatsApp: Boolean = false,
        saveViaSaf: Boolean = false
    ) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                if (asCsv) {
                    ShareHelper.writeCsvFile(
                        appContext,
                        "${fileBaseName}_${IndianFormatters.formatFileTimestamp()}.csv",
                        headers,
                        rows
                    )
                } else {
                    PdfInvoiceGenerator.generateTabularReportPdf(
                        appContext,
                        "${fileBaseName}_${IndianFormatters.formatFileTimestamp()}.pdf",
                        reportTitle,
                        summaryLines,
                        headers,
                        rows,
                        uiState.value.settings
                    )
                }
            }
            val mime = if (asCsv) "text/csv" else "application/pdf"
            when {
                saveViaSaf -> _events.emit(UiEvent.RequestSavePdfToSaf(file, file.name))
                shareToWhatsApp -> {
                    when (val res = ShareHelper.shareFileToWhatsApp(appContext, file, mime, "$reportTitle - NAQSH COLLECTION")) {
                        WhatsAppShareResult.Launched -> Unit
                        WhatsAppShareResult.NotInstalled -> {
                            _events.emit(UiEvent.ShowMessage("WhatsApp not found. Opening other apps...", isError = true))
                            ShareHelper.shareFileGeneric(appContext, file, mime, "Share Report")
                        }
                        is WhatsAppShareResult.Error -> _events.emit(UiEvent.ShowMessage(res.message, isError = true))
                    }
                }
                else -> ShareHelper.shareFileGeneric(appContext, file, mime, "Share $reportTitle")
            }
        }
    }

    fun printReportSystem(
        reportTitle: String,
        summaryLines: List<String>,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                PdfInvoiceGenerator.generateTabularReportPdf(
                    appContext,
                    "Print_${reportTitle.replace(" ", "_")}.pdf",
                    reportTitle,
                    summaryLines,
                    headers,
                    rows,
                    uiState.value.settings
                )
            }
            SystemPrintHelper.printPdfFile(appContext, file, reportTitle).onFailure {
                _events.emit(UiEvent.ShowMessage(it.message ?: "Print failed", isError = true))
            }
        }
    }

    // --- Backup & Restore ---
    fun refreshLocalBackups() {
        _localBackups.value = ShareHelper.listLocalBackups(appContext)
    }

    fun createLocalBackupNow() {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) { repository.exportBackupJson() }
                withContext(Dispatchers.IO) { ShareHelper.saveLocalBackupFile(appContext, json) }
                preferences.setLastBackupTimestamp(System.currentTimeMillis())
                refreshLocalBackups()
                _events.emit(UiEvent.ShowMessage("Local backup created & verified"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Backup failed", isError = true))
            }
        }
    }

    fun exportBackupToPhoneStorage() {
        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) { repository.exportBackupJson() }
                val fileName = "NaqshCollection_Backup_${IndianFormatters.formatFileTimestamp()}.naqshbackup"
                _events.emit(UiEvent.RequestSaveBackupToSaf(json, fileName))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Export failed", isError = true))
            }
        }
    }

    fun onBackupSavedToSaf(success: Boolean) {
        viewModelScope.launch {
            if (success) {
                preferences.setLastBackupTimestamp(System.currentTimeMillis())
                _events.emit(UiEvent.ShowMessage("Backup exported to phone storage"))
            } else {
                _events.emit(UiEvent.ShowMessage("Failed to save backup file", isError = true))
            }
        }
    }

    fun restoreFromSafUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) { ShareHelper.readTextFromSafUri(appContext, uri) }
                    ?: throw IllegalStateException("Cannot read selected backup file")
                withContext(Dispatchers.IO) { repository.restoreFromBackupJson(text) }
                clearCart()
                _events.emit(UiEvent.ShowMessage("Database restored & checksum verified!"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Restore failed", isError = true))
            }
        }
    }

    fun restoreFromLocalBackup(file: File) {
        viewModelScope.launch {
            try {
                val text = withContext(Dispatchers.IO) { file.readText(Charsets.UTF_8) }
                withContext(Dispatchers.IO) { repository.restoreFromBackupJson(text) }
                clearCart()
                _events.emit(UiEvent.ShowMessage("Restored from ${file.name}"))
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage(e.message ?: "Restore failed", isError = true))
            }
        }
    }

    private fun maybeTriggerAutoBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            val s = preferences.settingsFlow.first()
            if (!s.autoBackupEnabled) return@launch
            val sixHoursMs = 6L * 60L * 60L * 1000L
            if (System.currentTimeMillis() - s.lastBackupTimestamp >= sixHoursMs) {
                try {
                    val json = repository.exportBackupJson()
                    ShareHelper.saveLocalBackupFile(appContext, json)
                    preferences.setLastBackupTimestamp(System.currentTimeMillis())
                    refreshLocalBackups()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun emitMessage(msg: String, isError: Boolean = false) {
        viewModelScope.launch { _events.emit(UiEvent.ShowMessage(msg, isError)) }
    }

    private data class CoreTuple(
        val settings: BoutiqueSettings,
        val products: List<ProductEntity>,
        val customers: List<CustomerEntity>,
        val suppliers: List<SupplierEntity>,
        val sales: List<SaleEntity>
    )

    private data class LedgerTuple(
        val customerPayments: List<CustomerPaymentEntity>,
        val purchases: List<PurchaseEntity>,
        val supplierPayments: List<SupplierPaymentEntity>,
        val expenses: List<ExpenseEntity>,
        val saleReturns: List<SaleReturnEntity>
    )

    private data class DetailTuple(
        val stockMovements: List<StockMovementEntity>,
        val saleItems: List<SaleItemEntity>,
        val purchaseItems: List<PurchaseItemEntity>,
        val returnItems: List<SaleReturnItemEntity>
    )
}

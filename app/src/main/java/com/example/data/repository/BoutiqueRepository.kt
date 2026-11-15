package com.example.data.repository

import androidx.room.withTransaction
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
import com.example.util.IndianFormatters
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min

data class CartLineRequest(
    val productId: Long,
    val quantity: Int,
    val unitSellingPrice: Double,
    val itemDiscount: Double
)

data class PurchaseLineRequest(
    val productId: Long,
    val quantity: Int,
    val unitPurchasePrice: Double
)

data class ReturnLineRequest(
    val saleItemId: Long,
    val productId: Long,
    val returnQuantity: Int
)

data class ExchangeLineRequest(
    val productId: Long,
    val quantity: Int,
    val unitSellingPrice: Double
)

data class SaleWithItems(
    val sale: SaleEntity,
    val items: List<SaleItemEntity>
)

data class PurchaseWithItems(
    val purchase: PurchaseEntity,
    val items: List<PurchaseItemEntity>
)

data class AccountingSummary(
    val netSales: Double = 0.0,
    val grossBilledSales: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val costOfGoodsSold: Double = 0.0,
    val grossProfit: Double = 0.0,
    val operatingExpenses: Double = 0.0,
    val netProfit: Double = 0.0,
    val cashReceived: Double = 0.0,
    val upiReceived: Double = 0.0,
    val bankReceived: Double = 0.0,
    val otherReceived: Double = 0.0,
    val totalCollections: Double = 0.0,
    val creditSalesAmount: Double = 0.0,
    val customerOutstanding: Double = 0.0,
    val supplierOutstanding: Double = 0.0,
    val cashBalance: Double = 0.0,
    val dailyClosingBalance: Double = 0.0,
    val stockCostValue: Double = 0.0,
    val stockRetailValue: Double = 0.0
)

class BoutiqueRepository(
    private val db: NaqshDatabase,
    private val preferences: BoutiquePreferences
) {
    val products: Flow<List<ProductEntity>> = db.productDao().observeAll()
    val stockMovements: Flow<List<StockMovementEntity>> = db.stockMovementDao().observeAll()
    val customers: Flow<List<CustomerEntity>> = db.customerDao().observeAll()
    val customerPayments: Flow<List<CustomerPaymentEntity>> = db.customerPaymentDao().observeAll()
    val suppliers: Flow<List<SupplierEntity>> = db.supplierDao().observeAll()
    val supplierPayments: Flow<List<SupplierPaymentEntity>> = db.supplierPaymentDao().observeAll()
    val sales: Flow<List<SaleEntity>> = db.saleDao().observeAll()
    val saleItems: Flow<List<SaleItemEntity>> = db.saleItemDao().observeAll()
    val purchases: Flow<List<PurchaseEntity>> = db.purchaseDao().observeAll()
    val purchaseItems: Flow<List<PurchaseItemEntity>> = db.purchaseItemDao().observeAll()
    val expenses: Flow<List<ExpenseEntity>> = db.expenseDao().observeAll()
    val saleReturns: Flow<List<SaleReturnEntity>> = db.saleReturnDao().observeAll()
    val saleReturnItems: Flow<List<SaleReturnItemEntity>> = db.saleReturnItemDao().observeAll()

    // --- Products ---
    suspend fun saveProduct(product: ProductEntity): Long {
        val now = System.currentTimeMillis()
        return db.withTransaction {
            val cleanSku = product.sku.trim().ifBlank {
                "NC-${now.toString().takeLast(6)}"
            }
            if (product.id == 0L) {
                val newId = db.productDao().insert(
                    product.copy(sku = cleanSku, dateAdded = now, updatedAt = now)
                )
                if (product.quantity != 0) {
                    db.stockMovementDao().insert(
                        StockMovementEntity(
                            productId = newId,
                            productName = product.name,
                            type = "OPENING",
                            quantityChange = product.quantity,
                            previousQuantity = 0,
                            newQuantity = product.quantity,
                            referenceNo = cleanSku,
                            note = "Opening stock",
                            timestamp = now
                        )
                    )
                }
                newId
            } else {
                val existing = db.productDao().getById(product.id)
                db.productDao().update(product.copy(sku = cleanSku, updatedAt = now))
                if (existing != null && existing.quantity != product.quantity) {
                    db.stockMovementDao().insert(
                        StockMovementEntity(
                            productId = product.id,
                            productName = product.name,
                            type = "ADJUSTMENT",
                            quantityChange = product.quantity - existing.quantity,
                            previousQuantity = existing.quantity,
                            newQuantity = product.quantity,
                            referenceNo = cleanSku,
                            note = "Manual product edit",
                            timestamp = now
                        )
                    )
                }
                product.id
            }
        }
    }

    suspend fun adjustStock(
        productId: Long,
        delta: Int,
        note: String,
        allowNegativeStock: Boolean
    ) {
        if (delta == 0) return
        val now = System.currentTimeMillis()
        db.withTransaction {
            val product = db.productDao().getById(productId)
                ?: throw IllegalArgumentException("Product not found")
            val newQty = product.quantity + delta
            if (newQty < 0 && !allowNegativeStock) {
                throw IllegalStateException("Stock cannot go below 0 for ${product.name}")
            }
            db.productDao().update(product.copy(quantity = newQty, updatedAt = now))
            db.stockMovementDao().insert(
                StockMovementEntity(
                    productId = product.id,
                    productName = product.name,
                    type = "ADJUSTMENT",
                    quantityChange = delta,
                    previousQuantity = product.quantity,
                    newQuantity = newQty,
                    referenceNo = product.sku,
                    note = note.ifBlank { "Manual adjustment" },
                    timestamp = now
                )
            )
        }
    }

    suspend fun deleteProduct(product: ProductEntity) {
        db.productDao().delete(product)
    }

    // --- Customers ---
    suspend fun saveCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) {
            db.customerDao().insert(customer.copy(createdAt = System.currentTimeMillis()))
        } else {
            db.customerDao().update(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity) {
        db.customerDao().delete(customer)
    }

    suspend fun recordCustomerPayment(
        customerId: Long,
        amount: Double,
        paymentMethod: String,
        isAdvanceDeposit: Boolean,
        note: String
    ) {
        require(amount > 0) { "Amount must be greater than zero" }
        val now = System.currentTimeMillis()
        db.withTransaction {
            val customer = db.customerDao().getById(customerId)
                ?: throw IllegalArgumentException("Customer not found")
            if (isAdvanceDeposit) {
                db.customerDao().update(
                    customer.copy(
                        advanceBalance = customer.advanceBalance + amount,
                        totalPayments = customer.totalPayments + amount
                    )
                )
                db.customerPaymentDao().insert(
                    CustomerPaymentEntity(
                        customerId = customer.id,
                        customerName = customer.name,
                        amount = amount,
                        paymentMethod = paymentMethod,
                        type = "ADVANCE_DEPOSIT",
                        note = note.ifBlank { "Advance deposit" },
                        timestamp = now
                    )
                )
            } else {
                val appliedToDue = min(customer.outstandingBalance, amount)
                val leftoverAdvance = max(0.0, amount - appliedToDue)
                db.customerDao().update(
                    customer.copy(
                        outstandingBalance = max(0.0, customer.outstandingBalance - appliedToDue),
                        advanceBalance = customer.advanceBalance + leftoverAdvance,
                        totalPayments = customer.totalPayments + amount
                    )
                )
                db.customerPaymentDao().insert(
                    CustomerPaymentEntity(
                        customerId = customer.id,
                        customerName = customer.name,
                        amount = amount,
                        paymentMethod = paymentMethod,
                        type = "DUE_RECOVERY",
                        note = note.ifBlank { "Outstanding due payment" },
                        timestamp = now
                    )
                )
            }
        }
    }

    // --- Suppliers ---
    suspend fun saveSupplier(supplier: SupplierEntity): Long {
        return if (supplier.id == 0L) {
            db.supplierDao().insert(supplier.copy(createdAt = System.currentTimeMillis()))
        } else {
            db.supplierDao().update(supplier)
            supplier.id
        }
    }

    suspend fun deleteSupplier(supplier: SupplierEntity) {
        db.supplierDao().delete(supplier)
    }

    suspend fun recordSupplierPayment(
        supplierId: Long,
        amount: Double,
        paymentMethod: String,
        note: String
    ) {
        require(amount > 0) { "Amount must be greater than zero" }
        val now = System.currentTimeMillis()
        db.withTransaction {
            val supplier = db.supplierDao().getById(supplierId)
                ?: throw IllegalArgumentException("Supplier not found")
            db.supplierDao().update(
                supplier.copy(
                    totalPayments = supplier.totalPayments + amount,
                    outstandingAmount = max(0.0, supplier.outstandingAmount - amount)
                )
            )
            db.supplierPaymentDao().insert(
                SupplierPaymentEntity(
                    supplierId = supplier.id,
                    supplierName = supplier.name,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    note = note.ifBlank { "Supplier due payment" },
                    timestamp = now
                )
            )
        }
    }

    // --- POS Sale Creation ---
    suspend fun generateUniqueInvoiceNumber(): String {
        val dateCode = IndianFormatters.formatInvoiceDateCode()
        repeat(20) {
            val seq = preferences.nextInvoiceSequence()
            val candidate = "NC-$dateCode-${seq.toString().padStart(4, '0')}"
            if (db.saleDao().countByInvoiceNumber(candidate) == 0) {
                return candidate
            }
        }
        return "NC-$dateCode-${System.currentTimeMillis().toString().takeLast(5)}"
    }

    suspend fun createSale(
        invoiceNumberInput: String?,
        customerId: Long?,
        walkInName: String,
        walkInMobile: String,
        lines: List<CartLineRequest>,
        overallDiscountInput: Double,
        paymentMethod: String,
        tenderedAmount: Double,
        useAdvanceBalance: Boolean,
        allowNegativeStock: Boolean,
        notes: String
    ): SaleWithItems {
        require(lines.isNotEmpty()) { "Cart cannot be empty" }
        val now = System.currentTimeMillis()
        val finalInvoiceNo = invoiceNumberInput?.trim()?.takeIf { it.isNotEmpty() }
            ?: generateUniqueInvoiceNumber()

        return db.withTransaction {
            if (db.saleDao().countByInvoiceNumber(finalInvoiceNo) > 0) {
                throw IllegalStateException("Duplicate invoice number: $finalInvoiceNo")
            }

            val customer = customerId?.let { db.customerDao().getById(it) }
            val resolvedName = customer?.name ?: walkInName.ifBlank { "Walk-in Customer" }
            val resolvedMobile = customer?.mobile ?: walkInMobile.trim()

            var subtotal = 0.0
            var itemDiscountTotal = 0.0
            var totalCostOfGoods = 0.0
            val pendingSaleItems = mutableListOf<SaleItemEntity>()

            for (line in lines) {
                require(line.quantity > 0) { "Quantity must be positive" }
                val product = db.productDao().getById(line.productId)
                    ?: throw IllegalArgumentException("Product not found")
                if (!allowNegativeStock && product.quantity < line.quantity) {
                    throw IllegalStateException("Insufficient stock for ${product.name} (Available: ${product.quantity})")
                }

                val grossLine = line.unitSellingPrice * line.quantity
                val cappedItemDiscount = line.itemDiscount.coerceIn(0.0, grossLine)
                val lineTotal = grossLine - cappedItemDiscount
                val lineCost = product.purchasePrice * line.quantity

                subtotal += grossLine
                itemDiscountTotal += cappedItemDiscount
                totalCostOfGoods += lineCost

                val newQty = product.quantity - line.quantity
                db.productDao().update(product.copy(quantity = newQty, updatedAt = now))
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        productId = product.id,
                        productName = product.name,
                        type = "SALE",
                        quantityChange = -line.quantity,
                        previousQuantity = product.quantity,
                        newQuantity = newQty,
                        referenceNo = finalInvoiceNo,
                        note = "Sold to $resolvedName",
                        timestamp = now
                    )
                )

                pendingSaleItems.add(
                    SaleItemEntity(
                        saleId = 0L,
                        productId = product.id,
                        sku = product.sku,
                        productName = product.name,
                        size = product.size,
                        color = product.color,
                        quantity = line.quantity,
                        returnedQuantity = 0,
                        unitCostPrice = product.purchasePrice,
                        unitSellingPrice = line.unitSellingPrice,
                        itemDiscount = cappedItemDiscount,
                        lineTotal = lineTotal
                    )
                )
            }

            val afterItemDiscount = max(0.0, subtotal - itemDiscountTotal)
            val cappedOverallDiscount = overallDiscountInput.coerceIn(0.0, afterItemDiscount)
            val grandTotal = max(0.0, afterItemDiscount - cappedOverallDiscount)

            val advanceUsed = if (customer != null && useAdvanceBalance && customer.advanceBalance > 0.0) {
                min(customer.advanceBalance, grandTotal)
            } else {
                0.0
            }
            val netAfterAdvance = max(0.0, grandTotal - advanceUsed)

            val effectivePaid = if (paymentMethod == "CREDIT") {
                0.0
            } else {
                min(max(0.0, tenderedAmount), netAfterAdvance)
            }
            val changeReturned = if (paymentMethod == "CREDIT") {
                0.0
            } else {
                max(0.0, tenderedAmount - netAfterAdvance)
            }
            val remainingBalance = max(0.0, netAfterAdvance - effectivePaid)

            if (remainingBalance > 0.01 && customer == null) {
                throw IllegalStateException("Please select or add a Customer for Credit / Partial Payment sales")
            }

            val saleEntity = SaleEntity(
                invoiceNumber = finalInvoiceNo,
                customerId = customer?.id,
                customerName = resolvedName,
                customerMobile = resolvedMobile,
                subtotal = subtotal,
                itemDiscountTotal = itemDiscountTotal,
                overallDiscount = cappedOverallDiscount,
                grandTotal = grandTotal,
                totalCostOfGoods = totalCostOfGoods,
                paidAmount = effectivePaid,
                advanceUsed = advanceUsed,
                remainingBalance = remainingBalance,
                changeReturned = changeReturned,
                paymentMethod = paymentMethod,
                notes = notes.trim(),
                timestamp = now,
                hasReturn = false
            )

            val saleId = db.saleDao().insert(saleEntity)
            val savedItems = pendingSaleItems.map { it.copy(saleId = saleId) }
            db.saleItemDao().insertAll(savedItems)

            if (effectivePaid > 0.0) {
                db.customerPaymentDao().insert(
                    CustomerPaymentEntity(
                        customerId = customer?.id ?: 0L,
                        customerName = resolvedName,
                        saleId = saleId,
                        invoiceNumber = finalInvoiceNo,
                        amount = effectivePaid,
                        paymentMethod = paymentMethod,
                        type = "BILL_PAYMENT",
                        note = "Invoice $finalInvoiceNo",
                        timestamp = now
                    )
                )
            }
            if (advanceUsed > 0.0 && customer != null) {
                db.customerPaymentDao().insert(
                    CustomerPaymentEntity(
                        customerId = customer.id,
                        customerName = customer.name,
                        saleId = saleId,
                        invoiceNumber = finalInvoiceNo,
                        amount = advanceUsed,
                        paymentMethod = "ADVANCE_USED",
                        type = "ADVANCE_USED",
                        note = "Advance applied to $finalInvoiceNo",
                        timestamp = now
                    )
                )
            }

            if (customer != null) {
                db.customerDao().update(
                    customer.copy(
                        totalPurchases = customer.totalPurchases + grandTotal,
                        totalPayments = customer.totalPayments + effectivePaid,
                        advanceBalance = max(0.0, customer.advanceBalance - advanceUsed),
                        outstandingBalance = customer.outstandingBalance + remainingBalance
                    )
                )
            }

            val persistedItems = db.saleItemDao().getForSale(saleId)
            SaleWithItems(saleEntity.copy(id = saleId), persistedItems)
        }
    }

    suspend fun getSaleWithItems(saleId: Long): SaleWithItems? {
        val sale = db.saleDao().getById(saleId) ?: return null
        val items = db.saleItemDao().getForSale(saleId)
        return SaleWithItems(sale, items)
    }

    suspend fun findSaleByInvoice(invoiceNumber: String): SaleWithItems? {
        val sale = db.saleDao().getByInvoiceNumber(invoiceNumber.trim()) ?: return null
        val items = db.saleItemDao().getForSale(sale.id)
        return SaleWithItems(sale, items)
    }

    // --- Purchases ---
    suspend fun recordPurchase(
        purchaseInvoiceNo: String,
        supplierId: Long?,
        supplierNameInput: String,
        lines: List<PurchaseLineRequest>,
        paidAmountInput: Double,
        paymentMethod: String,
        purchaseDate: Long,
        notes: String
    ): PurchaseWithItems {
        require(lines.isNotEmpty()) { "Add at least one product to record a purchase" }
        val now = System.currentTimeMillis()
        val invoiceNo = purchaseInvoiceNo.trim().ifBlank {
            "PUR-${IndianFormatters.formatInvoiceDateCode(purchaseDate)}-${now.toString().takeLast(4)}"
        }

        return db.withTransaction {
            val supplier = supplierId?.let { db.supplierDao().getById(it) }
            val supplierName = supplier?.name ?: supplierNameInput.ifBlank { "Direct Supplier" }
            var totalAmount = 0.0
            val purchaseItems = mutableListOf<PurchaseItemEntity>()

            for (line in lines) {
                require(line.quantity > 0) { "Purchase quantity must be positive" }
                val product = db.productDao().getById(line.productId)
                    ?: throw IllegalArgumentException("Product not found")
                val lineTotal = line.quantity * line.unitPurchasePrice
                totalAmount += lineTotal

                val newQty = product.quantity + line.quantity
                db.productDao().update(
                    product.copy(
                        quantity = newQty,
                        purchasePrice = line.unitPurchasePrice,
                        supplierId = supplier?.id ?: product.supplierId,
                        supplierName = supplierName,
                        updatedAt = now
                    )
                )
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        productId = product.id,
                        productName = product.name,
                        type = "PURCHASE",
                        quantityChange = line.quantity,
                        previousQuantity = product.quantity,
                        newQuantity = newQty,
                        referenceNo = invoiceNo,
                        note = "Purchased from $supplierName",
                        timestamp = purchaseDate
                    )
                )
                purchaseItems.add(
                    PurchaseItemEntity(
                        purchaseId = 0L,
                        productId = product.id,
                        productName = product.name,
                        quantity = line.quantity,
                        unitPurchasePrice = line.unitPurchasePrice,
                        lineTotal = lineTotal
                    )
                )
            }

            val paidAmount = paidAmountInput.coerceIn(0.0, totalAmount)
            val remainingBalance = max(0.0, totalAmount - paidAmount)

            val purchase = PurchaseEntity(
                purchaseInvoiceNo = invoiceNo,
                supplierId = supplier?.id,
                supplierName = supplierName,
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                remainingBalance = remainingBalance,
                paymentMethod = paymentMethod,
                notes = notes.trim(),
                purchaseDate = purchaseDate
            )
            val purchaseId = db.purchaseDao().insert(purchase)
            val savedItems = purchaseItems.map { it.copy(purchaseId = purchaseId) }
            db.purchaseItemDao().insertAll(savedItems)

            if (supplier != null) {
                db.supplierDao().update(
                    supplier.copy(
                        totalPurchases = supplier.totalPurchases + totalAmount,
                        totalPayments = supplier.totalPayments + paidAmount,
                        outstandingAmount = supplier.outstandingAmount + remainingBalance
                    )
                )
                if (paidAmount > 0.0) {
                    db.supplierPaymentDao().insert(
                        SupplierPaymentEntity(
                            supplierId = supplier.id,
                            supplierName = supplier.name,
                            purchaseId = purchaseId,
                            purchaseInvoiceNo = invoiceNo,
                            amount = paidAmount,
                            paymentMethod = paymentMethod,
                            note = "Purchase $invoiceNo",
                            timestamp = purchaseDate
                        )
                    )
                }
            } else if (paidAmount > 0.0) {
                // Record cash/bank outflow even for walk-in/direct supplier so cash balance stays exact
                db.supplierPaymentDao().insert(
                    SupplierPaymentEntity(
                        supplierId = 0L,
                        supplierName = supplierName,
                        purchaseId = purchaseId,
                        purchaseInvoiceNo = invoiceNo,
                        amount = paidAmount,
                        paymentMethod = paymentMethod,
                        note = "Direct purchase $invoiceNo",
                        timestamp = purchaseDate
                    )
                )
            }

            PurchaseWithItems(purchase.copy(id = purchaseId), savedItems)
        }
    }

    // --- Expenses ---
    suspend fun addExpense(
        category: String,
        amount: Double,
        paymentMethod: String,
        description: String,
        dateMillis: Long
    ): Long {
        require(amount > 0) { "Expense amount must be positive" }
        return db.expenseDao().insert(
            ExpenseEntity(
                category = category.trim().ifBlank { "Miscellaneous" },
                amount = amount,
                paymentMethod = paymentMethod,
                description = description.trim(),
                expenseDate = dateMillis
            )
        )
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        db.expenseDao().delete(expense)
    }

    // --- Sales Returns & Exchanges ---
    suspend fun processReturnOrExchange(
        originalSaleId: Long,
        returnLines: List<ReturnLineRequest>,
        exchangeLines: List<ExchangeLineRequest>,
        paymentMethod: String, // CASH, UPI, BANK_TRANSFER, OTHER, or ADVANCE
        additionalPaidInput: Double,
        allowNegativeStock: Boolean,
        reason: String
    ): SaleReturnEntity {
        val activeReturns = returnLines.filter { it.returnQuantity > 0 }
        require(activeReturns.isNotEmpty()) { "Select at least one item to return" }

        val now = System.currentTimeMillis()
        val returnSeq = preferences.nextReturnSequence()
        val returnNo = "RET-${IndianFormatters.formatInvoiceDateCode(now)}-${returnSeq.toString().padStart(4, '0')}"

        return db.withTransaction {
            val sale = db.saleDao().getById(originalSaleId)
                ?: throw IllegalArgumentException("Original invoice not found")
            val saleItems = db.saleItemDao().getForSale(originalSaleId)
            val saleItemsMap = saleItems.associateBy { it.id }

            // Ratio for prorating overall invoice discount on returned items
            val totalAfterItemDiscount = max(1.0, sale.subtotal - sale.itemDiscountTotal)
            val overallDiscountRatio = (sale.grandTotal / totalAfterItemDiscount).coerceIn(0.0, 1.0)

            var returnedAmount = 0.0
            var returnedCogs = 0.0
            val pendingReturnItems = mutableListOf<SaleReturnItemEntity>()

            for (ret in activeReturns) {
                val origItem = saleItemsMap[ret.saleItemId]
                    ?: throw IllegalArgumentException("Original sale item not found")
                val returnableQty = origItem.quantity - origItem.returnedQuantity
                if (ret.returnQuantity > returnableQty) {
                    throw IllegalStateException("Cannot return ${ret.returnQuantity} of ${origItem.productName} (Returnable: $returnableQty)")
                }

                val perUnitNet = (origItem.lineTotal / origItem.quantity) * overallDiscountRatio
                val lineReturnValue = perUnitNet * ret.returnQuantity
                val lineReturnCost = origItem.unitCostPrice * ret.returnQuantity

                returnedAmount += lineReturnValue
                returnedCogs += lineReturnCost

                db.saleItemDao().update(
                    origItem.copy(returnedQuantity = origItem.returnedQuantity + ret.returnQuantity)
                )

                val product = db.productDao().getById(origItem.productId)
                if (product != null) {
                    val newQty = product.quantity + ret.returnQuantity
                    db.productDao().update(product.copy(quantity = newQty, updatedAt = now))
                    db.stockMovementDao().insert(
                        StockMovementEntity(
                            productId = product.id,
                            productName = product.name,
                            type = "RETURN",
                            quantityChange = ret.returnQuantity,
                            previousQuantity = product.quantity,
                            newQuantity = newQty,
                            referenceNo = returnNo,
                            note = "Return from ${sale.invoiceNumber}",
                            timestamp = now
                        )
                    )
                }

                pendingReturnItems.add(
                    SaleReturnItemEntity(
                        returnId = 0L,
                        saleItemId = origItem.id,
                        productId = origItem.productId,
                        productName = origItem.productName,
                        isExchangeItem = false,
                        quantity = ret.returnQuantity,
                        unitPrice = perUnitNet,
                        unitCostPrice = origItem.unitCostPrice,
                        lineTotal = lineReturnValue
                    )
                )
            }

            var exchangeTotal = 0.0
            var exchangeCogs = 0.0

            for (ex in exchangeLines.filter { it.quantity > 0 }) {
                val product = db.productDao().getById(ex.productId)
                    ?: throw IllegalArgumentException("Exchange product not found")
                if (!allowNegativeStock && product.quantity < ex.quantity) {
                    throw IllegalStateException("Insufficient stock for exchange item ${product.name}")
                }
                val lineTotal = ex.unitSellingPrice * ex.quantity
                val lineCost = product.purchasePrice * ex.quantity
                exchangeTotal += lineTotal
                exchangeCogs += lineCost

                val newQty = product.quantity - ex.quantity
                db.productDao().update(product.copy(quantity = newQty, updatedAt = now))
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        productId = product.id,
                        productName = product.name,
                        type = "EXCHANGE_OUT",
                        quantityChange = -ex.quantity,
                        previousQuantity = product.quantity,
                        newQuantity = newQty,
                        referenceNo = returnNo,
                        note = "Exchange against ${sale.invoiceNumber}",
                        timestamp = now
                    )
                )

                pendingReturnItems.add(
                    SaleReturnItemEntity(
                        returnId = 0L,
                        saleItemId = null,
                        productId = product.id,
                        productName = product.name,
                        isExchangeItem = true,
                        quantity = ex.quantity,
                        unitPrice = ex.unitSellingPrice,
                        unitCostPrice = product.purchasePrice,
                        lineTotal = lineTotal
                    )
                )
            }

            val netDifference = exchangeTotal - returnedAmount
            val customer = sale.customerId?.let { db.customerDao().getById(it) }
            val alreadyAdjustedOnSale = db.saleReturnDao().getTotalCreditAdjustedForSale(sale.id)
            val remainingSaleCredit = max(0.0, sale.remainingBalance - alreadyAdjustedOnSale)

            var creditAdjusted = 0.0
            var refundPaid = 0.0
            var additionalPaid = 0.0

            if (netDifference < 0.0) {
                val creditBack = -netDifference
                creditAdjusted = min(remainingSaleCredit, creditBack)
                val cashOrAdvanceRefund = max(0.0, creditBack - creditAdjusted)

                if (customer != null) {
                    val newOutstanding = max(0.0, customer.outstandingBalance - creditAdjusted)
                    val newAdvance = if (paymentMethod == "ADVANCE") {
                        customer.advanceBalance + cashOrAdvanceRefund
                    } else {
                        customer.advanceBalance
                    }
                    db.customerDao().update(
                        customer.copy(
                            totalPurchases = max(0.0, customer.totalPurchases - creditBack),
                            outstandingBalance = newOutstanding,
                            advanceBalance = newAdvance
                        )
                    )
                }
                refundPaid = if (paymentMethod == "ADVANCE") 0.0 else cashOrAdvanceRefund
            } else if (netDifference > 0.0) {
                additionalPaid = min(max(0.0, additionalPaidInput), netDifference)
                val addedCredit = max(0.0, netDifference - additionalPaid)
                if (addedCredit > 0.01 && customer == null) {
                    throw IllegalStateException("Walk-in customer must pay full exchange difference")
                }
                if (customer != null) {
                    db.customerDao().update(
                        customer.copy(
                            totalPurchases = customer.totalPurchases + netDifference,
                            totalPayments = customer.totalPayments + additionalPaid,
                            outstandingBalance = customer.outstandingBalance + addedCredit
                        )
                    )
                }
            }

            // Preserve original financial numbers on SaleEntity; only mark hasReturn = true
            if (!sale.hasReturn) {
                db.saleDao().update(sale.copy(hasReturn = true))
            }

            val saleReturn = SaleReturnEntity(
                returnNumber = returnNo,
                originalSaleId = sale.id,
                originalInvoiceNo = sale.invoiceNumber,
                customerId = sale.customerId,
                customerName = sale.customerName,
                returnedAmount = returnedAmount,
                returnedCostOfGoods = returnedCogs,
                exchangeTotal = exchangeTotal,
                exchangeCostOfGoods = exchangeCogs,
                netDifference = netDifference,
                refundPaidAmount = refundPaid,
                additionalPaidAmount = additionalPaid,
                creditAdjustedAmount = creditAdjusted,
                paymentMethod = paymentMethod,
                reason = reason.trim(),
                timestamp = now
            )

            val returnId = db.saleReturnDao().insert(saleReturn)
            db.saleReturnItemDao().insertAll(pendingReturnItems.map { it.copy(returnId = returnId) })
            saleReturn.copy(id = returnId)
        }
    }

    // --- Pure Accounting Engine ---
    fun computeAccountingSummary(
        allProducts: List<ProductEntity>,
        allCustomers: List<CustomerEntity>,
        allSuppliers: List<SupplierEntity>,
        allSales: List<SaleEntity>,
        allCustomerPayments: List<CustomerPaymentEntity>,
        allPurchases: List<PurchaseEntity>,
        allSupplierPayments: List<SupplierPaymentEntity>,
        allExpenses: List<ExpenseEntity>,
        allReturns: List<SaleReturnEntity>,
        fromMillis: Long = 0L,
        toMillis: Long = Long.MAX_VALUE
    ): AccountingSummary {
        val salesInRange = allSales.filter { it.timestamp in fromMillis..toMillis }
        val paymentsInRange = allCustomerPayments.filter { it.timestamp in fromMillis..toMillis }
        val purchasesInRange = allPurchases.filter { it.purchaseDate in fromMillis..toMillis }
        val supplierPaymentsInRange = allSupplierPayments.filter { it.timestamp in fromMillis..toMillis }
        val expensesInRange = allExpenses.filter { it.expenseDate in fromMillis..toMillis }
        val returnsInRange = allReturns.filter { it.timestamp in fromMillis..toMillis }

        val grossBilled = salesInRange.sumOf { it.grandTotal }
        val netSales = grossBilled - returnsInRange.sumOf { it.returnedAmount } + returnsInRange.sumOf { it.exchangeTotal }

        val cogs = salesInRange.sumOf { it.totalCostOfGoods } -
            returnsInRange.sumOf { it.returnedCostOfGoods } +
            returnsInRange.sumOf { it.exchangeCostOfGoods }

        val grossProfit = netSales - cogs
        val operatingExpenses = expensesInRange.sumOf { it.amount }
        val netProfit = grossProfit - operatingExpenses

        // Collections exclude ADVANCE_USED to prevent double counting (advance was counted on deposit)
        val realPayments = paymentsInRange.filter { it.type != "ADVANCE_USED" }
        var cashRec = realPayments.filter { it.paymentMethod == "CASH" }.sumOf { it.amount }
        var upiRec = realPayments.filter { it.paymentMethod == "UPI" }.sumOf { it.amount }
        var bankRec = realPayments.filter { it.paymentMethod == "BANK_TRANSFER" }.sumOf { it.amount }
        var otherRec = realPayments.filter { it.paymentMethod == "OTHER" }.sumOf { it.amount }

        // Adjust collections with exchange inflows and cash/UPI/bank refunds
        for (ret in returnsInRange) {
            when (ret.paymentMethod) {
                "CASH" -> cashRec += ret.additionalPaidAmount - ret.refundPaidAmount
                "UPI" -> upiRec += ret.additionalPaidAmount - ret.refundPaidAmount
                "BANK_TRANSFER" -> bankRec += ret.additionalPaidAmount - ret.refundPaidAmount
                "OTHER" -> otherRec += ret.additionalPaidAmount - ret.refundPaidAmount
            }
        }

        val totalCollections = cashRec + upiRec + bankRec + otherRec
        val creditSales = salesInRange.sumOf { it.remainingBalance }

        // Cumulative Cash-in-hand up to toMillis
        val cashInUpToEnd = allCustomerPayments
            .filter { it.timestamp <= toMillis && it.type != "ADVANCE_USED" && it.paymentMethod == "CASH" }
            .sumOf { it.amount } +
            allReturns.filter { it.timestamp <= toMillis && it.paymentMethod == "CASH" }
                .sumOf { it.additionalPaidAmount }

        val cashOutUpToEnd = allExpenses
            .filter { it.expenseDate <= toMillis && it.paymentMethod == "CASH" }
            .sumOf { it.amount } +
            allSupplierPayments.filter { it.timestamp <= toMillis && it.paymentMethod == "CASH" }
                .sumOf { it.amount } +
            allReturns.filter { it.timestamp <= toMillis && it.paymentMethod == "CASH" }
                .sumOf { it.refundPaidAmount }

        val cashBalance = cashInUpToEnd - cashOutUpToEnd

        // Today's closing balance
        val todayEnd = IndianFormatters.endOfDayMillis(System.currentTimeMillis())
        val todayCashIn = allCustomerPayments
            .filter { it.timestamp <= todayEnd && it.type != "ADVANCE_USED" && it.paymentMethod == "CASH" }
            .sumOf { it.amount } +
            allReturns.filter { it.timestamp <= todayEnd && it.paymentMethod == "CASH" }
                .sumOf { it.additionalPaidAmount }
        val todayCashOut = allExpenses
            .filter { it.expenseDate <= todayEnd && it.paymentMethod == "CASH" }
            .sumOf { it.amount } +
            allSupplierPayments.filter { it.timestamp <= todayEnd && it.paymentMethod == "CASH" }
                .sumOf { it.amount } +
            allReturns.filter { it.timestamp <= todayEnd && it.paymentMethod == "CASH" }
                .sumOf { it.refundPaidAmount }

        return AccountingSummary(
            netSales = netSales,
            grossBilledSales = grossBilled,
            totalPurchases = purchasesInRange.sumOf { it.totalAmount },
            costOfGoodsSold = cogs,
            grossProfit = grossProfit,
            operatingExpenses = operatingExpenses,
            netProfit = netProfit,
            cashReceived = cashRec,
            upiReceived = upiRec,
            bankReceived = bankRec,
            otherReceived = otherRec,
            totalCollections = totalCollections,
            creditSalesAmount = creditSales,
            customerOutstanding = allCustomers.sumOf { it.outstandingBalance },
            supplierOutstanding = allSuppliers.sumOf { it.outstandingAmount },
            cashBalance = cashBalance,
            dailyClosingBalance = todayCashIn - todayCashOut,
            stockCostValue = allProducts.sumOf { max(0, it.quantity) * it.purchasePrice },
            stockRetailValue = allProducts.sumOf { max(0, it.quantity) * it.sellingPrice }
        )
    }

    // --- Full Backup & Restore with SHA-256 Integrity Verification ---
    suspend fun exportBackupJson(): String {
        val payload = JSONObject()
        payload.put("app", "NAQSH_COLLECTION")
        payload.put("schemaVersion", 1)
        payload.put("exportedAt", System.currentTimeMillis())

        val dataObj = JSONObject()
        dataObj.put("products", JSONArray().apply {
            db.productDao().getAllOnce().forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("sku", p.sku); put("name", p.name)
                    put("category", p.category); put("purchasePrice", p.purchasePrice)
                    put("sellingPrice", p.sellingPrice); put("quantity", p.quantity)
                    put("size", p.size); put("color", p.color); put("brand", p.brand)
                    put("supplierId", p.supplierId ?: JSONObject.NULL)
                    put("supplierName", p.supplierName); put("minStockAlert", p.minStockAlert)
                    put("description", p.description); put("imageUri", p.imageUri ?: JSONObject.NULL)
                    put("dateAdded", p.dateAdded); put("updatedAt", p.updatedAt)
                })
            }
        })
        dataObj.put("stockMovements", JSONArray().apply {
            db.stockMovementDao().getAllOnce().forEach { m ->
                put(JSONObject().apply {
                    put("id", m.id); put("productId", m.productId); put("productName", m.productName)
                    put("type", m.type); put("quantityChange", m.quantityChange)
                    put("previousQuantity", m.previousQuantity); put("newQuantity", m.newQuantity)
                    put("referenceNo", m.referenceNo); put("note", m.note); put("timestamp", m.timestamp)
                })
            }
        })
        dataObj.put("customers", JSONArray().apply {
            db.customerDao().getAllOnce().forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id); put("name", c.name); put("mobile", c.mobile)
                    put("address", c.address); put("totalPurchases", c.totalPurchases)
                    put("totalPayments", c.totalPayments); put("advanceBalance", c.advanceBalance)
                    put("outstandingBalance", c.outstandingBalance); put("createdAt", c.createdAt)
                })
            }
        })
        dataObj.put("customerPayments", JSONArray().apply {
            db.customerPaymentDao().getAllOnce().forEach { cp ->
                put(JSONObject().apply {
                    put("id", cp.id); put("customerId", cp.customerId); put("customerName", cp.customerName)
                    put("saleId", cp.saleId ?: JSONObject.NULL)
                    put("invoiceNumber", cp.invoiceNumber ?: JSONObject.NULL)
                    put("amount", cp.amount); put("paymentMethod", cp.paymentMethod)
                    put("type", cp.type); put("note", cp.note); put("timestamp", cp.timestamp)
                })
            }
        })
        dataObj.put("suppliers", JSONArray().apply {
            db.supplierDao().getAllOnce().forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("name", s.name); put("contactNumber", s.contactNumber)
                    put("address", s.address); put("totalPurchases", s.totalPurchases)
                    put("totalPayments", s.totalPayments); put("outstandingAmount", s.outstandingAmount)
                    put("createdAt", s.createdAt)
                })
            }
        })
        dataObj.put("supplierPayments", JSONArray().apply {
            db.supplierPaymentDao().getAllOnce().forEach { sp ->
                put(JSONObject().apply {
                    put("id", sp.id); put("supplierId", sp.supplierId); put("supplierName", sp.supplierName)
                    put("purchaseId", sp.purchaseId ?: JSONObject.NULL)
                    put("purchaseInvoiceNo", sp.purchaseInvoiceNo ?: JSONObject.NULL)
                    put("amount", sp.amount); put("paymentMethod", sp.paymentMethod)
                    put("note", sp.note); put("timestamp", sp.timestamp)
                })
            }
        })
        dataObj.put("sales", JSONArray().apply {
            db.saleDao().getAllOnce().forEach { s ->
                put(JSONObject().apply {
                    put("id", s.id); put("invoiceNumber", s.invoiceNumber)
                    put("customerId", s.customerId ?: JSONObject.NULL)
                    put("customerName", s.customerName); put("customerMobile", s.customerMobile)
                    put("subtotal", s.subtotal); put("itemDiscountTotal", s.itemDiscountTotal)
                    put("overallDiscount", s.overallDiscount); put("grandTotal", s.grandTotal)
                    put("totalCostOfGoods", s.totalCostOfGoods); put("paidAmount", s.paidAmount)
                    put("advanceUsed", s.advanceUsed); put("remainingBalance", s.remainingBalance)
                    put("changeReturned", s.changeReturned); put("paymentMethod", s.paymentMethod)
                    put("notes", s.notes); put("timestamp", s.timestamp); put("hasReturn", s.hasReturn)
                })
            }
        })
        dataObj.put("saleItems", JSONArray().apply {
            db.saleItemDao().getAllOnce().forEach { si ->
                put(JSONObject().apply {
                    put("id", si.id); put("saleId", si.saleId); put("productId", si.productId)
                    put("sku", si.sku); put("productName", si.productName)
                    put("size", si.size); put("color", si.color); put("quantity", si.quantity)
                    put("returnedQuantity", si.returnedQuantity); put("unitCostPrice", si.unitCostPrice)
                    put("unitSellingPrice", si.unitSellingPrice); put("itemDiscount", si.itemDiscount)
                    put("lineTotal", si.lineTotal)
                })
            }
        })
        dataObj.put("purchases", JSONArray().apply {
            db.purchaseDao().getAllOnce().forEach { p ->
                put(JSONObject().apply {
                    put("id", p.id); put("purchaseInvoiceNo", p.purchaseInvoiceNo)
                    put("supplierId", p.supplierId ?: JSONObject.NULL)
                    put("supplierName", p.supplierName); put("totalAmount", p.totalAmount)
                    put("paidAmount", p.paidAmount); put("remainingBalance", p.remainingBalance)
                    put("paymentMethod", p.paymentMethod); put("notes", p.notes)
                    put("purchaseDate", p.purchaseDate)
                })
            }
        })
        dataObj.put("purchaseItems", JSONArray().apply {
            db.purchaseItemDao().getAllOnce().forEach { pi ->
                put(JSONObject().apply {
                    put("id", pi.id); put("purchaseId", pi.purchaseId); put("productId", pi.productId)
                    put("productName", pi.productName); put("quantity", pi.quantity)
                    put("unitPurchasePrice", pi.unitPurchasePrice); put("lineTotal", pi.lineTotal)
                })
            }
        })
        dataObj.put("expenses", JSONArray().apply {
            db.expenseDao().getAllOnce().forEach { e ->
                put(JSONObject().apply {
                    put("id", e.id); put("category", e.category); put("amount", e.amount)
                    put("paymentMethod", e.paymentMethod); put("description", e.description)
                    put("expenseDate", e.expenseDate)
                })
            }
        })
        dataObj.put("saleReturns", JSONArray().apply {
            db.saleReturnDao().getAllOnce().forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id); put("returnNumber", r.returnNumber)
                    put("originalSaleId", r.originalSaleId); put("originalInvoiceNo", r.originalInvoiceNo)
                    put("customerId", r.customerId ?: JSONObject.NULL)
                    put("customerName", r.customerName); put("returnedAmount", r.returnedAmount)
                    put("returnedCostOfGoods", r.returnedCostOfGoods); put("exchangeTotal", r.exchangeTotal)
                    put("exchangeCostOfGoods", r.exchangeCostOfGoods); put("netDifference", r.netDifference)
                    put("refundPaidAmount", r.refundPaidAmount); put("additionalPaidAmount", r.additionalPaidAmount)
                    put("creditAdjustedAmount", r.creditAdjustedAmount); put("paymentMethod", r.paymentMethod)
                    put("reason", r.reason); put("timestamp", r.timestamp)
                })
            }
        })
        dataObj.put("saleReturnItems", JSONArray().apply {
            db.saleReturnItemDao().getAllOnce().forEach { ri ->
                put(JSONObject().apply {
                    put("id", ri.id); put("returnId", ri.returnId)
                    put("saleItemId", ri.saleItemId ?: JSONObject.NULL)
                    put("productId", ri.productId); put("productName", ri.productName)
                    put("isExchangeItem", ri.isExchangeItem); put("quantity", ri.quantity)
                    put("unitPrice", ri.unitPrice); put("unitCostPrice", ri.unitCostPrice)
                    put("lineTotal", ri.lineTotal)
                })
            }
        })

        val dataString = canonicalJson(dataObj)
        val checksum = sha256(dataString)
        payload.put("sha256", checksum)
        payload.put("data", dataObj)
        return payload.toString(2)
    }

    suspend fun restoreFromBackupJson(rawJson: String) {
        val root = JSONObject(rawJson)
        require(root.optString("app") == "NAQSH_COLLECTION") {
            "Invalid backup file: Not a NAQSH COLLECTION backup"
        }
        val dataObj = root.getJSONObject("data")
        val expectedChecksum = root.optString("sha256", "")
        val actualChecksum = sha256(canonicalJson(dataObj))
        require(expectedChecksum.isNotEmpty() && expectedChecksum == actualChecksum) {
            "Backup integrity verification failed (checksum mismatch)"
        }

        val productsList = parseArray(dataObj.getJSONArray("products")) { o ->
            ProductEntity(
                id = o.getLong("id"), sku = o.getString("sku"), name = o.getString("name"),
                category = o.getString("category"), purchasePrice = o.getDouble("purchasePrice"),
                sellingPrice = o.getDouble("sellingPrice"), quantity = o.getInt("quantity"),
                size = o.getString("size"), color = o.getString("color"), brand = o.getString("brand"),
                supplierId = o.optLongOrNull("supplierId"), supplierName = o.optString("supplierName", ""),
                minStockAlert = o.optInt("minStockAlert", 3), description = o.optString("description", ""),
                imageUri = o.optStringOrNull("imageUri"), dateAdded = o.getLong("dateAdded"),
                updatedAt = o.getLong("updatedAt")
            )
        }
        val stockMovementsList = parseArray(dataObj.getJSONArray("stockMovements")) { o ->
            StockMovementEntity(
                id = o.getLong("id"), productId = o.getLong("productId"),
                productName = o.getString("productName"), type = o.getString("type"),
                quantityChange = o.getInt("quantityChange"), previousQuantity = o.getInt("previousQuantity"),
                newQuantity = o.getInt("newQuantity"), referenceNo = o.optString("referenceNo", ""),
                note = o.optString("note", ""), timestamp = o.getLong("timestamp")
            )
        }
        val customersList = parseArray(dataObj.getJSONArray("customers")) { o ->
            CustomerEntity(
                id = o.getLong("id"), name = o.getString("name"), mobile = o.getString("mobile"),
                address = o.optString("address", ""), totalPurchases = o.getDouble("totalPurchases"),
                totalPayments = o.getDouble("totalPayments"), advanceBalance = o.getDouble("advanceBalance"),
                outstandingBalance = o.getDouble("outstandingBalance"), createdAt = o.getLong("createdAt")
            )
        }
        val customerPaymentsList = parseArray(dataObj.getJSONArray("customerPayments")) { o ->
            CustomerPaymentEntity(
                id = o.getLong("id"), customerId = o.getLong("customerId"),
                customerName = o.getString("customerName"), saleId = o.optLongOrNull("saleId"),
                invoiceNumber = o.optStringOrNull("invoiceNumber"), amount = o.getDouble("amount"),
                paymentMethod = o.getString("paymentMethod"), type = o.getString("type"),
                note = o.optString("note", ""), timestamp = o.getLong("timestamp")
            )
        }
        val suppliersList = parseArray(dataObj.getJSONArray("suppliers")) { o ->
            SupplierEntity(
                id = o.getLong("id"), name = o.getString("name"),
                contactNumber = o.getString("contactNumber"), address = o.optString("address", ""),
                totalPurchases = o.getDouble("totalPurchases"), totalPayments = o.getDouble("totalPayments"),
                outstandingAmount = o.getDouble("outstandingAmount"), createdAt = o.getLong("createdAt")
            )
        }
        val supplierPaymentsList = parseArray(dataObj.getJSONArray("supplierPayments")) { o ->
            SupplierPaymentEntity(
                id = o.getLong("id"), supplierId = o.getLong("supplierId"),
                supplierName = o.getString("supplierName"), purchaseId = o.optLongOrNull("purchaseId"),
                purchaseInvoiceNo = o.optStringOrNull("purchaseInvoiceNo"), amount = o.getDouble("amount"),
                paymentMethod = o.getString("paymentMethod"), note = o.optString("note", ""),
                timestamp = o.getLong("timestamp")
            )
        }
        val salesList = parseArray(dataObj.getJSONArray("sales")) { o ->
            SaleEntity(
                id = o.getLong("id"), invoiceNumber = o.getString("invoiceNumber"),
                customerId = o.optLongOrNull("customerId"), customerName = o.getString("customerName"),
                customerMobile = o.optString("customerMobile", ""), subtotal = o.getDouble("subtotal"),
                itemDiscountTotal = o.getDouble("itemDiscountTotal"), overallDiscount = o.getDouble("overallDiscount"),
                grandTotal = o.getDouble("grandTotal"), totalCostOfGoods = o.getDouble("totalCostOfGoods"),
                paidAmount = o.getDouble("paidAmount"), advanceUsed = o.optDouble("advanceUsed", 0.0),
                remainingBalance = o.getDouble("remainingBalance"), changeReturned = o.optDouble("changeReturned", 0.0),
                paymentMethod = o.getString("paymentMethod"), notes = o.optString("notes", ""),
                timestamp = o.getLong("timestamp"), hasReturn = o.optBoolean("hasReturn", false)
            )
        }
        val saleItemsList = parseArray(dataObj.getJSONArray("saleItems")) { o ->
            SaleItemEntity(
                id = o.getLong("id"), saleId = o.getLong("saleId"), productId = o.getLong("productId"),
                sku = o.optString("sku", ""), productName = o.getString("productName"),
                size = o.optString("size", ""), color = o.optString("color", ""),
                quantity = o.getInt("quantity"), returnedQuantity = o.optInt("returnedQuantity", 0),
                unitCostPrice = o.getDouble("unitCostPrice"), unitSellingPrice = o.getDouble("unitSellingPrice"),
                itemDiscount = o.getDouble("itemDiscount"), lineTotal = o.getDouble("lineTotal")
            )
        }
        val purchasesList = parseArray(dataObj.getJSONArray("purchases")) { o ->
            PurchaseEntity(
                id = o.getLong("id"), purchaseInvoiceNo = o.getString("purchaseInvoiceNo"),
                supplierId = o.optLongOrNull("supplierId"), supplierName = o.getString("supplierName"),
                totalAmount = o.getDouble("totalAmount"), paidAmount = o.getDouble("paidAmount"),
                remainingBalance = o.getDouble("remainingBalance"), paymentMethod = o.getString("paymentMethod"),
                notes = o.optString("notes", ""), purchaseDate = o.getLong("purchaseDate")
            )
        }
        val purchaseItemsList = parseArray(dataObj.getJSONArray("purchaseItems")) { o ->
            PurchaseItemEntity(
                id = o.getLong("id"), purchaseId = o.getLong("purchaseId"),
                productId = o.getLong("productId"), productName = o.getString("productName"),
                quantity = o.getInt("quantity"), unitPurchasePrice = o.getDouble("unitPurchasePrice"),
                lineTotal = o.getDouble("lineTotal")
            )
        }
        val expensesList = parseArray(dataObj.getJSONArray("expenses")) { o ->
            ExpenseEntity(
                id = o.getLong("id"), category = o.getString("category"),
                amount = o.getDouble("amount"), paymentMethod = o.getString("paymentMethod"),
                description = o.optString("description", ""), expenseDate = o.getLong("expenseDate")
            )
        }
        val returnsList = parseArray(dataObj.getJSONArray("saleReturns")) { o ->
            SaleReturnEntity(
                id = o.getLong("id"), returnNumber = o.getString("returnNumber"),
                originalSaleId = o.getLong("originalSaleId"), originalInvoiceNo = o.getString("originalInvoiceNo"),
                customerId = o.optLongOrNull("customerId"), customerName = o.getString("customerName"),
                returnedAmount = o.getDouble("returnedAmount"), returnedCostOfGoods = o.getDouble("returnedCostOfGoods"),
                exchangeTotal = o.getDouble("exchangeTotal"), exchangeCostOfGoods = o.getDouble("exchangeCostOfGoods"),
                netDifference = o.getDouble("netDifference"), refundPaidAmount = o.getDouble("refundPaidAmount"),
                additionalPaidAmount = o.getDouble("additionalPaidAmount"),
                creditAdjustedAmount = o.getDouble("creditAdjustedAmount"),
                paymentMethod = o.getString("paymentMethod"), reason = o.optString("reason", ""),
                timestamp = o.getLong("timestamp")
            )
        }
        val returnItemsList = parseArray(dataObj.getJSONArray("saleReturnItems")) { o ->
            SaleReturnItemEntity(
                id = o.getLong("id"), returnId = o.getLong("returnId"),
                saleItemId = o.optLongOrNull("saleItemId"), productId = o.getLong("productId"),
                productName = o.getString("productName"), isExchangeItem = o.getBoolean("isExchangeItem"),
                quantity = o.getInt("quantity"), unitPrice = o.getDouble("unitPrice"),
                unitCostPrice = o.getDouble("unitCostPrice"), lineTotal = o.getDouble("lineTotal")
            )
        }

        db.withTransaction {
            db.saleReturnItemDao().clearAll()
            db.saleReturnDao().clearAll()
            db.purchaseItemDao().clearAll()
            db.purchaseDao().clearAll()
            db.saleItemDao().clearAll()
            db.saleDao().clearAll()
            db.supplierPaymentDao().clearAll()
            db.supplierDao().clearAll()
            db.customerPaymentDao().clearAll()
            db.customerDao().clearAll()
            db.stockMovementDao().clearAll()
            db.productDao().clearAll()
            db.expenseDao().clearAll()

            db.productDao().insertAll(productsList)
            db.stockMovementDao().insertAll(stockMovementsList)
            db.customerDao().insertAll(customersList)
            db.customerPaymentDao().insertAll(customerPaymentsList)
            db.supplierDao().insertAll(suppliersList)
            db.supplierPaymentDao().insertAll(supplierPaymentsList)
            db.saleDao().insertAll(salesList)
            db.saleItemDao().insertAll(saleItemsList)
            db.purchaseDao().insertAll(purchasesList)
            db.purchaseItemDao().insertAll(purchaseItemsList)
            db.expenseDao().insertAll(expensesList)
            db.saleReturnDao().insertAll(returnsList)
            db.saleReturnItemDao().insertAll(returnItemsList)
        }
    }

    private fun canonicalJson(value: Any?): String = when (value) {
        is JSONObject -> value.keys().asSequence().sorted().joinToString(",", "{", "}") { k ->
            "${JSONObject.quote(k)}:${canonicalJson(value.get(k))}"
        }
        is JSONArray -> (0 until value.length()).joinToString(",", "[", "]") { i ->
            canonicalJson(value.get(i))
        }
        is Number -> {
            val d = value.toDouble()
            if (d % 1.0 == 0.0 && d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) d.toLong().toString()
            else d.toString()
        }
        is String -> JSONObject.quote(value)
        is Boolean -> value.toString()
        else -> "null"
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    private inline fun <T> parseArray(arr: JSONArray, mapper: (JSONObject) -> T): List<T> {
        val list = ArrayList<T>(arr.length())
        for (i in 0 until arr.length()) {
            list.add(mapper(arr.getJSONObject(i)))
        }
        return list
    }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key)) null else optLong(key)

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key)
}

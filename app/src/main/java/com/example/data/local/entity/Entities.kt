package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object BoutiqueConstants {
    val PRODUCT_CATEGORIES = listOf(
        "Abayas",
        "Hijabs",
        "Naqabs",
        "Burqas",
        "Stoles",
        "Modest Accessories",
        "Pakistani Suits",
        "Other"
    )

    val PRODUCT_SIZES = listOf(
        "Free Size",
        "50",
        "52",
        "54",
        "56",
        "58",
        "S",
        "M",
        "L",
        "XL",
        "XXL",
        "Unstitched"
    )

    val EXPENSE_CATEGORIES = listOf(
        "Electricity",
        "Internet",
        "Transport",
        "Packaging",
        "Shop decoration",
        "Staff expenses",
        "Shopping and supplies",
        "Maintenance",
        "Miscellaneous"
    )

    val PAYMENT_METHODS = listOf(
        "CASH",
        "UPI",
        "BANK_TRANSFER",
        "CREDIT",
        "OTHER"
    )

    val DIRECT_PAYMENT_METHODS = listOf(
        "CASH",
        "UPI",
        "BANK_TRANSFER",
        "OTHER"
    )
}

@Entity(
    tableName = "products",
    indices = [Index(value = ["sku"])]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String,
    val name: String,
    val category: String,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val quantity: Int,
    val size: String,
    val color: String,
    val brand: String,
    val supplierId: Long? = null,
    val supplierName: String = "",
    val minStockAlert: Int = 3,
    val description: String = "",
    val imageUri: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val type: String, // OPENING, SALE, PURCHASE, RETURN, EXCHANGE_OUT, ADJUSTMENT
    val quantityChange: Int,
    val previousQuantity: Int,
    val newQuantity: Int,
    val referenceNo: String = "",
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mobile: String,
    val address: String = "",
    val totalPurchases: Double = 0.0,
    val totalPayments: Double = 0.0,
    val advanceBalance: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_payments")
data class CustomerPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long, // 0 for Walk-in customer
    val customerName: String,
    val saleId: Long? = null,
    val invoiceNumber: String? = null,
    val amount: Double,
    val paymentMethod: String, // CASH, UPI, BANK_TRANSFER, ADVANCE_USED, OTHER
    val type: String, // BILL_PAYMENT, DUE_RECOVERY, ADVANCE_DEPOSIT, EXCHANGE_PAYMENT
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contactNumber: String,
    val address: String = "",
    val totalPurchases: Double = 0.0,
    val totalPayments: Double = 0.0,
    val outstandingAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "supplier_payments")
data class SupplierPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val supplierName: String,
    val purchaseId: Long? = null,
    val purchaseInvoiceNo: String? = null,
    val amount: Double,
    val paymentMethod: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "sales",
    indices = [Index(value = ["invoiceNumber"], unique = true)]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerId: Long? = null,
    val customerName: String,
    val customerMobile: String = "",
    val subtotal: Double,
    val itemDiscountTotal: Double,
    val overallDiscount: Double,
    val grandTotal: Double,
    val totalCostOfGoods: Double,
    val paidAmount: Double,
    val advanceUsed: Double = 0.0,
    val remainingBalance: Double,
    val changeReturned: Double = 0.0,
    val paymentMethod: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val hasReturn: Boolean = false
)

@Entity(tableName = "sale_items")
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val sku: String,
    val productName: String,
    val size: String,
    val color: String,
    val quantity: Int,
    val returnedQuantity: Int = 0,
    val unitCostPrice: Double,
    val unitSellingPrice: Double,
    val itemDiscount: Double,
    val lineTotal: Double
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseInvoiceNo: String,
    val supplierId: Long? = null,
    val supplierName: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val remainingBalance: Double,
    val paymentMethod: String,
    val notes: String = "",
    val purchaseDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_items")
data class PurchaseItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val purchaseId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPurchasePrice: Double,
    val lineTotal: Double
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val amount: Double,
    val paymentMethod: String,
    val description: String = "",
    val expenseDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_returns")
data class SaleReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnNumber: String,
    val originalSaleId: Long,
    val originalInvoiceNo: String,
    val customerId: Long? = null,
    val customerName: String,
    val returnedAmount: Double,
    val returnedCostOfGoods: Double,
    val exchangeTotal: Double,
    val exchangeCostOfGoods: Double,
    val netDifference: Double,
    val refundPaidAmount: Double,
    val additionalPaidAmount: Double,
    val creditAdjustedAmount: Double,
    val paymentMethod: String,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sale_return_items")
data class SaleReturnItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val returnId: Long,
    val saleItemId: Long? = null,
    val productId: Long,
    val productName: String,
    val isExchangeItem: Boolean,
    val quantity: Int,
    val unitPrice: Double,
    val unitCostPrice: Double,
    val lineTotal: Double
)

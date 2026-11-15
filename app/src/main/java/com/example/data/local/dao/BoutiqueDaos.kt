package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY id ASC")
    suspend fun getAllOnce(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getBySku(sku: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<ProductEntity>)

    @Update
    suspend fun update(product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("DELETE FROM products")
    suspend fun clearAll()
}

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun observeForProduct(productId: Long): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY id ASC")
    suspend fun getAllOnce(): List<StockMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(movement: StockMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(movements: List<StockMovementEntity>)

    @Query("DELETE FROM stock_movements")
    suspend fun clearAll()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers ORDER BY id ASC")
    suspend fun getAllOnce(): List<CustomerEntity>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: CustomerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(customers: List<CustomerEntity>)

    @Update
    suspend fun update(customer: CustomerEntity)

    @Delete
    suspend fun delete(customer: CustomerEntity)

    @Query("DELETE FROM customers")
    suspend fun clearAll()
}

@Dao
interface CustomerPaymentDao {
    @Query("SELECT * FROM customer_payments ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<CustomerPaymentEntity>>

    @Query("SELECT * FROM customer_payments WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun observeForCustomer(customerId: Long): Flow<List<CustomerPaymentEntity>>

    @Query("SELECT * FROM customer_payments ORDER BY id ASC")
    suspend fun getAllOnce(): List<CustomerPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: CustomerPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<CustomerPaymentEntity>)

    @Query("DELETE FROM customer_payments")
    suspend fun clearAll()
}

@Dao
interface SupplierDao {
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun observeAll(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers ORDER BY id ASC")
    suspend fun getAllOnce(): List<SupplierEntity>

    @Query("SELECT * FROM suppliers WHERE id = :id")
    suspend fun getById(id: Long): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplier: SupplierEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(suppliers: List<SupplierEntity>)

    @Update
    suspend fun update(supplier: SupplierEntity)

    @Delete
    suspend fun delete(supplier: SupplierEntity)

    @Query("DELETE FROM suppliers")
    suspend fun clearAll()
}

@Dao
interface SupplierPaymentDao {
    @Query("SELECT * FROM supplier_payments ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SupplierPaymentEntity>>

    @Query("SELECT * FROM supplier_payments WHERE supplierId = :supplierId ORDER BY timestamp DESC")
    fun observeForSupplier(supplierId: Long): Flow<List<SupplierPaymentEntity>>

    @Query("SELECT * FROM supplier_payments ORDER BY id ASC")
    suspend fun getAllOnce(): List<SupplierPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: SupplierPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(payments: List<SupplierPaymentEntity>)

    @Query("DELETE FROM supplier_payments")
    suspend fun clearAll()
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE customerId = :customerId ORDER BY timestamp DESC")
    fun observeForCustomer(customerId: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales ORDER BY id ASC")
    suspend fun getAllOnce(): List<SaleEntity>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getById(id: Long): SaleEntity?

    @Query("SELECT * FROM sales WHERE invoiceNumber = :invoiceNumber LIMIT 1")
    suspend fun getByInvoiceNumber(invoiceNumber: String): SaleEntity?

    @Query("SELECT COUNT(*) FROM sales WHERE invoiceNumber = :invoiceNumber")
    suspend fun countByInvoiceNumber(invoiceNumber: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sales: List<SaleEntity>)

    @Update
    suspend fun update(sale: SaleEntity)

    @Query("DELETE FROM sales")
    suspend fun clearAll()
}

@Dao
interface SaleItemDao {
    @Query("SELECT * FROM sale_items ORDER BY id ASC")
    fun observeAll(): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId ORDER BY id ASC")
    suspend fun getForSale(saleId: Long): List<SaleItemEntity>

    @Query("SELECT * FROM sale_items ORDER BY id ASC")
    suspend fun getAllOnce(): List<SaleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SaleItemEntity>)

    @Update
    suspend fun update(item: SaleItemEntity)

    @Query("DELETE FROM sale_items")
    suspend fun clearAll()
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    fun observeAll(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE supplierId = :supplierId ORDER BY purchaseDate DESC")
    fun observeForSupplier(supplierId: Long): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases ORDER BY id ASC")
    suspend fun getAllOnce(): List<PurchaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(purchase: PurchaseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(purchases: List<PurchaseEntity>)

    @Query("DELETE FROM purchases")
    suspend fun clearAll()
}

@Dao
interface PurchaseItemDao {
    @Query("SELECT * FROM purchase_items ORDER BY id ASC")
    fun observeAll(): Flow<List<PurchaseItemEntity>>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId ORDER BY id ASC")
    suspend fun getForPurchase(purchaseId: Long): List<PurchaseItemEntity>

    @Query("SELECT * FROM purchase_items ORDER BY id ASC")
    suspend fun getAllOnce(): List<PurchaseItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PurchaseItemEntity>)

    @Query("DELETE FROM purchase_items")
    suspend fun clearAll()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY expenseDate DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY id ASC")
    suspend fun getAllOnce(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}

@Dao
interface SaleReturnDao {
    @Query("SELECT * FROM sale_returns ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SaleReturnEntity>>

    @Query("SELECT * FROM sale_returns ORDER BY id ASC")
    suspend fun getAllOnce(): List<SaleReturnEntity>

    @Query("SELECT COALESCE(SUM(creditAdjustedAmount), 0.0) FROM sale_returns WHERE originalSaleId = :saleId")
    suspend fun getTotalCreditAdjustedForSale(saleId: Long): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(saleReturn: SaleReturnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(returns: List<SaleReturnEntity>)

    @Query("DELETE FROM sale_returns")
    suspend fun clearAll()
}

@Dao
interface SaleReturnItemDao {
    @Query("SELECT * FROM sale_return_items ORDER BY id ASC")
    fun observeAll(): Flow<List<SaleReturnItemEntity>>

    @Query("SELECT * FROM sale_return_items WHERE returnId = :returnId ORDER BY id ASC")
    suspend fun getForReturn(returnId: Long): List<SaleReturnItemEntity>

    @Query("SELECT * FROM sale_return_items ORDER BY id ASC")
    suspend fun getAllOnce(): List<SaleReturnItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<SaleReturnItemEntity>)

    @Query("DELETE FROM sale_return_items")
    suspend fun clearAll()
}

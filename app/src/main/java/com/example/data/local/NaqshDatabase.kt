package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.CustomerDao
import com.example.data.local.dao.CustomerPaymentDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.PurchaseDao
import com.example.data.local.dao.PurchaseItemDao
import com.example.data.local.dao.SaleDao
import com.example.data.local.dao.SaleItemDao
import com.example.data.local.dao.SaleReturnDao
import com.example.data.local.dao.SaleReturnItemDao
import com.example.data.local.dao.StockMovementDao
import com.example.data.local.dao.SupplierDao
import com.example.data.local.dao.SupplierPaymentDao
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

@Database(
    entities = [
        ProductEntity::class,
        StockMovementEntity::class,
        CustomerEntity::class,
        CustomerPaymentEntity::class,
        SupplierEntity::class,
        SupplierPaymentEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        PurchaseEntity::class,
        PurchaseItemEntity::class,
        ExpenseEntity::class,
        SaleReturnEntity::class,
        SaleReturnItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NaqshDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun customerDao(): CustomerDao
    abstract fun customerPaymentDao(): CustomerPaymentDao
    abstract fun supplierDao(): SupplierDao
    abstract fun supplierPaymentDao(): SupplierPaymentDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun purchaseItemDao(): PurchaseItemDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun saleReturnDao(): SaleReturnDao
    abstract fun saleReturnItemDao(): SaleReturnItemDao

    companion object {
        @Volatile
        private var INSTANCE: NaqshDatabase? = null

        fun getInstance(context: Context): NaqshDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NaqshDatabase::class.java,
                    "naqsh_collection_boutique.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

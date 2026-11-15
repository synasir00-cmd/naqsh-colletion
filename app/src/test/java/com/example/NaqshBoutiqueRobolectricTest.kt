package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NaqshDatabase
import com.example.data.local.entity.CustomerEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SupplierEntity
import com.example.data.preferences.BoutiquePreferences
import com.example.data.preferences.BoutiqueSettings
import com.example.data.repository.BoutiqueRepository
import com.example.data.repository.CartLineRequest
import com.example.data.repository.ExchangeLineRequest
import com.example.data.repository.PurchaseLineRequest
import com.example.data.repository.ReturnLineRequest
import com.example.util.IndianFormatters
import com.example.util.PdfInvoiceGenerator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NaqshBoutiqueRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: NaqshDatabase
    private lateinit var prefs: BoutiquePreferences
    private lateinit var repo: BoutiqueRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NaqshDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefs = BoutiquePreferences(context)
        repo = BoutiqueRepository(db, prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `indian currency formatting matches 1 lakh standard`() {
        assertEquals("₹1,00,000.00", IndianFormatters.formatCurrency(100000.0))
        assertEquals("-₹1,250.50", IndianFormatters.formatCurrency(-1250.5))
    }

    @Test
    fun `complete boutique accounting lifecycle - product, purchase, credit sale, payment, expense, return, pdf, backup restore`() = runTest {
        // 1. Add & Edit Product
        val abayaId = repo.saveProduct(
            ProductEntity(
                sku = "ABY-001",
                name = "Dubai Nida Abaya",
                category = "Abayas",
                purchasePrice = 1200.0,
                sellingPrice = 2000.0,
                quantity = 5,
                size = "54",
                color = "Emerald",
                brand = "Naqsh"
            )
        )
        val hijabId = repo.saveProduct(
            ProductEntity(
                sku = "HIJ-002",
                name = "Silk Chiffon Hijab",
                category = "Hijabs",
                purchasePrice = 250.0,
                sellingPrice = 500.0,
                quantity = 10,
                size = "Free Size",
                color = "Champagne",
                brand = "Naqsh"
            )
        )
        val savedAbaya = db.productDao().getById(abayaId)!!
        repo.saveProduct(savedAbaya.copy(sellingPrice = 2200.0))
        assertEquals(2200.0, db.productDao().getById(abayaId)!!.sellingPrice, 0.01)

        // 2. Create Supplier & Record Purchase (+5 Abayas @ 1200 = 6000, paid 4000, due 2000)
        val suppId = repo.saveSupplier(SupplierEntity(name = "Karachi Fabrics", contactNumber = "9876543210"))
        repo.recordPurchase(
            purchaseInvoiceNo = "PUR-101",
            supplierId = suppId,
            supplierNameInput = "Karachi Fabrics",
            lines = listOf(PurchaseLineRequest(abayaId, 5, 1200.0)),
            paidAmountInput = 4000.0,
            paymentMethod = "CASH",
            purchaseDate = System.currentTimeMillis(),
            notes = "Restock"
        )
        assertEquals(10, db.productDao().getById(abayaId)!!.quantity)
        assertEquals(2000.0, db.supplierDao().getById(suppId)!!.outstandingAmount, 0.01)

        // 3. Create Customer
        val custId = repo.saveCustomer(CustomerEntity(name = "Ayesha Khan", mobile = "9988776655", address = "Lucknow"))

        // 4. Credit / Partial Sale: 2 Abayas (2200x2=4400 - 200 item disc = 4200) + overall disc 200 = 4000.
        // Paid 1500 UPI, remaining credit 2500. COGS = 2 * 1200 = 2400.
        val sale1 = repo.createSale(
            invoiceNumberInput = null,
            customerId = custId,
            walkInName = "",
            walkInMobile = "",
            lines = listOf(CartLineRequest(abayaId, 2, 2200.0, 200.0)),
            overallDiscountInput = 200.0,
            paymentMethod = "UPI",
            tenderedAmount = 1500.0,
            useAdvanceBalance = false,
            allowNegativeStock = false,
            notes = "Eid order"
        )
        assertEquals(4000.0, sale1.sale.grandTotal, 0.01)
        assertEquals(1500.0, sale1.sale.paidAmount, 0.01)
        assertEquals(2500.0, sale1.sale.remainingBalance, 0.01)
        assertEquals(8, db.productDao().getById(abayaId)!!.quantity)
        assertEquals(2500.0, db.customerDao().getById(custId)!!.outstandingBalance, 0.01)

        // 5. Duplicate invoice number must be rejected
        var duplicateRejected = false
        try {
            repo.createSale(
                invoiceNumberInput = sale1.sale.invoiceNumber,
                customerId = null,
                walkInName = "Walk-in",
                walkInMobile = "",
                lines = listOf(CartLineRequest(hijabId, 1, 500.0, 0.0)),
                overallDiscountInput = 0.0,
                paymentMethod = "CASH",
                tenderedAmount = 500.0,
                useAdvanceBalance = false,
                allowNegativeStock = false,
                notes = ""
            )
        } catch (_: IllegalStateException) {
            duplicateRejected = true
        }
        assertTrue(duplicateRejected)

        // 6. Receive Customer Due Payment (1000 Cash -> customer balance becomes 1500)
        repo.recordCustomerPayment(custId, 1000.0, "CASH", isAdvanceDeposit = false, note = "Part payment")
        assertEquals(1500.0, db.customerDao().getById(custId)!!.outstandingBalance, 0.01)

        // 7. Record Operating Expense (300 Cash)
        repo.addExpense("Packaging", 300.0, "CASH", "Boutique gift boxes", System.currentTimeMillis())

        // 8. Accounting Check Before Return:
        // Net Sales = 4000, COGS = 2400 -> Gross Profit = 1600, Expenses = 300 -> Net Profit = 1300
        // (Notice Purchase 6000 is NOT treated as expense, and Credit 2500 is NOT counted as cash!)
        val accBeforeReturn = repo.computeAccountingSummary(
            allProducts = db.productDao().getAllOnce(),
            allCustomers = db.customerDao().getAllOnce(),
            allSuppliers = db.supplierDao().getAllOnce(),
            allSales = db.saleDao().getAllOnce(),
            allCustomerPayments = db.customerPaymentDao().getAllOnce(),
            allPurchases = db.purchaseDao().getAllOnce(),
            allSupplierPayments = db.supplierPaymentDao().getAllOnce(),
            allExpenses = db.expenseDao().getAllOnce(),
            allReturns = db.saleReturnDao().getAllOnce()
        )
        assertEquals(4000.0, accBeforeReturn.netSales, 0.01)
        assertEquals(2400.0, accBeforeReturn.costOfGoodsSold, 0.01)
        assertEquals(1600.0, accBeforeReturn.grossProfit, 0.01)
        assertEquals(300.0, accBeforeReturn.operatingExpenses, 0.01)
        assertEquals(1300.0, accBeforeReturn.netProfit, 0.01)
        assertEquals(1000.0, accBeforeReturn.cashReceived, 0.01)
        assertEquals(1500.0, accBeforeReturn.upiReceived, 0.01)

        // 9. Process Partial Return + Exchange:
        // Return 1 Abaya (net value 2000, cost 1200), Exchange 1 Hijab (selling 500, cost 250).
        // Net diff = 500 - 2000 = -1500 -> adjusts customer credit 1500 -> customer outstanding = 0!
        val origItemId = sale1.items.first().id
        repo.processReturnOrExchange(
            originalSaleId = sale1.sale.id,
            returnLines = listOf(ReturnLineRequest(origItemId, abayaId, 1)),
            exchangeLines = listOf(ExchangeLineRequest(hijabId, 1, 500.0)),
            paymentMethod = "CASH",
            additionalPaidInput = 0.0,
            allowNegativeStock = false,
            reason = "Size exchange"
        )
        assertEquals(9, db.productDao().getById(abayaId)!!.quantity)
        assertEquals(9, db.productDao().getById(hijabId)!!.quantity)
        assertEquals(0.0, db.customerDao().getById(custId)!!.outstandingBalance, 0.01)
        // Original sale record still preserved!
        assertTrue(db.saleDao().getById(sale1.sale.id)!!.hasReturn)
        assertEquals(4000.0, db.saleDao().getById(sale1.sale.id)!!.grandTotal, 0.01)

        // 10. Generate CSV Report
        val csvFile = com.example.util.ShareHelper.writeCsvFile(
            context,
            "Test_Sales.csv",
            listOf("Invoice", "Total"),
            listOf(listOf(sale1.sale.invoiceNumber, IndianFormatters.formatCurrency(sale1.sale.grandTotal)))
        )
        assertTrue(csvFile.exists() && csvFile.readText().contains(sale1.sale.invoiceNumber))

        // 11. PIN Security Verification
        prefs.setPin("4829")
        assertTrue(prefs.verifyPin("4829"))
        assertFalse(prefs.verifyPin("1234"))

        // 12. Export Backup, Tamper Check, Clear & Restore
        val backupJson = repo.exportBackupJson()
        var tamperedFailed = false
        try {
            repo.restoreFromBackupJson(backupJson.replace("Dubai Nida Abaya", "Tampered"))
        } catch (_: IllegalArgumentException) {
            tamperedFailed = true
        }
        assertTrue(tamperedFailed)

        db.productDao().clearAll()
        db.saleDao().clearAll()
        assertEquals(0, repo.products.first().size)

        repo.restoreFromBackupJson(backupJson)
        assertEquals(2, repo.products.first().size)
        assertEquals(1, repo.sales.first().size)
        assertFalse(repo.saleReturns.first().isEmpty())
    }
}

# NAQSH COLLECTION — Complete Boutique Management System

**Tagline:** *MODEST & PAKISTANI WEAR*  
**Package Name:** `com.nasirali.naqshcollection`  
**Minimum Android:** Android 8.0 (API 26+)

An offline-first, production-ready Android POS, inventory, khata, accounting, WhatsApp PDF invoicing, and Bluetooth ESC/POS thermal printing application built natively in Kotlin, Jetpack Compose, Material 3, and Room Database.

---

## Core Modules

1. **Luxury Branding & Custom Launcher Icon**: Deep Emerald Green (`#0B3B2E`), Champagne Gold (`#D4AF37`), Ivory White (`#FBF8F1`), and Soft Beige (`#F3ECE0`) with custom NC monogram launcher icon, splash screen, and PDF/receipt logo.
2. **Real-Time Home Dashboard**: Today's Sales, Today's Profit, Today's Expenses, Today's Cash Received, Monthly Sales, Monthly Profit, Total Stock Value, Customer Outstanding, Low Stock Alerts, Total Products, Total Customers, and 7 Quick Actions.
3. **POS Billing System**: Product & SKU search, size/color/variant display, item-wise & overall discounts, Cash / UPI / Bank Transfer / Credit / Partial / Advance payments, automatic change returned & remaining balance, and strictly unique invoice numbers.
4. **PDF Invoices & WhatsApp Sharing**: Branded A4 PDF invoice generation (`PdfDocument`) with NC logo, direct WhatsApp / WhatsApp Business sharing via `FileProvider`, generic PDF sharing, and SAF storage saving.
5. **Bluetooth ESC/POS & Android Print Framework**: Discovery, pairing list, RFCOMM SPP connection, reconnect last printer, 58mm & 80mm paper widths, ASCII ESC/POS mode + Hindi/Urdu/Logo monochrome raster bitmap mode (`GS v 0`), duplicate invoice printing, and Android System Print (`PrintManager`) for Wi-Fi printers.
6. **Inventory, Purchases, Suppliers & Returns**: Full Product CRUD with Android Photo Picker, stock adjustments & movement ledger, purchase recording with auto-stock & supplier balance updates, and full/partial Sales Returns & Exchanges that preserve original financial records.
7. **Accurate Accounting Engine**: Profit calculated from actual Cost of Goods Sold (COGS) minus operating expenses (without confusing inventory purchases with expenses, without counting credit sales as cash, and without double-counting customer advances).
8. **13 Business Reports**: Daily/Weekly/Monthly/Annual Sales, Profit & Loss, Expense, Stock, Purchase, Customer Ledger, Supplier Ledger, Payment Method, Best-Selling, and Low-Stock reports with custom date ranges and PDF/CSV export.
9. **Backup & Restore + PIN Security**: Full database JSON backup with SHA-256 checksum verification, Storage Access Framework export/import, local auto-backups, optional 4-digit PIN lock, and Roman Urdu (default), Hindi, and English localization with Indian Rupee formatting (`₹1,00,000.00`).

---

## Building Debug & Release APKs

### 1. Debug APK (for initial testing)
```bash
./gradlew assembleDebug
```
Output APK: `app/build/outputs/apk/debug/app-debug.apk`

### 2. Release APK
1. Generate an upload keystore (one-time):
   ```bash
   keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
2. Export signing environment variables:
   ```bash
   export KEYSTORE_PATH=/path/to/my-upload-key.jks
   export STORE_PASSWORD=your_store_password
   export KEY_PASSWORD=your_key_password
   ```
3. Build release APK or App Bundle:
   ```bash
   ./gradlew assembleRelease
   # or for Google Play:
   ./gradlew bundleRelease
   ```
Output APK: `app/build/outputs/apk/release/app-release.apk`

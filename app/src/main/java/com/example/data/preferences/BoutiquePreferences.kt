package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "naqsh_settings")

data class BoutiqueSettings(
    val language: AppLanguage = AppLanguage.ROMAN_URDU,
    val shopName: String = "NAQSH COLLECTION",
    val shopTagline: String = "MODEST & PAKISTANI WEAR",
    val shopPhone: String = "",
    val shopAddress: String = "",
    val pinHash: String = "",
    val pinEnabled: Boolean = false,
    val lockReportsWithPin: Boolean = true,
    val allowNegativeStock: Boolean = false,
    val paperWidthMm: Int = 58,
    val bitmapThermalMode: Boolean = true,
    val lastPrinterAddress: String = "",
    val lastPrinterName: String = "",
    val autoBackupEnabled: Boolean = true,
    val lastBackupTimestamp: Long = 0L
)

class BoutiquePreferences(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val SHOP_TAGLINE = stringPreferencesKey("shop_tagline")
        val SHOP_PHONE = stringPreferencesKey("shop_phone")
        val SHOP_ADDRESS = stringPreferencesKey("shop_address")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val LOCK_REPORTS_WITH_PIN = booleanPreferencesKey("lock_reports_with_pin")
        val ALLOW_NEGATIVE_STOCK = booleanPreferencesKey("allow_negative_stock")
        val PAPER_WIDTH_MM = intPreferencesKey("paper_width_mm")
        val BITMAP_THERMAL_MODE = booleanPreferencesKey("bitmap_thermal_mode")
        val LAST_PRINTER_ADDRESS = stringPreferencesKey("last_printer_address")
        val LAST_PRINTER_NAME = stringPreferencesKey("last_printer_name")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val INVOICE_COUNTER = intPreferencesKey("invoice_counter")
        val RETURN_COUNTER = intPreferencesKey("return_counter")
    }

    val settingsFlow: Flow<BoutiqueSettings> = context.dataStore.data.map { prefs ->
        BoutiqueSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            shopName = prefs[Keys.SHOP_NAME] ?: "NAQSH COLLECTION",
            shopTagline = prefs[Keys.SHOP_TAGLINE] ?: "MODEST & PAKISTANI WEAR",
            shopPhone = prefs[Keys.SHOP_PHONE] ?: "",
            shopAddress = prefs[Keys.SHOP_ADDRESS] ?: "",
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            pinEnabled = prefs[Keys.PIN_ENABLED] ?: false,
            lockReportsWithPin = prefs[Keys.LOCK_REPORTS_WITH_PIN] ?: true,
            allowNegativeStock = prefs[Keys.ALLOW_NEGATIVE_STOCK] ?: false,
            paperWidthMm = prefs[Keys.PAPER_WIDTH_MM] ?: 58,
            bitmapThermalMode = prefs[Keys.BITMAP_THERMAL_MODE] ?: true,
            lastPrinterAddress = prefs[Keys.LAST_PRINTER_ADDRESS] ?: "",
            lastPrinterName = prefs[Keys.LAST_PRINTER_NAME] ?: "",
            autoBackupEnabled = prefs[Keys.AUTO_BACKUP_ENABLED] ?: true,
            lastBackupTimestamp = prefs[Keys.LAST_BACKUP_TIMESTAMP] ?: 0L
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun updateShopDetails(name: String, tagline: String, phone: String, address: String) {
        context.dataStore.edit {
            it[Keys.SHOP_NAME] = name.ifBlank { "NAQSH COLLECTION" }
            it[Keys.SHOP_TAGLINE] = tagline.ifBlank { "MODEST & PAKISTANI WEAR" }
            it[Keys.SHOP_PHONE] = phone.trim()
            it[Keys.SHOP_ADDRESS] = address.trim()
        }
    }

    suspend fun setPin(pin: String) {
        val hash = hashPin(pin)
        context.dataStore.edit {
            it[Keys.PIN_HASH] = hash
            it[Keys.PIN_ENABLED] = true
        }
    }

    suspend fun disablePin() {
        context.dataStore.edit {
            it[Keys.PIN_ENABLED] = false
            it[Keys.PIN_HASH] = ""
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val currentHash = context.dataStore.data.first()[Keys.PIN_HASH] ?: ""
        return currentHash.isNotEmpty() && currentHash == hashPin(pin)
    }

    suspend fun setLockReportsWithPin(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCK_REPORTS_WITH_PIN] = enabled }
    }

    suspend fun setAllowNegativeStock(allowed: Boolean) {
        context.dataStore.edit { it[Keys.ALLOW_NEGATIVE_STOCK] = allowed }
    }

    suspend fun setPrinterConfig(paperWidthMm: Int, bitmapMode: Boolean) {
        context.dataStore.edit {
            it[Keys.PAPER_WIDTH_MM] = paperWidthMm
            it[Keys.BITMAP_THERMAL_MODE] = bitmapMode
        }
    }

    suspend fun setLastPrinter(address: String, name: String) {
        context.dataStore.edit {
            it[Keys.LAST_PRINTER_ADDRESS] = address
            it[Keys.LAST_PRINTER_NAME] = name
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_TIMESTAMP] = timestamp }
    }

    suspend fun nextInvoiceSequence(): Int {
        var next = 1
        context.dataStore.edit { prefs ->
            next = (prefs[Keys.INVOICE_COUNTER] ?: 0) + 1
            prefs[Keys.INVOICE_COUNTER] = next
        }
        return next
    }

    suspend fun nextReturnSequence(): Int {
        var next = 1
        context.dataStore.edit { prefs ->
            next = (prefs[Keys.RETURN_COUNTER] ?: 0) + 1
            prefs[Keys.RETURN_COUNTER] = next
        }
        return next
    }

    companion object {
        fun hashPin(pin: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(("NAQSH_SALT_$pin").toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}

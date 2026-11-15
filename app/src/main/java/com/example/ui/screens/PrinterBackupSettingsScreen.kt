package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.BoutiqueUiState
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedBg
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneSoft
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenBg
import com.example.util.IndianFormatters
import com.example.util.LocalBackupInfo
import com.example.util.PrinterConnectionState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSetupScreen(
    state: BoutiqueUiState,
    printerState: PrinterConnectionState,
    onRefreshPaired: () -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onConnectPrinter: (String, String) -> Unit,
    onDisconnectPrinter: () -> Unit,
    onReconnectLast: () -> Unit,
    onTestPrint: () -> Unit,
    onSetPrinterConfig: (Int, Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    val btPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    var pendingScanAfterPerm by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            onRefreshPaired()
            if (pendingScanAfterPerm) {
                pendingScanAfterPerm = false
                onStartDiscovery()
            }
        }
    }

    LaunchedEffect(Unit) {
        onRefreshPaired()
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(strings.printerSettings, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldDeep)
        )

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Connection Status Card
            item {
                val connected = printerState.connectedName != null
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (connected) ProfitGreenBg else MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, if (connected) ProfitGreen else MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (connected) Icons.Filled.CheckCircle else Icons.Filled.Bluetooth,
                                contentDescription = null,
                                tint = if (connected) ProfitGreen else EmeraldPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = printerState.connectedName ?: "No Thermal Printer Connected",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = printerState.statusMessage ?: (printerState.connectedAddress ?: "Ready to connect ESC/POS printer"),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        if (printerState.isConnecting || printerState.isPrinting || printerState.isScanning) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        printerState.errorMessage?.let { err ->
                            Surface(color = AlertRedBg, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(err, color = AlertRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (connected) {
                                Button(
                                    onClick = onTestPrint,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.weight(1f).testTag("btn_test_print")
                                ) {
                                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.testPrint)
                                }
                                OutlinedButton(onClick = onDisconnectPrinter, modifier = Modifier.weight(1f)) {
                                    Text(strings.disconnect)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { permLauncher.launch(btPermissions); onReconnectLast() },
                                    enabled = state.settings.lastPrinterAddress.isNotBlank(),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(strings.reconnectLast)
                                }
                                Button(
                                    onClick = {
                                        pendingScanAfterPerm = true
                                        permLauncher.launch(btPermissions)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier.weight(1f).testTag("btn_scan_printers")
                                ) {
                                    Icon(Icons.Filled.BluetoothSearching, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.discoverPrinters)
                                }
                            }
                        }
                        if (printerState.isScanning) {
                            TextButton(onClick = onStopDiscovery) { Text("Stop Scan") }
                        }
                    }
                }
            }

            // Paper Width & Hindi/Urdu Bitmap Settings
            item {
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.paperWidth, style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = state.settings.paperWidthMm == 58,
                                onClick = { onSetPrinterConfig(58, state.settings.bitmapThermalMode) },
                                label = { Text("58mm (2 inch)") }
                            )
                            FilterChip(
                                selected = state.settings.paperWidthMm == 80,
                                onClick = { onSetPrinterConfig(80, state.settings.bitmapThermalMode) },
                                label = { Text("80mm (3 inch)") }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.bitmapReceiptMode, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Renders NC logo, Hindi & Urdu cleanly via ESC/POS raster graphics",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.settings.bitmapThermalMode,
                                onCheckedChange = { onSetPrinterConfig(state.settings.paperWidthMm, it) }
                            )
                        }
                    }
                }
            }

            // Paired Devices
            item {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("${strings.pairedDevices} (${printerState.pairedDevices.size})", style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
                    TextButton(onClick = { permLauncher.launch(btPermissions); onRefreshPaired() }) { Text("Refresh") }
                }
            }
            items(printerState.pairedDevices, key = { it.address }) { dev ->
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dev.name, fontWeight = FontWeight.Bold)
                            Text(dev.address, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = { onConnectPrinter(dev.address, dev.name) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) { Text(strings.connect) }
                    }
                }
            }

            // Discovered Devices
            if (printerState.discoveredDevices.isNotEmpty()) {
                item {
                    Text("${strings.discoveredDevices} (${printerState.discoveredDevices.size})", style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
                }
                items(printerState.discoveredDevices, key = { "disc_${it.address}" }) { dev ->
                    Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dev.name, fontWeight = FontWeight.Bold)
                                Text(dev.address, style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = { onConnectPrinter(dev.address, dev.name) }) {
                                Text(strings.connect)
                            }
                        }
                    }
                }
            }

            // Troubleshooting Card
            item {
                Card(colors = CardDefaults.cardColors(containerColor = ChampagneSoft), border = BorderStroke(1.dp, ChampagneGold)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(strings.printerTroubleshooting, style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
                        Text("1. Supports standard Bluetooth Classic ESC/POS thermal printers (SPP profile, 58mm & 80mm).", style = MaterialTheme.typography.bodySmall)
                        Text("2. Pair the printer once in Android Bluetooth Settings (PIN usually 0000 or 1234), then tap Refresh.", style = MaterialTheme.typography.bodySmall)
                        Text("3. BLE-only or proprietary cloud printers that do not support ESC/POS SPP cannot print over RFCOMM; use 'Wi-Fi / System Print' on the invoice sheet for Wi-Fi/AirPrint/Mopria printers.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    state: BoutiqueUiState,
    localBackups: List<LocalBackupInfo>,
    onCreateLocalBackup: () -> Unit,
    onExportBackupStorage: () -> Unit,
    onPickRestoreFile: () -> Unit,
    onRestoreLocalBackup: (File) -> Unit,
    onToggleAutoBackup: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var pendingRestoreFile by remember { mutableStateOf<File?>(null) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(strings.backupRestore, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldDeep)
        )

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ChampagneSoft),
                    border = BorderStroke(1.dp, ChampagneGold)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SHA-256 Verified Offline Backup", style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
                        Text(
                            text = if (state.settings.lastBackupTimestamp > 0) {
                                "Last Backup: ${IndianFormatters.formatDateTime(state.settings.lastBackupTimestamp)}"
                            } else "No backup created yet",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(strings.autoBackupOption, style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = state.settings.autoBackupEnabled, onCheckedChange = onToggleAutoBackup)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onCreateLocalBackup,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_local_backup")
                ) {
                    Icon(Icons.Filled.Backup, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.createLocalBackup, fontWeight = FontWeight.Bold)
                }
            }
            item {
                OutlinedButton(
                    onClick = onExportBackupStorage,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_export_backup")
                ) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.exportBackupStorage)
                }
            }
            item {
                OutlinedButton(
                    onClick = onPickRestoreFile,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_import_backup")
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.importBackupStorage)
                }
            }

            item {
                Text("Local Auto-Backups (${localBackups.size})", style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
            }
            items(localBackups, key = { it.name }) { info ->
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(info.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text(
                                "${IndianFormatters.formatDateTime(info.timestamp)} • ${info.sizeBytes / 1024 + 1} KB",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        TextButton(onClick = { pendingRestoreFile = info.file }) {
                            Text("Restore", color = AlertRed)
                        }
                    }
                }
            }
        }
    }

    pendingRestoreFile?.let { file ->
        AlertDialog(
            onDismissRequest = { pendingRestoreFile = null },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = AlertRed) },
            title = { Text("Confirm Database Restore") },
            text = { Text("${strings.restoreConfirmMsg}\n\nFile: ${file.name}") },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreLocalBackup(file)
                        pendingRestoreFile = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreFile = null }) { Text(strings.cancel) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSecurityScreen(
    state: BoutiqueUiState,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSaveShopDetails: (String, String, String, String) -> Unit,
    onSetupOrChangePin: (String?, String) -> Unit,
    onDisablePin: (String) -> Unit,
    onToggleLockReports: (Boolean) -> Unit,
    onToggleNegativeStock: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var shopName by remember(state.settings.shopName) { mutableStateOf(state.settings.shopName) }
    var tagline by remember(state.settings.shopTagline) { mutableStateOf(state.settings.shopTagline) }
    var phone by remember(state.settings.shopPhone) { mutableStateOf(state.settings.shopPhone) }
    var address by remember(state.settings.shopAddress) { mutableStateOf(state.settings.shopAddress) }

    var showPinDialog by remember { mutableStateOf(false) }
    var disablePinDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text(strings.settingsSecurity, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = EmeraldDeep)
        )

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Language Selector
            item {
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.languageLabel, style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppLanguage.entries.forEach { lang ->
                                FilterChip(
                                    selected = state.settings.language == lang,
                                    onClick = { onLanguageSelected(lang) },
                                    label = { Text(lang.displayName) }
                                )
                            }
                        }
                    }
                }
            }

            // Boutique Header Details
            item {
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(strings.shopDetails, style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
                        OutlinedTextField(value = shopName, onValueChange = { shopName = it }, label = { Text("Boutique Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = tagline, onValueChange = { tagline = it }, label = { Text("Tagline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text(strings.mobileNumber) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text(strings.address) }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = { onSaveShopDetails(shopName, tagline, phone, address) },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(strings.save) }
                    }
                }
            }

            // Security & PIN Lock
            item {
                Card(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.pinLockTitle, style = MaterialTheme.typography.titleSmall, color = EmeraldDeep)
                        }
                        Text(
                            if (state.settings.pinEnabled) "PIN Lock is ACTIVE" else "PIN Lock is OFF (Optional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.settings.pinEnabled) ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showPinDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (state.settings.pinEnabled) strings.changePin else "Enable PIN")
                            }
                            if (state.settings.pinEnabled) {
                                OutlinedButton(onClick = { disablePinDialog = true }, modifier = Modifier.weight(1f)) {
                                    Text("Disable PIN", color = AlertRed)
                                }
                            }
                        }
                        if (state.settings.pinEnabled) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text(strings.financialPinLock, style = MaterialTheme.typography.bodySmall)
                                Switch(checked = state.settings.lockReportsWithPin, onCheckedChange = onToggleLockReports)
                            }
                        }
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(strings.negativeStockSetting, style = MaterialTheme.typography.bodySmall)
                            Switch(checked = state.settings.allowNegativeStock, onCheckedChange = onToggleNegativeStock)
                        }
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        var currentPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(if (state.settings.pinEnabled) strings.changePin else strings.pinLockTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.settings.pinEnabled) {
                        OutlinedTextField(
                            value = currentPin,
                            onValueChange = { if (it.length <= 4) currentPin = it },
                            label = { Text("Current 4-Digit PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4) newPin = it },
                        label = { Text("New 4-Digit PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetupOrChangePin(if (state.settings.pinEnabled) currentPin else null, newPin)
                        showPinDialog = false
                    },
                    enabled = newPin.length == 4
                ) { Text(strings.save) }
            },
            dismissButton = { TextButton(onClick = { showPinDialog = false }) { Text(strings.cancel) } }
        )
    }

    if (disablePinDialog) {
        var currentPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { disablePinDialog = false },
            title = { Text("Disable PIN Lock") },
            text = {
                OutlinedTextField(
                    value = currentPin,
                    onValueChange = { if (it.length <= 4) currentPin = it },
                    label = { Text("Enter Current 4-Digit PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDisablePin(currentPin)
                        disablePinDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                    enabled = currentPin.length == 4
                ) { Text(strings.confirm) }
            },
            dismissButton = { TextButton(onClick = { disablePinDialog = false }) { Text(strings.cancel) } }
        )
    }
}

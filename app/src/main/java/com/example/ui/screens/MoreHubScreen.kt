package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.BoutiqueUiState
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneSoft
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.util.IndianFormatters
import com.example.util.PrinterConnectionState

enum class MoreDestination {
    HUB, CUSTOMERS, EXPENSES, REPORTS, PRINTERS, BACKUP, SETTINGS
}

private data class MoreMenuEntry(
    val dest: MoreDestination,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tag: String
)

@Composable
fun MoreHubScreen(
    state: BoutiqueUiState,
    printerState: PrinterConnectionState,
    onNavigate: (MoreDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val entries = listOf(
        MoreMenuEntry(
            MoreDestination.CUSTOMERS,
            strings.customers,
            "${state.customers.size} • Due: ${IndianFormatters.formatCurrency(state.overallAccounting.customerOutstanding)}",
            Icons.Filled.People,
            "more_customers"
        ),
        MoreMenuEntry(
            MoreDestination.EXPENSES,
            strings.expenses,
            "${strings.todaysExpenses}: ${IndianFormatters.formatCurrency(state.todayAccounting.operatingExpenses)}",
            Icons.Filled.MoneyOff,
            "more_expenses"
        ),
        MoreMenuEntry(
            MoreDestination.REPORTS,
            strings.reportsAnalytics,
            "P&L, Sales, Stock, Ledger, PDF & CSV",
            Icons.Filled.Assessment,
            "more_reports"
        ),
        MoreMenuEntry(
            MoreDestination.PRINTERS,
            strings.printerSettings,
            printerState.connectedName ?: "${state.settings.paperWidthMm}mm Bluetooth & Wi-Fi Print",
            Icons.Filled.Print,
            "more_printers"
        ),
        MoreMenuEntry(
            MoreDestination.BACKUP,
            strings.backupRestore,
            if (state.settings.lastBackupTimestamp > 0) "Last: ${IndianFormatters.formatDateTime(state.settings.lastBackupTimestamp)}" else "SHA-256 Safe Local & Storage Backup",
            Icons.Filled.Backup,
            "more_backup"
        ),
        MoreMenuEntry(
            MoreDestination.SETTINGS,
            strings.settingsSecurity,
            "${state.settings.language.displayName} • ${if (state.settings.pinEnabled) "PIN Active" else "PIN Off"}",
            Icons.Filled.Settings,
            "more_settings"
        )
    )

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Surface(color = EmeraldDeep, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = strings.navMore,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(entries) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(item.dest) }
                        .testTag(item.tag),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = ChampagneSoft,
                            border = BorderStroke(1.dp, ChampagneGold),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = EmeraldPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = EmeraldDeep)
                            Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

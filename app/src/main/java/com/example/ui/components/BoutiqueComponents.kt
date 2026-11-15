package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.repository.SaleWithItems
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ChampagneSoft
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IvoryWhite
import com.example.ui.theme.ProfitGreen
import com.example.util.IndianFormatters
import kotlinx.coroutines.delay

@Composable
fun NaqshSplashScreen(onFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        delay(1050)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(EmeraldDeep, EmeraldPrimary, Color(0xFF07281F))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(550)) + scaleIn(initialScale = 0.9f, animationSpec = tween(550))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_nc_monogram),
                    contentDescription = "NAQSH COLLECTION Logo",
                    modifier = Modifier
                        .size(136.dp)
                        .clip(CircleShape)
                        .border(2.dp, ChampagneGold, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "NAQSH COLLECTION",
                    style = MaterialTheme.typography.displaySmall,
                    color = IvoryWhite,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "MODEST & PAKISTANI WEAR",
                    style = MaterialTheme.typography.labelLarge,
                    color = ChampagneGold,
                    letterSpacing = 2.4.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PinPadView(
    title: String,
    subtitle: String,
    onPinComplete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var pin by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = EmeraldDeep,
            border = BorderStroke(1.5.dp, ChampagneGold),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Security Lock",
                    tint = ChampagneGold,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        // 4 PIN Dots
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            repeat(4) { idx ->
                val filled = idx < pin.length
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(if (filled) EmeraldPrimary else Color.Transparent)
                        .border(2.dp, if (filled) ChampagneGold else MaterialTheme.colorScheme.outline, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.height(28.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "DEL")
        )
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                row.forEach { key ->
                    Surface(
                        shape = CircleShape,
                        color = if (key == "C" || key == "DEL") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable {
                                when (key) {
                                    "C" -> pin = ""
                                    "DEL" -> if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                    else -> {
                                        if (pin.length < 4) {
                                            pin += key
                                            if (pin.length == 4) {
                                                val submitted = pin
                                                pin = ""
                                                onPinComplete(submitted)
                                            }
                                        }
                                    }
                                }
                            }
                            .testTag("pin_key_$key")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (key == "DEL") {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiMetricCard(
    label: String,
    value: String,
    accentColor: Color = EmeraldPrimary,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EmptyBoutiqueState(
    icon: ImageVector,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = ChampagneSoft,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    body: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(body) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
            ) {
                Text(strings.delete)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceActionsSheet(
    saleWithItems: SaleWithItems,
    printerConnectedName: String?,
    onShareWhatsApp: () -> Unit,
    onSharePdf: () -> Unit,
    onPrintThermal: (isDuplicate: Boolean) -> Unit,
    onPrintSystem: () -> Unit,
    onSaveInvoicePdf: () -> Unit,
    onOpenPrinterSetup: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val sale = saleWithItems.sale

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = ProfitGreen,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.invoiceSaved,
                        style = MaterialTheme.typography.titleLarge,
                        color = EmeraldDeep
                    )
                    Text(
                        text = "${sale.invoiceNumber} • ${IndianFormatters.formatDateTime(sale.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(sale.customerName, fontWeight = FontWeight.SemiBold)
                        Text("${saleWithItems.items.sumOf { it.quantity }} items", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.grandTotal, fontWeight = FontWeight.Bold)
                        Text(
                            IndianFormatters.formatCurrency(sale.grandTotal),
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDeep
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${strings.paid} (${sale.paymentMethod})", style = MaterialTheme.typography.bodySmall)
                        Text(IndianFormatters.formatCurrency(sale.paidAmount), color = ProfitGreen, fontWeight = FontWeight.SemiBold)
                    }
                    if (sale.remainingBalance > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.remainingCredit, style = MaterialTheme.typography.bodySmall, color = AlertRed)
                            Text(IndianFormatters.formatCurrency(sale.remainingBalance), color = AlertRed, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (sale.changeReturned > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(strings.changeReturned, style = MaterialTheme.typography.bodySmall)
                            Text(IndianFormatters.formatCurrency(sale.changeReturned), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 1. SHARE ON WHATSAPP
            Button(
                onClick = onShareWhatsApp,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF128C7E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_share_whatsapp")
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.shareOnWhatsapp, fontWeight = FontWeight.Bold)
            }

            // 2. SHARE PDF & 4. SAVE INVOICE
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onSharePdf,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_share_pdf"),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.sharePdf, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                OutlinedButton(
                    onClick = onSaveInvoicePdf,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_save_invoice_pdf"),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.saveInvoicePdf, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            // 3. PRINT BILL (Bluetooth ESC/POS + Android Print Framework)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (printerConnectedName != null) onPrintThermal(false) else onOpenPrinterSetup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_print_thermal"),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (printerConnectedName != null) strings.printBill else "BT Printer",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onPrintSystem,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_print_system"),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.systemPrintPdf, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            if (printerConnectedName != null) {
                TextButton(
                    onClick = { onPrintThermal(true) },
                    modifier = Modifier.align(Alignment.CenterHorizontally).minimumInteractiveComponentSize()
                ) {
                    Text("${strings.duplicatePrint} ($printerConnectedName)")
                }
            }
        }
    }
}

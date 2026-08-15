package ba.noniboy.struja.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ba.noniboy.struja.ui.components.DividerItem
import ba.noniboy.struja.ui.components.MonthOutlookCard
import ba.noniboy.struja.ui.viewmodel.MeterDetailViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings

/**
 * Meter detail screen — shows readings, bills, and month outlook for a meter.
 * Port of the web app's `/meters/[id]` page.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MeterDetailScreen(
    meterId: String,
    onBack: () -> Unit,
    onAddReading: () -> Unit,
    onBillClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: MeterDetailViewModel = hiltViewModel()
) {
    val meter by viewModel.meter.collectAsState()
    val readings by viewModel.readings.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val monthOutlook by viewModel.monthOutlook.collectAsState()

    val tabs = listOf("Pregled", "Računi", "Postavke")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(meter?.name ?: "Učitavanje...") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddReading) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Dodaj očitanje"
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Meter info
            meter?.let { m ->
                MeterInfoRow(
                    tariffGroup = m.tariffGroup,
                    approvedKw = m.approvedKw,
                    notes = m.notes
                )
            }

            // Tabs
            var selectedTab by remember { mutableStateOf(0) }
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            when (selectedTab) {
                0 -> {
                    // Preview tab — month outlook + latest bill preview
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        monthOutlook?.let { outlook ->
                            MonthOutlookCard(outlook = outlook)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Latest bill summary if any
                        val latestBill = bills.firstOrNull()
                        latestBill?.let { bill ->
                            LatestBillSummary(
                                bill = bill,
                                onClick = { onBillClick(bill.id) }
                            )
                        }
                    }
                }

                1 -> {
                    // Bills tab
                    if (bills.isEmpty()) {
                        EmptyStateMessage("Nema računa")
                    } else {
                        BillList(
                            bills = bills,
                            onBillClick = onBillClick
                        )
                    }
                }

                2 -> {
                    // Readings tab
                    if (readings.isEmpty()) {
                        EmptyStateMessage("Nema očitanja")
                    } else {
                        ReadingList(
                            readings = readings,
                            onDelete = { viewModel.deleteReading(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MeterInfoRow(tariffGroup: String, approvedKw: Double, notes: String?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoText(label = "Tarifa", value = tariffGroup)
                InfoText(label = "Snaga", value = "${approvedKw} kW")
            }
            notes?.let { n ->
                if (n.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = n,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoText(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LatestBillSummary(
    bill: ba.noniboy.struja.data.local.entity.BillEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A3A5F).copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${bill.periodStart} → ${bill.periodEnd}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Potrošnja: ${"%.1f".format(bill.consumptionKwh)} kWh",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "KM ${"%.2f".format(bill.total)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00C5B7)
                )
            }
        }
    }
}

@Composable
fun BillList(
    bills: List<ba.noniboy.struja.data.local.entity.BillEntity>,
    onBillClick: (String) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(bills.size) { index ->
            val bill = bills[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onBillClick(bill.id) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "${bill.periodStart} → ${bill.periodEnd}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Potrošnja: ${"%.1f".format(bill.consumptionKwh)} kWh",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "KM ${"%.2f".format(bill.total)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ReadingList(
    readings: List<ba.noniboy.struja.data.local.entity.ReadingEntity>,
    onDelete: (ba.noniboy.struja.data.local.entity.ReadingEntity) -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(readings.size) { index ->
            val reading = readings[index]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = reading.recordedAt,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val values = buildString {
                            reading.vt?.let { append("VT: $it  ") }
                            reading.mt?.let { append("MT: $it  ") }
                            reading.reading?.let { append("Vrijednost: $it") }
                        }
                        Text(
                            text = values.ifEmpty { "Nema podataka" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        reading.confidence?.let { c ->
                            Text(
                                text = "Pouzdanje: $c",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (c == "high") Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                        }
                    }

                    IconButton(onClick = { onDelete(reading) }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Obriši",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

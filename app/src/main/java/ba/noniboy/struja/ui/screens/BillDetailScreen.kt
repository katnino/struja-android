package ba.noniboy.struja.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ba.noniboy.struja.ui.components.BillBreakdown
import ba.noniboy.struja.ui.components.DividerItem
import ba.noniboy.struja.ui.viewmodel.BillDetailViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import kotlinx.coroutines.launch

/**
 * Bill detail screen — shows the full bill breakdown and supports PDF export.
 * Port of the web app's `/meters/[id]/bills/[billId]` page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillDetailScreen(
    billId: String,
    onBack: () -> Unit,
    viewModel: BillDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(billId) {
        viewModel.loadBill(billId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalji računa") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error ?: "Greška",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        } else {
            uiState.bill?.let { bill ->
                BillDetailContent(
                    bill = bill,
                    modifier = Modifier.padding(padding),
                    onGeneratePdf = {
                        scope.launch {
                            val uri = viewModel.generatePdf(bill, context)
                            uri?.let {
                                sharePdf(context, it)
                            }
                        }
                    }
                )
            }
        }
    }
}

/**
 * Share a PDF file via intent.
 */
private fun sharePdf(context: android.content.Context, uri: Uri) {
    try {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Podijeli PDF"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun BillDetailContent(
    bill: ba.noniboy.struja.data.local.entity.BillEntity,
    modifier: Modifier = Modifier,
    onGeneratePdf: () -> Unit
) {
    val blocksJson = bill.blocksJson

    // Parse blocks JSON using Gson
    val gson = Gson()
    val blocks: List<ba.noniboy.struja.domain.models.BlockBreakdown> = try {
        val type = object : TypeToken<List<ba.noniboy.struja.domain.models.BlockBreakdown>>() {}.type
        gson.fromJson(blocksJson, type) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Period header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Period računa",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${bill.periodStart} → ${bill.periodEnd}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (bill.isPartialObračun) "Potpuni (djelomični)" else "Potpuni",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Block breakdown
        if (blocks.isNotEmpty()) {
            BillBreakdown(blocks = blocks)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Summary card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A3A5F).copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sažetak",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                DividerItem(label = "Potrošnja (kWh)", value = bill.consumptionKwh)
                DividerItem(label = "Energija", value = bill.energyCost)
                DividerItem(label = "Mjerna pristojina", value = bill.mjernoMjesto)
                DividerItem(label = "Obr. snaga", value = bill.obracunskaSnaga)
                DividerItem(label = "OIE", value = bill.oieCost)
                DividerItem(
                    label = "Prenos + Distribucija",
                    value = bill.subtotal - bill.energyCost - bill.mjernoMjesto - bill.obracunskaSnaga - bill.oieCost
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                DividerItem(label = "Podražunjeno", value = bill.subtotal, isBold = true)
                DividerItem(label = "PDV (17%)", value = bill.vatAmount)
                DividerItem(
                    label = "UKUPNO",
                    value = bill.total,
                    isBold = true,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PDF Export and Share buttons (iText7)
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onGeneratePdf,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("  Generiši PDF", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onGeneratePdf,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("  Podijeli", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

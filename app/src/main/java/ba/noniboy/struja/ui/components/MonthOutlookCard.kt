package ba.noniboy.struja.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ba.noniboy.struja.domain.outlook.MonthOutlook
import ba.noniboy.struja.domain.outlook.OutlookConfidence
import ba.noniboy.struja.ui.theme.BlockIColor

/**
 * Card showing the month outlook — actual consumption vs. projected.
 * Port of MonthOutlookCard.tsx from the web app.
 */
@Composable
fun MonthOutlookCard(
    outlook: MonthOutlook,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E3A5F).copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mesecni pregled",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ConfidenceBadge(confidence = outlook.confidence)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actual vs Projected
            val actualVt = outlook.actual?.vtKwh ?: 0.0
            val actualMt = outlook.actual?.mtKwh ?: 0.0
            val totalVt = outlook.totalEstimatedVt
            val totalMt = outlook.totalEstimatedMt

            OutlookRow(
                label = "VT (visoka tarifa)",
                actual = actualVt,
                projected = totalVt,
                dailyRate = outlook.runRate?.vtPerDay
            )
            OutlookRow(
                label = "MT (niska tarifa)",
                actual = actualMt,
                projected = totalMt,
                dailyRate = outlook.runRate?.mtPerDay
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Projected bill total
            outlook.bill?.let { bill ->
                DividerItem(
                    label = "Procjenjena račun",
                    value = bill.total,
                    isBold = true
                )
            }
        }
    }
}

@Composable
fun ConfidenceBadge(confidence: OutlookConfidence) {
    val (text, color) = when (confidence) {
        OutlookConfidence.MEASURED -> "Izmjereno" to Color(0xFF4CAF50)
        OutlookConfidence.PROJECTED -> "Projekcija" to Color(0xFFFF9800)
        OutlookConfidence.INSUFFICIENT_DATA -> "Nedovoljno podataka" to Color(0xFF9E9E9E)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun OutlookRow(
    label: String,
    actual: Double,
    projected: Double,
    dailyRate: Double?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "%.1f kWh".format(actual), style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "→ %.1f kWh".format(projected),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = BlockIColor
            )
        }
        dailyRate?.let { rate ->
            Text(
                text = "%.1f kWh/dan".format(rate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

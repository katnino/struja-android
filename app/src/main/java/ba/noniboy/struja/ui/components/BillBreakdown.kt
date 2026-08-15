package ba.noniboy.struja.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ba.noniboy.struja.domain.models.BlockBreakdown
import ba.noniboy.struja.ui.theme.BlockIColor
import ba.noniboy.struja.ui.theme.BlockIIColor
import ba.noniboy.struja.ui.theme.BlockIIIColor

@Composable
fun blockColor(blockIndex: Int): Color {
    return when (blockIndex) {
        0 -> BlockIColor
        1 -> BlockIIColor
        2 -> BlockIIIColor
        else -> MaterialTheme.colorScheme.primary
    }
}

@Composable
fun BillBreakdown(
    blocks: List<BlockBreakdown>,
    modifier: Modifier = Modifier
) {
    if (blocks.isEmpty()) {
        Text(
            text = "Nema podataka o blokovima",
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
        )
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Raspored blokova",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        blocks.forEachIndexed { index, block ->
            BlockCard(block = block, blockIndex = index)
        }
    }
}

@Composable
fun BlockCard(
    block: BlockBreakdown,
    blockIndex: Int
) {
    val scaleAnim by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .scale(scaleAnim),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = block.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = blockColor(blockIndex)
                )
                Text(
                    text = "${block.kwh.toInt()} kWh",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Rates and costs
            val items = listOf(
                "Energija" to block.activeEnergyCost,
                "Prenos" to block.transmissionCost,
                "Distribucija" to block.distributionCost,
                "OIE" to block.oieCost
            )

            items.forEach { (label, cost) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${"%.2f".format(cost)} KM",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Total
            DividerItem(
                label = "Ukupno",
                value = block.totalCost,
                isBold = true
            )
        }
    }
}

@Composable
fun DividerItem(
    label: String,
    value: Double,
    isBold: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = if (isBold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${"%.2f".format(value)} KM",
            style = if (isBold) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isBold) 16.sp else 14.sp
        )
    }
}

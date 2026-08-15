package ba.noniboy.struja.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ba.noniboy.struja.data.vision.ConfidenceLevel
import ba.noniboy.struja.data.vision.ExtractResult
import ba.noniboy.struja.ui.theme.BlockIColor
import ba.noniboy.struja.ui.theme.BlockIIColor

/**
 * Preview card showing the OCR-extracted meter reading values.
 * Allows the user to edit/fix the extracted values before saving.
 *
 * Port of ExtractedPreview.tsx from the web app.
 */
@Composable
fun ExtractedPreview(
    result: ExtractResult?,
    onVtChanged: (String) -> Unit,
    onMtChanged: (String) -> Unit,
    onConfirm: () -> Unit
) {
    if (result == null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A2E)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nema prepoznate vrijednosti",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Confidence indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prepoznato iz slike",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                ConfidenceChip(confidence = result.confidence)
            }

            result.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // VT input
            if (result.vt != null || result.mt != null) {
                DualTariffInputs(
                    vt = result.vt?.toString() ?: "",
                    mt = result.mt?.toString() ?: "",
                    onVtChanged = onVtChanged,
                    onMtChanged = onMtChanged
                )
            } else if (result.reading != null) {
                SingleTariffInput(
                    reading = result.reading.toString(),
                    onReadingChanged = { onVtChanged(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Potvrdi vrijednosti", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ConfidenceChip(confidence: ConfidenceLevel) {
    val (text, color) = when (confidence) {
        ConfidenceLevel.HIGH -> "Visoka" to Color(0xFF4CAF50)
        ConfidenceLevel.LOW -> "Niska" to Color(0xFFFF9800)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "Pouzdanje: $text",
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DualTariffInputs(
    vt: String,
    mt: String,
    onVtChanged: (String) -> Unit,
    onMtChanged: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // VT (upper) input
        OutlinedTextField(
            value = vt,
            onValueChange = { value ->
                if (value.length <= 5 || value.isEmpty()) onVtChanged(value)
            },
            label = { Text("VT (gornja linija)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(2.dp)
                        .background(BlockIColor)
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // MT (lower) input
        OutlinedTextField(
            value = mt,
            onValueChange = { value ->
                if (value.length <= 5 || value.isEmpty()) onMtChanged(value)
            },
            label = { Text("MT (donja linija)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = {
                Box(
                    modifier = Modifier
                        .width(12.dp)
                        .height(2.dp)
                        .background(BlockIIColor)
                )
            }
        )
    }
}

@Composable
fun SingleTariffInput(
    reading: String,
    onReadingChanged: (String) -> Unit
) {
    OutlinedTextField(
        value = reading,
        onValueChange = { value ->
            if (value.length <= 5 || value.isEmpty()) onReadingChanged(value)
        },
        label = { Text("Prijedlog brojila") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

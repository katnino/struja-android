package ba.noniboy.struja.data.vision

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import kotlin.math.min

/**
 * On-device meter reading OCR using ML Kit's TextRecognition.
 *
 * Flow:
 * 1. Take a photo of the meter via CameraX
 * 2. Convert to InputImage
 * 3. Run ML Kit's on-device text recognizer
 * 4. Parse the recognized text using [parseMeterText]
 *
 * The parsing logic mirrors the web app's AI vision prompt (buildPrompt() in types.ts),
 * adapted for local text recognition instead of LLM-based extraction.
 */
class MeterOcrProcessor {

    companion object {
        private const val TAG = "MeterOcrProcessor"

        // Minimum confidence threshold for "high" confidence
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.8f
    }

    // We create a new recognizer instance per call to avoid lifecycle issues
    // ML Kit's on-device text recognizer ships with the app (no model download needed at runtime
    // when using the bundled model, or it auto-downloads on supported devices)

    /**
     * Extract meter reading from a bitmap image.
     *
     * @param bitmap The photo of the electricity meter.
     * @return ExtractResult with vt, mt values and confidence level.
     */
    suspend fun extractMeterReading(bitmap: Bitmap): ExtractResult {
        return try {
            val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                com.google.mlkit.vision.text.latin.TextRecognizerOptions.Builder()
                    .build()
            )

            // Convert bitmap to InputImage
            val inputImage = com.google.mlkit.vision.common.InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(inputImage).await()

            // Parse the recognized text
            val parseResult = parseMeterText(result.text)

            recognizer.close()
            parseResult
        } catch (e: Exception) {
            Log.e(TAG, "OCR extraction failed", e)
            ExtractResult(
                vt = null,
                mt = null,
                confidence = ConfidenceLevel.LOW,
                note = "Greška pri prepoznavanju: ${e.message}"
            )
        }
    }

    /**
     * Parse recognized text from ML Kit to extract meter reading values.
     *
     * Strategy:
     * 1. Normalize text and split into lines.
     * 2. For each line, extract digit groups.
     * 3. Look for the pattern: two lines with 5-digit numbers (VT top, MT bottom).
     * 4. If only one 5-digit number found, treat as single-tariff reading.
     * 5. Use digit count and position heuristics to determine confidence.
     */
    @WorkerThread
    fun parseMeterText(text: String): ExtractResult {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return ExtractResult(
                null, null, confidence = ConfidenceLevel.LOW,
                note = "Nema prepoznatog teksta"
            )
        }

        // Extract digit groups from each line
        val digitGroups = lines.map { line ->
            val digits = line.replace(Regex("[^0-9]"), "")
            digits to digits.length
        }.filter { it.first.isNotEmpty() }

        // Look for lines with 5-6 digits (meter readings are typically 5 digits)
        val fiveDigitLines = digitGroups.filter { it.second >= 5 && it.second <= 6 }

        return when {
            fiveDigitLines.size >= 2 -> {
                // Take the last two matching lines (bottom two on the meter)
                // Top row = VT (first), Bottom row = MT (second)
                val vtStr = fiveDigitLines[fiveDigitLines.size - 2].first
                val mtStr = fiveDigitLines[fiveDigitLines.size - 1].first

                // Take last 5 digits (ignore potential extra digits)
                val vt = vtStr.takeLast(5).toIntOrNull()
                val mt = mtStr.takeLast(5).toIntOrNull()

                ExtractResult(
                    vt = vt,
                    mt = mt,
                    confidence = if (vt != null && mt != null) ConfidenceLevel.HIGH else ConfidenceLevel.LOW,
                    note = if (vt != null && mt != null) null else "Nije moguće prepoznati oba broja"
                )
            }

            fiveDigitLines.size == 1 -> {
                val readingStr = fiveDigitLines[0].first.takeLast(5)
                val reading = readingStr.toDoubleOrNull()
                ExtractResult(
                    reading = reading,
                    confidence = if (reading != null) ConfidenceLevel.HIGH else ConfidenceLevel.LOW,
                    note = if (reading != null) null else "Nije moguće prepoznati broj"
                )
            }

            else -> {
                // Look for any digit sequence as fallback
                val allDigits = digitGroups.maxByOrNull { it.second }
                if (allDigits != null && allDigits.second >= 4) {
                    val readingStr = allDigits.first.takeLast(5)
                    val reading = readingStr.toDoubleOrNull()
                    ExtractResult(
                        reading = reading,
                        confidence = ConfidenceLevel.LOW,
                        note = "Preporučuje se ručna provjera vrijednosti"
                    )
                } else {
                    ExtractResult(
                        null, null, confidence = ConfidenceLevel.LOW,
                        note = "Nema dovoljno cifara za prepoznavanje"
                    )
                }
            }
        }
    }

    /**
     * Convert a Bitmap to an integer array for easier processing.
     * Used for brightness/contrast adjustments before OCR.
     */
    fun preprocessBitmap(bitmap: Bitmap): Bitmap {
        // For now, return the bitmap as-is. Can add preprocessing (brightness, contrast, etc.)
        return bitmap
    }
}

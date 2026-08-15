package ba.noniboy.struja.data.vision

/**
 * Result of OCR extraction from a meter reading photo.
 */
data class ExtractResult(
    val vt: Int? = null,
    val mt: Int? = null,
    val reading: Double? = null, // For single-tariff meters (TG1)
    val confidence: ConfidenceLevel,
    val note: String? = null
)

enum class ConfidenceLevel {
    HIGH,
    LOW
}

/**
 * Prompt logic adapted from buildPrompt() in the web app's types.ts.
 *
 * The original prompt instructs the AI to:
 * - Identify a dual-tariff meter (DTS) with two rows of digits
 * - Top row = VT consumption (5 digits), bottom row = MT consumption (5 digits)
 * - Ignore the red decimal digit
 * - Ignore serial numbers and specification text below the disc
 * - Return JSON: {"vt": 82345, "mt": 52341, "confidence": "high", "note": "..."}
 *
 * In the local Android app, ML Kit does raw OCR text extraction, and this logic
 * is applied as post-processing to parse the meter reading.
 */
object MeterPrompt {
    const val SYSTEM_PROMPT = """
        You are a specialized electricity meter reading assistant. Extract the VT (peak) and MT (off-peak) consumption values from a dual-tariff electricity meter (DTS).

        Identification rules:
        - A DTS meter has TWO rows of digits: top row (left to right) = VT (visoko-tarifna), bottom row = MT (nisko-tarifna).
        - Each row has exactly 5 digits (ignore the red decimal digit to the right).
        - Ignore serial numbers and specification text below the meter disc.
        - Return ONLY valid JSON: {"vt": <int>, "mt": <int>, "confidence": "high", "note": "..."}

        If you cannot confidently read both rows, return confidence "low" and explain in "note".
        If the meter is single-tariff (TG1), return only {"reading": <int>}.
    """

    const val TEST_PROMPT = "Extract VT and MT from this electricity meter photo"
}

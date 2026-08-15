package ba.noniboy.struja.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ba.noniboy.struja.data.local.entity.BillEntity
import ba.noniboy.struja.data.repository.BillRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BillDetailUiState(
    val bill: BillEntity? = null,
    val blocksJson: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for the BillDetail screen.
 * Displays the bill breakdown and supports PDF generation.
 */
@HiltViewModel
class BillDetailViewModel @Inject constructor(
    private val billRepository: BillRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillDetailUiState(isLoading = true))
    val uiState: StateFlow<BillDetailUiState> = _uiState.asStateFlow()

    fun loadBill(billId: String) {
        viewModelScope.launch {
            try {
                val bill = billRepository.get(billId)
                _uiState.value = BillDetailUiState(
                    bill = bill,
                    blocksJson = bill?.blocksJson,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = BillDetailUiState(
                    isLoading = false,
                    error = "Greška: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Generate a PDF file for the given bill using iText7.
     * Saves to the app's external documents directory and returns a Uri.
     */
    fun generatePdf(bill: BillEntity, context: Context): Uri? {
        return try {
            val blocks = billRepository.parseBlocks(bill.blocksJson)

            val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            val file = File(documentsDir, "racun_${bill.periodStart}_${bill.periodEnd}.pdf")
            val writer = com.itextpdf.kernel.pdf.PdfWriter(file)
            val pdf = com.itextpdf.kernel.pdf.PdfDocument(writer)
            val document = com.itextpdf.layout.Document(pdf, com.itextpdf.kernel.geom.PageSize.A4)

            // Fonts
            val regularFont = com.itextpdf.kernel.font.PdfFontFactory.createFont()
            val boldFont = com.itextpdf.kernel.font.PdfFontFactory.createFont(
                com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD
            )

            // Title
            document.add(
                com.itextpdf.layout.element.Paragraph("Račun za električnu energiju")
                    .setFont(boldFont)
                    .setFontSize(18f)
                    .setMarginBottom(20f)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
            )

            // Period info
            document.add(
                com.itextpdf.layout.element.Paragraph("Period: ${bill.periodStart} → ${bill.periodEnd}")
                    .setFont(regularFont)
                    .setFontSize(12f)
                    .setMarginBottom(10f)
            )
            document.add(
                com.itextpdf.layout.element.Paragraph(
                    if (bill.isPartialObračun) "Potpuni (djelomični)" else "Potpuni"
                ).setFont(regularFont)
                    .setFontSize(12f)
                    .setMarginBottom(20f)
            )

            // Block breakdown
            if (blocks.isNotEmpty()) {
                document.add(
                    com.itextpdf.layout.element.Paragraph("Raspored blokova")
                        .setFont(boldFont)
                        .setFontSize(14f)
                        .setMarginBottom(10f)
                )

                val table = com.itextpdf.layout.element.Table(
                    com.itextpdf.layout.properties.UnitValue.createPercentArray(
                        floatArrayOf(3f, 1f, 1.5f, 1.5f, 1.5f, 1.5f, 1.5f)
                    )
                ).setWidth(com.itextpdf.layout.properties.UnitValue.createPercentValue(100f)).setMarginBottom(20f)

                // Header row
                val headers = listOf("Blok", "kWh", "Energija", "Prenos", "Distribucija", "OIE", "Ukupno")
                headers.forEach { h ->
                    table.addHeaderCell(
                        com.itextpdf.layout.element.Cell().add(
                            com.itextpdf.layout.element.Paragraph(h)
                                .setFont(boldFont)
                                .setFontSize(10f)
                        )
                    )
                }

                blocks.forEachIndexed { index, block ->
                    val blockLabel = "Blok ${index + 1}"
                    val cells = listOf(
                        blockLabel,
                        "${block.kwh.toInt()}",
                        "${"%.2f".format(block.activeEnergyCost)} KM",
                        "${"%.2f".format(block.transmissionCost)} KM",
                        "${"%.2f".format(block.distributionCost)} KM",
                        "${"%.2f".format(block.oieCost)} KM",
                        "${"%.2f".format(block.totalCost)} KM"
                    )
                    cells.forEach { cellText ->
                        table.addCell(
                            com.itextpdf.layout.element.Cell().add(
                                com.itextpdf.layout.element.Paragraph(cellText)
                                    .setFont(regularFont).setFontSize(10f)
                            )
                        )
                    }
                }

                document.add(table)
            }

            // Summary
            document.add(
                com.itextpdf.layout.element.Paragraph("Sažetak")
                    .setFont(boldFont)
                    .setFontSize(14f)
                    .setMarginBottom(10f)
            )

            fun addSummaryRow(label: String, value: Double) {
                document.add(
                    com.itextpdf.layout.element.Paragraph("$label: ${"%.2f".format(value)} KM")
                        .setFont(regularFont)
                        .setFontSize(10f)
                        .setMarginBottom(2f)
                )
            }

            addSummaryRow("Potrošnja (kWh)", bill.consumptionKwh)
            addSummaryRow("Energija", bill.energyCost)
            addSummaryRow("Mjerna pristojina", bill.mjernoMjesto)
            addSummaryRow("Obr. snaga", bill.obracunskaSnaga)
            addSummaryRow("OIE", bill.oieCost)
            val transmissionDistribution =
                bill.subtotal - bill.energyCost - bill.mjernoMjesto - bill.obracunskaSnaga - bill.oieCost
            addSummaryRow("Prenos + Distribucija", transmissionDistribution)
            document.add(
                com.itextpdf.layout.element.Paragraph("__________________________________").setFont(regularFont)
            )
            addSummaryRow("Podražunjeno", bill.subtotal)
            addSummaryRow("PDV (17%)", bill.vatAmount)
            addSummaryRow("UKUPNO", bill.total)

            document.close()

            FileProvider.getUriForFile(context, "ba.noniboy.struja.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

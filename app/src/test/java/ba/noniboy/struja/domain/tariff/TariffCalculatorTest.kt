package ba.noniboy.struja.domain.tariff

import ba.noniboy.struja.domain.models.DEFAULT_RATES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.round

class TariffCalculatorTest {

    @Test
    fun `splitBlocks should correctly split consumption into blocks`() {
        // Case 1: Only Block I
        val result1 = TariffCalculator.splitBlocks(300.0)
        assertEquals(300.0, result1.first, 0.001)
        assertEquals(0.0, result1.second, 0.001)
        assertEquals(0.0, result1.third, 0.001)

        // Case 2: Block I and II
        val result2 = TariffCalculator.splitBlocks(800.0)
        assertEquals(500.0, result2.first, 0.001)
        assertEquals(300.0, result2.second, 0.001)
        assertEquals(0.0, result2.third, 0.001)

        // Case 3: Block I, II, and III
        val result3 = TariffCalculator.splitBlocks(2000.0)
        assertEquals(500.0, result3.first, 0.001)
        assertEquals(1000.0, result3.second, 0.001)
        assertEquals(500.0, result3.third, 0.001)

        // Case 4: Zero consumption
        val result4 = TariffCalculator.splitBlocks(0.0)
        assertEquals(0.0, result4.first, 0.001)
        assertEquals(0.0, result4.second, 0.001)
        assertEquals(0.0, result4.third, 0.001)
    }

    @Test
    fun `calculateBill should calculate the June 2026 bill like the paper invoice`() {
        val result = TariffCalculator.calculateBill(136.0, 108.0)

        assertEquals(244.0, result.consumptionKwh, 0.01)
        assertEquals(1, result.blocks.size)
        assertEquals(244.0, result.blocks[0].kwh, 0.01)
        assertEquals(15.44, result.totalEnergy, 0.01)
        assertEquals(3.09, result.totalTransmission, 0.01)
        assertEquals(23.49, result.totalDistribution, 0.01)
        assertEquals(0.18, result.totalOie, 0.01)
        assertEquals(2.48, result.mjernoMjesto, 0.01)
        assertEquals(11.51, result.obracunskaSnaga, 0.01)
        assertEquals(44.68, result.subtotal, 0.01)
        assertEquals(7.60, result.vatAmount, 0.01)
        assertEquals(52.28, result.total, 0.01)
    }

    @Test
    fun `calculateBill should handle zero consumption`() {
        val result = TariffCalculator.calculateBill(0.0, 0.0)
        assertEquals(0.0, result.consumptionKwh, 0.01)
        assertEquals(0, result.blocks.size)
        assertEquals(0.0, result.totalEnergy, 0.01)
        assertEquals(0.0, result.totalTransmission, 0.01)
        assertEquals(0.0, result.totalDistribution, 0.01)
    }

    @Test
    fun `calculateBill should handle invalid inputs`() {
        val result1 = TariffCalculator.calculateBill(-100.0, 100.0)
        assertEquals(0.0, result1.consumptionKwh, 0.01)

        val result2 = TariffCalculator.calculateBill(Double.NaN, 100.0)
        assertEquals(0.0, result2.consumptionKwh, 0.01)
    }

    @Test
    fun `calculateBill should calculate costs based on VT and MT ratio`() {
        // 100% VT consumption
        val resultVT = TariffCalculator.calculateBill(200.0, 0.0)
        val costVT = resultVT.totalEnergy

        // 100% MT consumption
        val resultMT = TariffCalculator.calculateBill(0.0, 200.0)
        val costMT = resultMT.totalEnergy

        // VT is generally more expensive than MT
        assertTrue(costVT > costMT)
    }

    @Test
    fun `calculateBill should correctly apply VAT`() {
        val result = TariffCalculator.calculateBill(100.0, 100.0)
        val expectedVat = result.subtotal * DEFAULT_RATES.vat
        assertEquals(round(expectedVat * 100) / 100, result.vatAmount, 0.001)
        assertEquals(round((result.subtotal + result.vatAmount) * 100) / 100, result.total, 0.001)
    }
}

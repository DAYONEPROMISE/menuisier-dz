package dz.menuisier.app.optimizer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CutOptimizerTest {

    @Test
    fun `exact fit uses one bar with zero waste`() {
        val r = CutOptimizer.optimizeLinear(
            listOf(LinearPart("A", 3000, 2)),
            stockLengthMm = 6000, kerfMm = 0,
        )
        assertEquals(1, r.barsUsed)
        assertEquals(0, r.totalWasteMm)
        assertEquals(1, r.lowerBoundBars)
    }

    @Test
    fun `kerf is accounted per cut`() {
        val r = CutOptimizer.optimizeLinear(
            listOf(LinearPart("A", 1000, 2)),
            stockLengthMm = 2500, kerfMm = 10,
        )
        // 2x(1000+10)=2020 fits one bar; waste = 2500-2020.
        assertEquals(1, r.barsUsed)
        assertEquals(480, r.totalWasteMm)
        assertEquals(20, r.totalKerfMm)
    }

    @Test
    fun `conservation holds stock equals parts plus kerf plus waste`() {
        val parts = listOf(
            LinearPart("Montant", 1200, 4),
            LinearPart("Traverse", 800, 6),
            LinearPart("Petit", 350, 10),
        )
        val stock = 6000
        val r = CutOptimizer.optimizeLinear(parts, stock, kerfMm = 3)
        val partsLen = 4 * 1200 + 6 * 800 + 10 * 350
        assertEquals(r.barsUsed * stock, partsLen + r.totalKerfMm + r.totalWasteMm)
    }

    @Test
    fun `ffd never exceeds 11-9 OPT bound on sample`() {
        val parts = listOf(
            LinearPart("A", 1500, 6),
            LinearPart("B", 900, 8),
            LinearPart("C", 450, 12),
        )
        val r = CutOptimizer.optimizeLinear(parts, 6000, kerfMm = 3)
        assertTrue(r.barsUsed <= (11.0 / 9.0 * r.lowerBoundBars + 1).toInt() + 1)
        assertTrue(r.barsUsed >= r.lowerBoundBars)
    }

    @Test
    fun `deterministic same input same plan`() {
        val parts = listOf(LinearPart("A", 1100, 3), LinearPart("B", 700, 5))
        val r1 = CutOptimizer.optimizeLinear(parts, 6000, 3)
        val r2 = CutOptimizer.optimizeLinear(parts, 6000, 3)
        assertEquals(r1, r2)
    }

    @Test
    fun `panels pack without overlap and inside bounds`() {
        val parts = listOf(
            RectPart("Porte", 600, 2000, 2),
            RectPart("Etagere", 800, 400, 4),
        )
        val r = CutOptimizer.optimizePanels(parts, panelWMm = 2440, panelHMm = 1220)
        assertTrue(r.panelsUsed >= 1)
        for (p in r.panels) {
            for (a in p.placed) {
                assertTrue(a.xMm + a.wMm <= 2440)
                assertTrue(a.yMm + a.hMm <= 1220)
            }
            for (i in p.placed.indices) for (j in i + 1 until p.placed.size) {
                val a = p.placed[i]; val b = p.placed[j]
                val overlap = a.xMm < b.xMm + b.wMm && b.xMm < a.xMm + a.wMm &&
                    a.yMm < b.yMm + b.hMm && b.yMm < a.yMm + a.hMm
                assertTrue("overlap $a vs $b", !overlap)
            }
        }
        val total = parts.sumOf { it.wMm.toLong() * it.hMm * it.quantity }
        val used = r.panels.sumOf { it.usedAreaMm2 }
        assertEquals(total, used)
    }

    @Test
    fun `reste a payer never negative`() {
        val o = dz.menuisier.app.data.ClientOrder(clientName = "Test", totalDzd = 100000, acompteDzd = 120000)
        assertEquals(0, o.resteDzd)
    }
}

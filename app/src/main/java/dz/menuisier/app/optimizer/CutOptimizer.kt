package dz.menuisier.app.optimizer

/**
 * Clean-room 1D cutting-stock optimizer (First Fit Decreasing) + 2D shelf
 * packer for rectangular panels. No third-party optimizer dependency.
 * All units are integer millimetres; kerf (blade width) in mm.
 *
 * 1D FFD: expand demands, sort longest-first, place each piece into the
 * first stock bar with enough remaining space (piece + kerf), else open a
 * new bar. Deterministic: ties broken by input order.
 * Guarantee (classical bin-packing bound): uses at most 11/9*OPT + 1 bars.
 */

data class LinearPart(val label: String, val lengthMm: Int, val quantity: Int)

data class BarCut(val partLabel: String, val lengthMm: Int)

data class BarPlan(
    val barIndex: Int,
    val cuts: List<BarCut>,
    val usedMm: Int,
    /** Leftover on this bar, kerf already accounted in [usedMm]. */
    val wasteMm: Int,
)

data class LinearResult(
    val bars: List<BarPlan>,
    val barsUsed: Int,
    val totalWasteMm: Int,
    val totalKerfMm: Int,
    /** ceil(sum(parts) / stock) — fragmentation-ignoring lower bound. */
    val lowerBoundBars: Int,
    val utilizationPct: Double,
)

data class RectPart(val label: String, val wMm: Int, val hMm: Int, val quantity: Int)

data class PlacedRect(val label: String, val xMm: Int, val yMm: Int, val wMm: Int, val hMm: Int)

data class PanelPlan(
    val panelIndex: Int,
    val placed: List<PlacedRect>,
    val usedAreaMm2: Long,
    val wastePct: Double,
)

data class PanelResult(
    val panels: List<PanelPlan>,
    val panelsUsed: Int,
    val meanWastePct: Double,
)

object CutOptimizer {

    fun optimizeLinear(
        parts: List<LinearPart>,
        stockLengthMm: Int,
        kerfMm: Int = 3,
    ): LinearResult {
        require(stockLengthMm > 0) { "stockLengthMm must be > 0" }
        require(kerfMm >= 0) { "kerfMm must be >= 0" }
        val expanded = parts.flatMapIndexed { row, p ->
            require(p.lengthMm > 0 && p.quantity >= 0) { "bad part: $p" }
            require(p.lengthMm <= stockLengthMm) { "part ${p.label} longer than stock" }
            List(p.quantity) { Triple(p.lengthMm, row, p.label) }
        }.sortedWith(compareByDescending<Triple<Int, Int, String>> { it.first }.thenBy { it.second })

        val bars = mutableListOf<MutableList<BarCut>>()
        val remaining = mutableListOf<Int>()
        var totalKerf = 0
        val totalPartsLen = expanded.sumOf { it.first }.toLong()

        for ((len, _, label) in expanded) {
            var placed = false
            for (i in bars.indices) {
                // Each cut consumes piece + one kerf (conservative; matches shop convention).
                if (remaining[i] >= len + kerfMm) {
                    bars[i].add(BarCut(label, len))
                    remaining[i] -= (len + kerfMm)
                    totalKerf += kerfMm
                    placed = true
                    break
                }
            }
            if (!placed) {
                bars.add(mutableListOf(BarCut(label, len)))
                remaining.add(stockLengthMm - len - kerfMm)
                totalKerf += kerfMm
            }
        }

        val plans = bars.mapIndexed { idx, cuts ->
            val used = cuts.sumOf { it.lengthMm } + cuts.size * kerfMm
            BarPlan(idx + 1, cuts.toList(), used, stockLengthMm - used)
        }
        val totalWaste = plans.sumOf { it.wasteMm }
        val totalStock = plans.size.toLong() * stockLengthMm
        val lower = if (totalPartsLen == 0L) 0
            else ((totalPartsLen + kerfMm * expanded.size) + stockLengthMm - 1) / stockLengthMm
        val util = if (totalStock == 0L) 100.0
            else (totalPartsLen + totalKerf).toDouble() / totalStock.toDouble() * 100.0
        return LinearResult(plans, plans.size, totalWaste, totalKerf, lower.toInt(), util)
    }

    /**
     * Shelf (guillotine-compatible) 2D packer: sort parts by height desc,
     * fill horizontal shelves left-to-right, open a new shelf when the part
     * does not fit the current one, new panel when the shelf does not fit
     * vertically. Rotation of 90° allowed when it makes a part fit.
     */
    fun optimizePanels(
        parts: List<RectPart>,
        panelWMm: Int,
        panelHMm: Int,
    ): PanelResult {
        require(panelWMm > 0 && panelHMm > 0) { "panel size must be > 0" }
        val expanded = parts.flatMap { p ->
            require(p.wMm > 0 && p.hMm > 0 && p.quantity >= 0) { "bad part: $p" }
            List(p.quantity) { Pair(p.wMm, p.hMm) to p.label }
        }.sortedWith(
            compareByDescending<Pair<Pair<Int, Int>, String>> { maxOf(it.first.first, it.first.second) }
                .thenByDescending { minOf(it.first.first, it.first.second) },
        )
        val panels = mutableListOf<MutableList<PlacedRect>>()
        var shelfY = 0
        var shelfH = 0
        var shelfX = 0

        fun newPanel() {
            panels.add(mutableListOf())
            shelfY = 0; shelfH = 0; shelfX = 0
        }
        newPanel()

        for ((wh, label) in expanded) {
            var (w, h) = wh
            // Try upright; rotate if it helps fit the panel at all.
            if ((w > panelWMm || h > panelHMm) && (h <= panelWMm && w <= panelHMm)) {
                val t = w; w = h; h = t
            }
            require(w <= panelWMm && h <= panelHMm) { "part $label does not fit panel" }
            // New shelf if needed.
            if (shelfX + w > panelWMm) {
                shelfY += shelfH; shelfH = 0; shelfX = 0
            }
            // Rotate into current shelf if it fits better.
            if (shelfX + w > panelWMm || shelfY + h > panelHMm) {
                if (h <= panelWMm - shelfX && w <= panelHMm - shelfY && shelfX + h <= panelWMm) {
                    val t = w; w = h; h = t
                }
            }
            if (shelfY + h > panelHMm) {
                newPanel()
            }
            if (shelfX + w > panelWMm) {
                shelfY += shelfH; shelfH = 0; shelfX = 0
                if (shelfY + h > panelHMm) newPanel()
            }
            panels.last().add(PlacedRect(label, shelfX, shelfY, w, h))
            shelfX += w
            if (h > shelfH) shelfH = h
        }

        val panelArea = panelWMm.toLong() * panelHMm.toLong()
        val plans = panels.mapIndexed { idx, placed ->
            val used = placed.sumOf { it.wMm.toLong() * it.hMm.toLong() }
            PanelPlan(idx + 1, placed.toList(), used, (1.0 - used.toDouble() / panelArea.toDouble()) * 100.0)
        }
        val meanWaste = if (plans.isEmpty()) 0.0 else plans.sumOf { it.wastePct } / plans.size
        return PanelResult(plans, plans.size, meanWaste)
    }
}

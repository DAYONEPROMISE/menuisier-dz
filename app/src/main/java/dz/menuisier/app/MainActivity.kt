package dz.menuisier.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.room.Room
import dz.menuisier.app.data.AppDatabase
import dz.menuisier.app.data.ClientOrder
import dz.menuisier.app.optimizer.CutOptimizer
import dz.menuisier.app.optimizer.LinearPart
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "menuisierdz.db")
            .fallbackToDestructiveMigration()
            .build()
        setContent {
            // Arabic toggle flips layout direction (RTL) to prove bidi support.
            var arabic by remember { mutableStateOf(false) }
            androidx.compose.runtime.CompositionLocalProvider(
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                MaterialTheme {
                    AppRoot(db, arabic) { arabic = it }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(db: AppDatabase, arabic: Boolean, onLang: (Boolean) -> Unit) {
    var tab by remember { mutableStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (arabic) "منجرة DZ" else "MenuisierDZ") },
                actions = {
                    Button(onClick = { onLang(!arabic) }) {
                        Text(if (arabic) "FR" else "AR")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    label = { Text(if (arabic) "الطلبات" else "Commandes") },
                    icon = {},
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    label = { Text(if (arabic) "القص" else "Découpe") },
                    icon = {},
                )
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            if (tab == 0) OrdersScreen(db, arabic) else OptimizeScreen(arabic)
        }
    }
}

@Composable
fun OrdersScreen(db: AppDatabase, arabic: Boolean) {
    val orders by db.orderDao().observeOrders().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var acompte by remember { mutableStateOf("") }
    val dzd = remember { NumberFormat.getNumberInstance(Locale.FRANCE) }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text(if (arabic) "الزبون" else "Client") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(total, { total = it.filter(Char::isDigit) }, label = { Text(if (arabic) "المجموع (دج)" else "Total (DZD)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(acompte, { acompte = it.filter(Char::isDigit) }, label = { Text(if (arabic) "العربون (دج)" else "Acompte (DZD)") }, modifier = Modifier.weight(1f))
        }
        Button(onClick = {
            val t = total.toLongOrNull() ?: return@Button
            val a = acompte.toLongOrNull() ?: 0L
            scope.launch {
                db.orderDao().insertOrder(ClientOrder(clientName = name, totalDzd = t, acompteDzd = a))
                name = ""; total = ""; acompte = ""
            }
        }) { Text(if (arabic) "إضافة طلب" else "Ajouter commande") }
        Spacer(Modifier.height(4.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(orders) { o ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(o.clientName, fontWeight = FontWeight.Bold)
                        Text(
                            if (arabic) "المجموع: ${dzd.format(o.totalDzd)} دج — العربون: ${dzd.format(o.acompteDzd)} دج — الباقي: ${dzd.format(o.resteDzd)} دج"
                            else "Total : ${dzd.format(o.totalDzd)} DZD — Acompte : ${dzd.format(o.acompteDzd)} DZD — Reste : ${dzd.format(o.resteDzd)} DZD",
                        )
                        Text(if (arabic) "الحالة: ${o.status}" else "Statut : ${o.status}")
                    }
                }
            }
        }
    }
}

@Composable
fun OptimizeScreen(arabic: Boolean) {
    var stock by remember { mutableStateOf("6000") }
    var kerf by remember { mutableStateOf("3") }
    var partsText by remember { mutableStateOf("Montant:1200x4\nTraverse:800x6\nPanneau:500x2") }
    var result by remember { mutableStateOf("") }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(stock, { stock = it.filter(Char::isDigit) }, label = { Text(if (arabic) "اللوح (مم)" else "Barre stock (mm)") }, modifier = Modifier.weight(1f))
            OutlinedTextField(kerf, { kerf = it.filter(Char::isDigit) }, label = { Text(if (arabic) "الشفرة (مم)" else "Lame/kerf (mm)") }, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(
            partsText, { partsText = it },
            label = { Text(if (arabic) "القطع: اسم:طولxكمية (سطر لكل قطعة)" else "Pièces : nom:longueurXqté (1/ligne)") },
            modifier = Modifier.fillMaxWidth(), minLines = 3,
        )
        Button(onClick = {
            result = try {
                val stockLen = stock.toInt()
                val kf = kerf.toIntOrNull() ?: 3
                val parts = partsText.lines().mapNotNull { line ->
                    val halves = line.split(":")
                    if (halves.size != 2) return@mapNotNull null
                    val (lenQ) = halves.drop(1)
                    val lq = lenQ.split("x", "X", "×")
                    if (lq.size != 2) return@mapNotNull null
                    val len = lq[0].trim().toIntOrNull() ?: return@mapNotNull null
                    val qty = lq[1].trim().toIntOrNull() ?: return@mapNotNull null
                    LinearPart(halves[0].trim(), len, qty)
                }
                val r = CutOptimizer.optimizeLinear(parts, stockLen, kf)
                buildString {
                    appendLine(if (arabic) "الألواح: ${r.barsUsed} — الهالك: ${r.totalWasteMm} مم — الاستغلال: ${"%.1f".format(r.utilizationPct)}٪" else "Barres : ${r.barsUsed} — Chutes : ${r.totalWasteMm} mm — Rendement : ${"%.1f".format(r.utilizationPct)} %")
                    r.bars.forEach { b ->
                        appendLine("  #${b.barIndex}: ${b.cuts.joinToString(" + ") { "${it.partLabel}(${it.lengthMm})" }} → chute ${b.wasteMm} mm")
                    }
                }
            } catch (e: Exception) {
                (if (arabic) "خطأ: " else "Erreur : ") + e.message
            }
        }) { Text(if (arabic) "احسب" else "Optimiser") }
        Text(result)
    }
}

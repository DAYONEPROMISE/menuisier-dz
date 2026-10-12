package dz.menuisier.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.room.Room
import dz.menuisier.app.data.AppDatabase
import dz.menuisier.app.data.ClientOrder
import dz.menuisier.app.optimizer.CutOptimizer
import dz.menuisier.app.optimizer.LinearPart
import dz.menuisier.app.ui.DzTheme
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "menuisierdz.db")
            .fallbackToDestructiveMigration()
            .build()
        setContent {
            // FR default (LTR); AR toggle flips to RTL. Dark mode follows system by default.
            var arabic by remember { mutableStateOf(false) }
            var darkOverride by remember { mutableStateOf<Boolean?>(null) }
            val darkTheme = darkOverride ?: isSystemInDarkTheme()
            CompositionLocalProvider(
                LocalLayoutDirection provides if (arabic) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                DzTheme(darkTheme = darkTheme) {
                    AppRoot(db, arabic, darkTheme, { arabic = it }, { darkOverride = it })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    db: AppDatabase,
    arabic: Boolean,
    darkTheme: Boolean,
    onLang: (Boolean) -> Unit,
    onDark: (Boolean?) -> Unit,
) {
    var tab by remember { mutableStateOf(0) }
    val ordersLabel = if (arabic) "\u0627\u0644\u0637\u0644\u0628\u0627\u062a" else "Commandes"
    val cutLabel = if (arabic) "\u0627\u0644\u0642\u0635" else "D\u00e9coupe"
    val navItems = listOf(
        ordersLabel to Icons.Filled.ReceiptLong,
        cutLabel to Icons.Filled.ContentCut
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (arabic) "\u0645\u0646\u062c\u0631\u0629 DZ" else "MenuisierDZ") },
                actions = {
                    IconButton(
                        onClick = { onDark(if (darkTheme) false else true) },
                        modifier = Modifier.semantics {
                            contentDescription = if (darkTheme) "Light mode" else "Dark mode"
                        },
                    ) {
                        Icon(
                            if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = null,
                        )
                    }
                    Button(onClick = { onLang(!arabic) }) {
                        Text(if (arabic) "FR" else "AR")
                    }
                },
            )
        },
        // Adaptive navigation: rail on wide screens (tablet/landscape), bar on phones.
        bottomBar = {
            BoxWithConstraints {
                if (maxWidth < 600.dp) {
                    NavigationBar {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            navItems.forEachIndexed { i, (label, icon) ->
                                NavBarItem(selected = tab == i, onClick = { tab = i }, label = label, icon = icon)
                            }
                        }
                    }
                }
            }
        },
    ) { pad ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(pad).consumeWindowInsets(pad)) {
            val wide = maxWidth >= 600.dp
            Row(Modifier.fillMaxSize()) {
                if (wide) {
                    NavigationRail {
                        NavRailEntry(selected = tab == 0, onClick = { tab = 0 }, label = ordersLabel, icon = Icons.Filled.ReceiptLong)
                        NavRailEntry(selected = tab == 1, onClick = { tab = 1 }, label = cutLabel, icon = Icons.Filled.ContentCut)
                    }
                }
                Column(Modifier.weight(1f).fillMaxSize()) {
                    if (tab == 0) OrdersScreen(db, arabic) else OptimizeScreen(arabic)
                }
            }
        }
    }
}

@Composable
private fun NavBarItem(selected: Boolean, onClick: () -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun NavRailEntry(selected: Boolean, onClick: () -> Unit, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    androidx.compose.material3.NavigationRailItem(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = label) },
    )
}

@Composable
fun OrdersScreen(db: AppDatabase, arabic: Boolean) {
    val orders by db.orderDao().observeOrders().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var acompte by remember { mutableStateOf("") }
    val dzd = remember { NumberFormat.getNumberInstance(Locale.FRANCE) }

    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(if (arabic) "\u0627\u0644\u0632\u0628\u0648\u0646" else "Client") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(total, { total = it.filter(Char::isDigit) }, label = { Text(if (arabic) "\u0627\u0644\u0645\u062c\u0645\u0648\u0639 (\u062f\u062c)" else "Total (DZD)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(acompte, { acompte = it.filter(Char::isDigit) }, label = { Text(if (arabic) "\u0627\u0644\u0639\u0631\u0628\u0648\u0646 (\u062f\u062c)" else "Acompte (DZD)") }, modifier = Modifier.weight(1f))
                }
                Button(onClick = {
                    val t = total.toLongOrNull() ?: return@Button
                    val a = acompte.toLongOrNull() ?: 0L
                    scope.launch {
                        db.orderDao().insertOrder(ClientOrder(clientName = name, totalDzd = t, acompteDzd = a))
                        name = ""; total = ""; acompte = ""
                    }
                }) { Text(if (arabic) "\u0625\u0636\u0627\u0641\u0629 \u0637\u0644\u0628" else "Ajouter commande") }
                Spacer(Modifier.height(4.dp))
            }
        }
        items(orders) { o ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(o.clientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (arabic) "\u0627\u0644\u0645\u062c\u0645\u0648\u0639: ${dzd.format(o.totalDzd)} \u062f\u062c \u2014 \u0627\u0644\u0639\u0631\u0628\u0648\u0646: ${dzd.format(o.acompteDzd)} \u062f\u062c \u2014 \u0627\u0644\u0628\u0627\u0642\u064a: ${dzd.format(o.resteDzd)} \u062f\u062c"
                        else "Total : ${dzd.format(o.totalDzd)} DZD \u2014 Acompte : ${dzd.format(o.acompteDzd)} DZD \u2014 Reste : ${dzd.format(o.resteDzd)} DZD",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        if (arabic) "\u0627\u0644\u062d\u0627\u0644\u0629: ${o.status}" else "Statut : ${o.status}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(stock, { stock = it.filter(Char::isDigit) }, label = { Text(if (arabic) "\u0627\u0644\u0644\u0648\u062d (\u0645\u0645)" else "Barre stock (mm)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(kerf, { kerf = it.filter(Char::isDigit) }, label = { Text(if (arabic) "\u0627\u0644\u0634\u0641\u0631\u0629 (\u0645\u0645)" else "Lame/kerf (mm)") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    partsText, { partsText = it },
                    label = { Text(if (arabic) "\u0627\u0644\u0642\u0637\u0639: \u0627\u0633\u0645:\u0637\u0648\u0644x\u0643\u0645\u064a\u0629 (\u0633\u0637\u0631 \u0644\u0643\u0644 \u0642\u0637\u0639\u0629)" else "Pi\u00e8ces : nom:longueurXqt\u00e9 (1/ligne)") },
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
                            val lq = lenQ.split("x", "X", "\u00d7")
                            if (lq.size != 2) return@mapNotNull null
                            val len = lq[0].trim().toIntOrNull() ?: return@mapNotNull null
                            val qty = lq[1].trim().toIntOrNull() ?: return@mapNotNull null
                            LinearPart(halves[0].trim(), len, qty)
                        }
                        val r = CutOptimizer.optimizeLinear(parts, stockLen, kf)
                        buildString {
                            appendLine(if (arabic) "\u0627\u0644\u0623\u0644\u0648\u0627\u062d: ${r.barsUsed} \u2014 \u0627\u0644\u0647\u0627\u0644\u0643: ${r.totalWasteMm} \u0645\u0645 \u2014 \u0627\u0644\u0627\u0633\u062a\u063a\u0644\u0627\u0644: ${"%.1f".format(r.utilizationPct)}\u066a" else "Barres : ${r.barsUsed} \u2014 Chutes : ${r.totalWasteMm} mm \u2014 Rendement : ${"%.1f".format(r.utilizationPct)} %")
                            r.bars.forEach { b ->
                                appendLine("  #${b.barIndex}: ${b.cuts.joinToString(" + ") { "${it.partLabel}(${it.lengthMm})" }} \u2192 chute ${b.wasteMm} mm")
                            }
                        }
                    } catch (e: Exception) {
                        (if (arabic) "\u062e\u0637\u0623: " else "Erreur : ") + e.message
                    }
                }) { Text(if (arabic) "\u0627\u062d\u0633\u0628" else "Optimiser") }
                if (result.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Text(result, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

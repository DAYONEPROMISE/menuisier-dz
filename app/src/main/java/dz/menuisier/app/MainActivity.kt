package dz.menuisier.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.room.Room
import dz.menuisier.app.data.AppDatabase
import dz.menuisier.app.data.ClientOrder
import dz.menuisier.app.optimizer.CutOptimizer
import dz.menuisier.app.optimizer.LinearPart
import dz.menuisier.app.ui.DzTheme
import dz.menuisier.app.ui.MenuEmptyState
import dz.menuisier.app.ui.MenuHeader
import dz.menuisier.app.ui.SectionLabel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

private const val PREFS = "menuisierdz"
private const val KEY_LANG = "lang"

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val lang = newBase.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANG, "fr") ?: "fr"
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "menuisierdz.db")
            .fallbackToDestructiveMigration()
            .build()
        setContent {
            var lang by remember {
                mutableStateOf(
                    getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANG, "fr") ?: "fr"
                )
            }
            var darkOverride by remember { mutableStateOf<Boolean?>(null) }
            val darkTheme = darkOverride ?: isSystemInDarkTheme()
            DzTheme(darkTheme = darkTheme) {
                AppRoot(
                    db = db,
                    lang = lang,
                    darkTheme = darkTheme,
                    onLang = { next ->
                        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putString(KEY_LANG, next).apply()
                        lang = next
                        recreate()
                    },
                    onDark = { darkOverride = it },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    db: AppDatabase,
    lang: String,
    darkTheme: Boolean,
    onLang: (String) -> Unit,
    onDark: (Boolean?) -> Unit,
) {
    val arabic = lang == "ar"
    var tab by remember { mutableStateOf(0) }
    val ordersLabel = stringResource(R.string.tab_orders)
    val cutLabel = stringResource(R.string.tab_cut)
    Scaffold(
        topBar = {
            MenuHeader(
                title = stringResource(R.string.app_name),
                trailing = {
                    IconButton(
                        onClick = { onDark(if (darkTheme) false else true) }
                    ) {
                        Icon(
                            if (darkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = stringResource(
                                if (darkTheme) R.string.action_light_mode else R.string.action_dark_mode
                            ),
                        )
                    }
                    Button(onClick = { onLang(if (arabic) "fr" else "ar") }) {
                        Text(stringResource(R.string.lang_other))
                    }
                }
            )
        },
        // Adaptive navigation: rail on wide screens (tablet/landscape), bar on phones.
        bottomBar = {
            BoxWithConstraints {
                if (maxWidth < 600.dp) {
                    NavigationBar {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf(ordersLabel to Icons.Filled.ReceiptLong, cutLabel to Icons.Filled.ContentCut)
                                .forEachIndexed { i, (label, icon) ->
                                    NavigationBarItem(
                                        selected = tab == i,
                                        onClick = { tab = i },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        label = { Text(label) },
                                        icon = { Icon(icon, contentDescription = label) }
                                    )
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
                        listOf(ordersLabel to Icons.Filled.ReceiptLong, cutLabel to Icons.Filled.ContentCut)
                            .forEachIndexed { i, (label, icon) ->
                                NavigationRailItem(
                                    selected = tab == i,
                                    onClick = { tab = i },
                                    label = { Text(label) },
                                    icon = { Icon(icon, contentDescription = label) }
                                )
                            }
                    }
                }
                Column(Modifier.weight(1f).fillMaxSize()) {
                    if (tab == 0) OrdersScreen(db) else OptimizeScreen()
                }
            }
        }
    }
}

@Composable
fun OrdersScreen(db: AppDatabase) {
    val orders by db.orderDao().observeOrders().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var acompte by remember { mutableStateOf("") }
    val dzd = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.section_new_order))
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.orders_client)) }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(total, { total = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.orders_total)) }, modifier = Modifier.weight(1f))
                    OutlinedTextField(acompte, { acompte = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.orders_deposit)) }, modifier = Modifier.weight(1f))
                }
                Button(onClick = {
                    val t = total.toLongOrNull() ?: return@Button
                    val a = acompte.toLongOrNull() ?: 0L
                    scope.launch {
                        db.orderDao().insertOrder(ClientOrder(clientName = name, totalDzd = t, acompteDzd = a))
                        name = ""; total = ""; acompte = ""
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.orders_add)) }
                Spacer(Modifier.height(4.dp))
            }
        }
        if (orders.isEmpty()) {
            item {
                MenuEmptyState(
                    icon = Icons.Filled.Widgets,
                    title = stringResource(R.string.empty_orders_title),
                    hint = stringResource(R.string.empty_orders_hint),
                )
            }
        }
        items(orders) { o ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(o.clientName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.orders_line,
                            dzd.format(o.totalDzd),
                            dzd.format(o.acompteDzd),
                            dzd.format(o.resteDzd),
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        stringResource(R.string.orders_status, o.status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun OptimizeScreen() {
    val ctx = LocalContext.current
    var stock by remember { mutableStateOf("6000") }
    var kerf by remember { mutableStateOf("3") }
    var partsText by remember { mutableStateOf("Montant:1200x4\nTraverse:800x6\nPanneau:500x2") }
    var result by remember { mutableStateOf("") }

    LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(stringResource(R.string.section_cut))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(stock, { stock = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.cut_stock)) }, modifier = Modifier.weight(1f))
                    OutlinedTextField(kerf, { kerf = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.cut_kerf)) }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    partsText, { partsText = it },
                    label = { Text(stringResource(R.string.cut_parts)) },
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
                            appendLine(
                                ctx.getString(
                                    R.string.cut_summary,
                                    r.barsUsed,
                                    r.totalWasteMm,
                                    String.format(Locale.getDefault(), "%.1f", r.utilizationPct),
                                )
                            )
                            r.bars.forEach { b ->
                                appendLine(
                                    "  " + ctx.getString(
                                        R.string.cut_bar,
                                        b.barIndex,
                                        b.cuts.joinToString(" + ") { "${it.partLabel}(${it.lengthMm})" },
                                        b.wasteMm,
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        ctx.getString(R.string.cut_error, e.message ?: "")
                    }
                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cut_optimize)) }
                if (result.isNotEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Text(result, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}
